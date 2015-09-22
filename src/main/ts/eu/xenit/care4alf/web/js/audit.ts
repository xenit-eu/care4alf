/// <reference path="care4alf.ts" />

care4alf.controller('audit', function($scope, $http) {
    $http.get("/alfresco/service/api/audit/control").success(function(data) {
        $scope.control={};
        $scope.control.enabled=data.enabled;
        $scope.control.applications=data.applications;
    });

    $scope.entries=[];
    $scope.load = function(app){
        console.log("loading app: " + app.path);
        $http.get("/alfresco/service/api/audit/query" + app.path + "?verbose=true&limit=1000").success(function(data){
            $scope.entries = data.entries;
        });
    };

    $scope.query = function(app,key, value){
        console.log("Query: " + value);
        $http.get("/alfresco/s/api/audit/query"+app.path+key+"?verbose=true&limit=1000&value="+value).success(function(data){
            $scope.entries = data.entries;
        });
    };
});