/// <reference path="care4alf.ts" />

care4alf.controller('solradmin', ($scope,$http: ng.IHttpService) => {
    $http.get('/alfresco/s/xenit/care4alf/solr/proxy/alfresco/admin/luke?numTerms=0').success((data:any) => {
        $scope.fields = Object.keys(data.fields);
    });
});