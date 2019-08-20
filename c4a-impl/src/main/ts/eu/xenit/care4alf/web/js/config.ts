/// <reference path="care4alf.ts" />

care4alf.controller('config', function($scope, $http) {
    $scope.properties = {};
    $http.get("config/").success(function(properties) {
        $scope.properties = properties;
    });

    $scope.save = function(key,value) {
        $http.post("config/", {key:key, value:value});
    };

    $scope.remove = function(key) {
        $http.delete("config/"+key, {key:key});
    };
});