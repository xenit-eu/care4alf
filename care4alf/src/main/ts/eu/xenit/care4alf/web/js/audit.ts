/// <reference path="care4alf.ts" />

care4alf.controller('audit', function($scope, $http, $routeParams, $window: Window, $location) {
    $scope.sortType     = 'time';
    $scope.sortReverse  = false;
    $scope.showClear = false;
    $scope.showBack = false;
    let lastFunc = "";
    console.log("found route params: "+$routeParams.subtoken+" and "+$routeParams.subtoken2);

    $scope.load = function(app){
        lastFunc = "load";
        //$location.url('/audit'+app);
        $scope.showClear = true;
        $scope.showBack = false;
        console.log("loading app: " + app);
        let fromIdBit = $scope.fromId == "" ? "" : "&fromId=" + $scope.fromId;
        let fromTimeBit = $scope.fromTime == "" ? "" : "&fromTime=" + $scope.fromTime;
        let userFilterBit = $scope.userFilter == "" ? "" : "&user=" + $scope.userFilter
        $http.get("/alfresco/service/api/audit/query" + app + "?verbose=true&limit=" + $scope.limit
                + fromIdBit + fromTimeBit + "&forward=" + $scope.forward + userFilterBit).success(function(data) {
            $scope.entries = data.entries;
        });
    };

    $scope.query = function(app: string, key: string, value: string){
        lastFunc = "query?"+key+"?"+value;
        $scope.showClear = true;
        $scope.showBack = true;
        console.log("Query: " + value);
        console.log("key: "+key);
        $http.get("/alfresco/s/api/audit/query"+app+key+"?verbose=true&limit=1000&forward=true&value="+value).success(function(data){
            $scope.entries = data.entries;
        });
    };

    $scope.queryUser = function (app, user) {
        lastFunc = "user?"+user;
        $scope.showBack = true;
        $http.get("/alfresco/s/api/audit/query"+app+"?verbose=true&limit=1000&forward=true&user="+user).success(function(data){
            $scope.entries = data.entries;
        });
    };

    $scope.reload = function () {
        let params = lastFunc.split("?");
        if(params[0] == "load"){
            $scope.load($scope.application);
        }
        if(params[0] == "user"){
            $scope.queryUser($scope.application, params[1]);
        }
        if(params[0] == "query"){
            $scope.query($scope.application, params[1], params[2]);
        }
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

    $scope.deleteEntry = function(app, entry) {
        if ($window.confirm("Are you sure you want to delete entry " + entry.id + " ?")) {
            $http.delete("/alfresco/s/xenit/care4alf/audit" + app + "/" + entry.id).success(() => {
                $scope.entries = _.without($scope.entries, entry)
            })
        }
    }

    $scope.limit = 1000;
    $scope.fromId = "";
    $scope.fromTime = "";
    $scope.forward = true;
    $scope.valueFilter = "";
});