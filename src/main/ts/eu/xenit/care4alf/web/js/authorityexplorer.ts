///<reference path="care4alf.ts"/>

care4alf.controller('authorityexplorer', ($scope,$http: ng.IHttpService) => {

    $scope.webscript = "authorityexplorer/groups";

    $http.get('authorityexplorer/groups').success((res) => {
        console.log("SUCCESS: ",res);
        $scope.res = res;
    }).error((e) => {
        console.error(e);
        e.stacktrace = e.stacktrace.replace(/^/g,"\t").replace(/\n/g,"\n\t");
        $scope.showStackTrace = false;
        $scope.errMessage = e;
    });

});