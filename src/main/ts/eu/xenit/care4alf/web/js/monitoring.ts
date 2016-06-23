/// <reference path="care4alf.ts" />

care4alf.controller('monitoring', ($scope,$http: ng.IHttpService) => {
    $scope.vars={};
    $scope.info={
        "properties.residual":{
                "description":'Number of <a href="properties/residual">residual</a> properties'
            },
        "solr.errors":{
            "description":'Number of Solr errors. Get an <a href="solr/errors/nodes">overview</a> of the nodes not indexed and try to <a href="#/solradmin">fix</a> them accordingly.'
        }
    };
    $http.get('monitoring/vars').success((result:any) => {
        $scope.vars=result;
    });
});