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
            });
            return deferred.promise;
        };

        $scope.deleteAll = function() {
            if ($scope.documents.length > 0) {
                $scope.delete($scope.documents[0]).then($scope.deleteAll);
            }
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
;
