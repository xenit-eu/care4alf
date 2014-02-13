angular.module('care4alf', ['ngRoute', 'ngResource'])
    .filter('stripPrefix', function() {
        return function(input) {
            return input.replace('cm:', '');
        };
    })
    .config(['$routeProvider', function ($routeProvider) {
        angular.forEach(care4alfModules, function(module) {
            $routeProvider.when('/' + module.id, {templateUrl: 'resources/partials/' + module.id + '.html', controller: module.id});
        });
        $routeProvider.otherwise({templateUrl: 'resources/partials/default.html', controller:'default'});
    }])
    .controller('default', function($scope,$http) {
        $scope.modules = care4alfModules;
    })
    .controller('documentmodels', function($scope,$http,$q) {
        $http.get('documentmodels/invalidtypes').success(function(invalidTypes) {
            $scope.invalidTypes = invalidTypes;
        });

        $scope.delete = function(document) {
            var deferred = $q.defer();
            $http.delete('documentmodels/node/' + document.id).success(function () {
                $scope.invalidTypes.splice($scope.invalidTypes.indexOf(document), 1);
                deferred.resolve();
            }).error(function(data, status, headers, config) {
                console.log("failed to delete %o because of %o", document, data);
                document.error = data;
                deferred.resolve();
            });
            return deferred.promise;
        };

        $scope.deleteAll = function() {
            var index = 0;
            while ($scope.invalidTypes.length > 0) {
                if (angular.isUndefined($scope.invalidTypes[index].error)) {
                    $scope.delete($scope.invalidTypes[index]).then($scope.deleteAll);
                    break;
                } else {
                    index++;
                }
            }
        };

        $http.get('documentmodels/models').success(function(models) {
            $scope.models = models;
        });

        $scope.removeModel = function(model) {
            $http.delete('documentmodels/models/' + model).success(function () {
                $scope.models.splice($scope.models.indexOf(model), 1);
            }).error(function(error) {
                alert("Failed to remove model: " + error);
            });
        };
    })
    .controller('amps', function($scope, $http) {
        $http.get("amps/list").success(function(modules) {
            $scope.modules = modules;
        });

        $scope.save = function(module) {
            $http.post("amps/save", module);
        };
    })
    .controller('dictionary', function($scope,$http) {
        $http.get('dictionary/namespaces').success(function(namespaces) {
           $scope.namespaces = namespaces;
        });
    })
    .controller('workflow', function($scope,$resource) {
        var Definition = $resource('workflow/:workflowId', {workflowId:'@id'});

        $scope.definitions = Definition.query();

        $scope.deleteDefinition = function(definition) {
            definition.$delete(function() {
                $scope.definitions = Definition.query();
            });
        };
    })
;
