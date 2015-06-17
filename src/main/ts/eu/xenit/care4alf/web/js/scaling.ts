care4alf.controller('scaling', ($scope,$http: ng.IHttpService) => {
    $http.get('scaling/NTAX').success((result:any) => {
        $scope.N1 = result.N1;
        $scope.N2 = result.N2;
        $scope.N3 = result.N3;
        $scope.T = result.T;
        $scope.A = result.A;
        $scope.X = result.X;
        $scope.solr = {};
        $scope.solr.filterCache=64;
        $scope.solr.queryResultCache=1024;
        $scope.solr.authorityCache=64;
        $scope.solr.pathCache=64;
    });
});