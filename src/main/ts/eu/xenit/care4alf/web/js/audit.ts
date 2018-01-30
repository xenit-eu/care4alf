/// <reference path="care4alf.ts" />

care4alf.controller('audit', function($scope, $http, $routeParams, $location) {
    $scope.sortType     = 'time';
    $scope.sortReverse  = false;
    console.log("found route param: "+$routeParams.subtoken);

    $scope.load = function(app){
        //$location.url('/audit'+app);
        console.log("loading app: " + app);
        $http.get("/alfresco/service/api/audit/query" + app + "?verbose=true&limit=1000&forward=true").success(function(data){
            $scope.entries = data.entries;
        });
    };


    if($routeParams.subtoken !== null) {
        console.log("putting the routeparam in");
        $scope.application = "/"+$routeParams.subtoken;
        $scope.load("/"+$routeParams.subtoken);
    }

    $http.get("/alfresco/service/api/audit/control").success(function(data) {
        $scope.control={};
        $scope.control.enabled=data.enabled;
        $scope.control.applications=data.applications;
    });

    $scope.entries=[];

    $scope.setRoute = function (app) {
        $location.url('/audit'+app);
    };

    $scope.query = function(app,key, value){
        console.log("Query: " + value);
        $http.get("/alfresco/s/api/audit/query"+app+key+"?verbose=true&limit=1000&forward=true&value="+value).success(function(data){
            $scope.entries = data.entries;
        });
    };

    $scope.clear = function (app) {
        console.log("Clearing:" +app);
        $http.post("/alfresco/s/api/audit/clear"+app).success(function(){
            $scope.showSuccessAlert = true;
        });
    };

    $scope.successTextAlert = "The audit for this application has been successfully cleared";
    $scope.showSuccessAlert = false;

    // switch flag
    $scope.switchBool = function (value) {
        $scope[value] = !$scope[value];
    };
});