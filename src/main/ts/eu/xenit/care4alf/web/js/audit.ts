/// <reference path="care4alf.ts" />

care4alf.controller('audit', function($scope, $http, $routeParams, $location) {
    $scope.sortType     = 'time';
    $scope.sortReverse  = false;
    $scope.showClear = false;
    console.log("found route params: "+$routeParams.subtoken+" and "+$routeParams.subtoken2);

    $scope.load = function(app){
        //$location.url('/audit'+app);
        $scope.showClear = true;
        console.log("loading app: " + app);
        $http.get("/alfresco/service/api/audit/query" + app + "?verbose=true&limit=1000&forward=true").success(function(data){
            $scope.entries = data.entries;
        });
    };

    $scope.query = function(app,key, value){
        $scope.showClear = true;
        console.log("Query: " + value);
        console.log("key: "+key);
        $http.get("/alfresco/s/api/audit/query"+app+key+"?verbose=true&limit=1000&forward=true&value="+value).success(function(data){
            $scope.entries = data.entries;
        });
    };

    if($routeParams.subtoken !== undefined) {
        console.log("putting the routeparam in");
        $scope.application = "/"+$routeParams.subtoken;

        if($routeParams.subtoken2 !== undefined){
            var params = decodeURIComponent($routeParams.subtoken2);
            $scope.query("/" + $routeParams.subtoken,params,$routeParams.subtoken3)
        } else {
            $scope.load("/" + $routeParams.subtoken);
        }
    }

    $http.get("/alfresco/service/api/audit/control").success(function(data) {
        $scope.control={};
        $scope.control.enabled=data.enabled;
        $scope.control.applications=data.applications;
    });

    $scope.entries=[];

    $scope.setRouteLoad = function (app) {
        $location.path('/audit'+app, false);
        $scope.load(app);
    };

    $scope.setRouteQuery = function(app,key,value){
        console.log(encodeURIComponent(key));
        $location.path('/audit'+app+"/"+encodeURIComponent(key)+"/"+value, false);
        $scope.query(app,key,value);
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