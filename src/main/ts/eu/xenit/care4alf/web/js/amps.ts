/// <reference path="care4alf.ts" />

care4alf.controller('amps', function($scope, $http) {
    var load = function(){
       $http.get("amps/").success(function(modules) {
           $scope.modules = modules;
       });
    }

    load();

    $scope.save = function(module) {
        $http.post("amps/", module).then(load,load);
    };

    $scope.clear = function(){
        $http.delete("amps/").then(load);
    }
});