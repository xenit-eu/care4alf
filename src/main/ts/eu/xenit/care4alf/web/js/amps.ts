/// <reference path="care4alf.ts" />

care4alf.controller('amps', function($scope, $http, $window: Window) {
    var load = function() {
       $http.get("amps/").success(function(modules) {
           $scope.modules = modules;
       });
    }

    load();

    $scope.save = function(module) {
        $http.post("amps/", module).then(load,load);
    };

    $scope.clear = function() {
        if ($window.confirm("Are you sure you want to delete all the amps in this Alfresco? " +
                "This will cause any features implemented in these amps to stop working.")) {
            $http.delete("amps/").then(load);
        }
    }
});
