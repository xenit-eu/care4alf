/// <reference path="care4alf.ts" />

care4alf.controller('amps', function($scope, $http) {
    $http.get("amps/list").success(function(modules) {
        $scope.modules = modules;
    });

    $scope.save = function(module) {
        $http.post("amps/save", module);
    };
});