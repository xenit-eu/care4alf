/// <reference path="care4alf.ts" />

declare var care4alfModules;
declare var serviceUrl:string;

var care4alf = angular.module('care4alf', ['ngRoute', 'ngSanitize', 'ngResource', 'ui.bootstrap', 'ui.bootstrap.modal',
        'ui.bootstrap.dropdown', 'angularFileUpload', 'toaster', 'angular-loading-bar'])
        .config(($httpProvider:ng.IHttpProvider, $provide) => {
            $provide.factory('loader', ($injector, $q:ng.IQService, $rootScope, toaster) => {
                var loadOperations = 0;
                return {
                    'request': (request) => {
                        loadOperations++;
                        $rootScope.loading = loadOperations > 0;
                        delete $rootScope.requestError;
                        return request;
                    },
                    response: (response) => {
                        loadOperations--;
                        $rootScope.loading = loadOperations > 0;
                        return response;
                    },
                    responseError: (error) => {
                        loadOperations--;
                        $rootScope.loading = loadOperations > 0;
                        $rootScope.requestError = error.data;

                        if (angular.isDefined(error.data.message)) {
                            toaster.pop('error', "XHR operation failed", error.data.message);
                        }

                        return $q.reject(error);
                    }
                }
            });
            $httpProvider.interceptors.push('loader');
        })
        .config(['$routeProvider', function ($routeProvider:ng.route.IRouteProvider) {
            angular.forEach(care4alfModules, function (module) {
                $routeProvider.when('/' + module.id + '/:subtoken?/:subtoken2?/:subtoken3?', {
                    templateUrl: 'resources/partials/' + module.id + '.html',
                    controller: module.id
                    , controllerAs: module.id
                });
            });
            $routeProvider.otherwise({templateUrl: 'resources/partials/default.html', controller: 'default'});
        }])
        .controller('default', function ($scope) {
            $scope.modules = care4alfModules;
        })


        .run(['$route', '$rootScope', '$location', function ($route, $rootScope, $location) {
        var original = $location.path;
        $location.path = function (path, reload) {
        if (reload === false) {
            var lastRoute = $route.current;
            var un = $rootScope.$on('$locationChangeSuccess', function () {
                $route.current = lastRoute;
                un();
            });
        }
        return original.apply($location, [path]);
        };
        }])


;
