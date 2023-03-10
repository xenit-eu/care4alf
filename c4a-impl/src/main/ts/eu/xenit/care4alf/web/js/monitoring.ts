/// <reference path="care4alf.ts" />

care4alf.controller('monitoring', ($scope,$http: ng.IHttpService) => {
    $scope.vars={};
    $scope.license={};
    $scope.info={
        "properties.residual":{
                "description":'Number of <a href="properties/residual">residual</a> properties'
        },
        "solr.errors":{
            "description":'Number of Solr errors. Get an <a href="solr/errors/nodes">overview</a> of the nodes not indexed and try to <a href="#/solradmin">fix</a> them accordingly.'
        },
        "solr.lag.nodes":{
            "description": 'Number of nodes to index. If this is too high, check the <a href="solr/transactions">transaction</a> sizes'
        },
        "days":{
            "description": 'Number of days remaining. Hardcoded to 999999999 in case of a Perpetual License.'
        },
        "users.max":{
            "description": 'Maximum number of users. Hardcoded to 999999999 in case there is no limit.'
        }
    };
    $http.get('monitoring/vars').success((result:any) => {
        $scope.vars = result;
    });
    $http.get('monitoring/license').success((result:any) => {
        $scope.license = result;
    });
});
