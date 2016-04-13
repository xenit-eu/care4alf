/// <reference path="care4alf.ts" />

care4alf.controller('solradmin', ($scope,$http: ng.IHttpService) => {
    $http.get('/alfresco/s/xenit/care4alf/solr/proxy/alfresco/admin/luke?numTerms=0').success((data:any) => {
        $scope.fields = Object.keys(data.fields);
    });

    $scope.result="";
    $scope.action=(operation)=>{
        var url = "/alfresco/s/xenit/care4alf/solr/proxy/admin/cores?wt=json&action=" + operation;
        if($scope.nodeid) url += "&nodeid=" + $scope.nodeid;
        console.log(url);
        $http.get(url).success((data:any) => {
            $scope.result = data;
        });
    }
});