angular.module('care4alf', ['ngRoute', 'ngResource', 'ngSanitize', 'ui.bootstrap'])
    .config(function ($httpProvider,$provide) {
        $provide.factory('loader', function($injector,$q,$rootScope) {
            var loadOperations = 0;
            return {
                'request': function(request) {
                    loadOperations++;
                    $rootScope.loading = loadOperations > 0;
                    return request;
                },
                response: function(response) {
                    loadOperations--;
                    $rootScope.loading = loadOperations > 0;
                    return response;
                }
            }
        });
        $httpProvider.interceptors.push('loader');
    })
    .filter('stripPrefix', function() {
        return function(input) {
            return input.replace('cm:', '');
        };
    })
    .filter('humanBytes', function() {
        return function(fileSizeInBytes) {
            var i = -1;
            var byteUnits = [' kB', ' MB', ' GB', ' TB', 'PB', 'EB', 'ZB', 'YB'];
            do {
                fileSizeInBytes = fileSizeInBytes / 1024;
                i++;
            } while (fileSizeInBytes > 1024);

            return Math.max(fileSizeInBytes, 0.1).toFixed(1) + byteUnits[i];
        }
    })
    .config(['$routeProvider', function ($routeProvider) {
        angular.forEach(care4alfModules, function(module) {
            $routeProvider.when('/' + module.id + '/:subtoken?', {templateUrl: 'resources/partials/' + module.id + '.html', controller: module.id});
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
            }).error(function(data) {
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
    .controller('workflowdefinitions', function($scope,$resource,$http,$window) {
        var Definition = $resource('workflow/definitions/:workflowId', {workflowId:'@id'});

        $scope.definitions = Definition.query();

        $scope.deleteDefinition = function(definition) {
            $http.delete('workflow/definitions', {params: {workflowId: definition.id}}).success(function() {
                $scope.definitions.splice($scope.definitions.indexOf(definition), 1);
            }, function(error) {
                alert(error.data);
            });
        };
    })
    .controller('workflowinstances', function($scope,$resource,$http,$window) {
        $scope.idpattern = /^\w+\$\d+$/;

        var instanceResultHandler = function (instances) {
            $scope.instances = instances;
        };

        $scope.cancelWorkflow = function(instance) {
            $http.delete('workflow/instances/' + instance.id + '/cancel').success(function() {
               $scope.instances.splice($scope.instances.indexOf(instance), 1);
            }).error(function(error) {
                $window.alert(error);
            });
        };

        $scope.deleteWorkflow = function(instance) {
            $http.delete('workflow/instances/' + instance.id + '/delete').success(function() {
                $scope.instances.splice($scope.instances.indexOf(instance), 1);
            }).error(function(error) {
                $window.alert(error);
            });
        };

        $scope.loadTasks = function(instance) {
            $http.get('workflow/instances/' + instance.id + "/tasks").success(function(tasks) {
               instance.tasks = tasks;
            });
        };

        $scope.findInstances = function() {
            if (angular.isDefined($scope.taskid) && $scope.taskid.length > 0) {
                $http.get('workflow/instances/find/task/' + $scope.taskid).success(instanceResultHandler);
            } else if (angular.isDefined($scope.instanceid) && $scope.instanceid.length > 0) {
                $http.get('workflow/instances/find/instance/' + $scope.instanceid).success(instanceResultHandler);
            }
        };

        $scope.findAllActive = function() {
            $http.get('workflow/instances/active').success(instanceResultHandler);
        };
    })
    .controller('dummymail', function($http,$scope) {
        $http.get('smtp/list').success(function(messages) {
            $scope.messages = messages;
        });

        $scope.clear = function() {
            $http.delete('smtp/list').success(function() {
                $scope.messages = [];
            });
        };
    })
    .controller('spring', function($http,$scope,$routeParams) {
        var subToken = $routeParams.subtoken;
        var subUrl = '';
        if (angular.isDefined(subToken)) {
            subUrl = '/' + subToken;
        }
        $http.get('spring/beannames' + subUrl).success(function(beans) {
           $scope.beans = beans;
        });
    })
    .controller('tickets', function($scope,$http) {
        var getUsers = function() {
            $http.get('tickets/list').success(function (users) {
                $scope.users = users;
            });
        };

        getUsers();

        $scope.expire = function(username) {
            $http.delete('tickets/expire/' + username).success(function() {
                getUsers();
            })
        };
    })
    .controller('diskusage', function($scope,$http) {
        var get = function() {
            $http.get('diskusage/byowner').success(function(usage) {
                $scope.sortedUsage = _.sortBy(usage, 'workspace');
            });
        };

        get();

        $scope.updateStats = function() {
            $http.put('diskusage/update').success(function () {
                get();
            });
        };
    })
    .controller('actions', function($scope,$http) {
        $http.get('actions/').success(function(actions) {
            $scope.actions = actions;
        });

        $scope.execute = function(action) {
            $http.post('actions/' + action.name + '/run', {noderef: $scope.noderef}).error(function(error) {
                alert(error.message);
                console.log("failed to execute %o", error);
            });
        };
    })
;
