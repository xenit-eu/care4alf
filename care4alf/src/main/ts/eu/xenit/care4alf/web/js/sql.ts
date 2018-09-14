/// <reference path="care4alf.ts" />

care4alf.controller('sql', ($scope,$http: ng.IHttpService) => {
    $scope.sql={};
    $scope.sql.query="SELECT 1";
    $scope.query = () => {
        var query: string = $scope.sql.query;
        $http.get('sql?query='+query).success((results) => {
           $scope.results=results;
        });
    };

    getData();

    function getData(){
        $http.get('queries').success((results) => {
            $scope.queries = results;
        });
    }

    $scope.update = () => {
        $scope.sql.query = $scope.selectedQuery.query;
    }

});