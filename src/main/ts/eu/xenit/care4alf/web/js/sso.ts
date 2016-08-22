/// <reference path="care4alf.ts" />

care4alf.controller('sso', ($scope,$http: ng.IHttpService) => {
    $scope.response = {};
    $scope.headerLength = 15000;
    $scope.error=false;

    $scope.sendRequest = function(){
        $scope.error=false;
        var generatedHeader = "";
        for(var i=0; i < $scope.headerLength ; i++){
            generatedHeader += "1"
        }
        $http.get('sso/header', { headers:{
            "HeaderTest":generatedHeader
        }}).success((data:any) => {
            $scope.response = data;
        }).error((data:any) => {
            $scope.response = data;
            $scope.error=true;
        });
    };
});