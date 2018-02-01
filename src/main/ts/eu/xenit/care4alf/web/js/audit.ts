/// <reference path="care4alf.ts" />

care4alf.controller('audit', function($scope, $http, $routeParams, $location) {
    $scope.sortType     = 'time';
    $scope.sortReverse  = false;
    $scope.showClear = false;
    $scope.showBack = false;
    console.log("found route params: "+$routeParams.subtoken+" and "+$routeParams.subtoken2);

    $scope.load = function(app){
        //$location.url('/audit'+app);
        $scope.showClear = true;
        $scope.showBack = false;
        console.log("loading app: " + app);
        $http.get("/alfresco/service/api/audit/query" + app + "?verbose=true&limit=1000&forward=true").success(function(data){
            $scope.entries = data.entries;
        });
    };

    $scope.query = function(app,key, value){
        $scope.showClear = true;
        $scope.showBack = true;
        console.log("Query: " + value);
        console.log("key: "+key);
        $http.get("/alfresco/s/api/audit/query"+app+key+"?verbose=true&limit=1000&forward=true&value="+value).success(function(data){
            $scope.entries = data.entries;
        });
    };
    
    $scope.queryUser = function (app, user) {
        $scope.showBack = true;
        $http.get("/alfresco/s/api/audit/query"+app+"?verbose=true&limit=1000&forward=true&user="+user).success(function(data){
            $scope.entries = data.entries;
        });
    };

    $scope.reload = function () {
        window.location.reload();
    };

    if($routeParams.subtoken !== undefined) {
        console.log("putting the routeparam in");
        $scope.application = "/"+$routeParams.subtoken;

        if($routeParams.subtoken3 !== undefined){
            var params = decodeURIComponent($routeParams.subtoken3);
            $scope.query("/" + $routeParams.subtoken,params,$routeParams.subtoken2)
        } else {
            if($routeParams.subtoken2 !== undefined){
                $scope.queryUser("/" + $routeParams.subtoken, $routeParams.subtoken2);
            } else {
                $scope.load("/" + $routeParams.subtoken);
            }
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
        $location.path('/audit'+app+"/"+value+"/"+encodeURIComponent(key), false);
        $scope.query(app,key,value);
    };

    $scope.setRouteUser = function (app, user) {
        $location.path('/audit'+app+"/"+user, false);
        $scope.queryUser(app,user);
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