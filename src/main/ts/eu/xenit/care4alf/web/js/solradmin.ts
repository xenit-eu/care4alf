/// <reference path="care4alf.ts" />

care4alf.controller('solradmin', ($scope,$http: ng.IHttpService) => {
    $scope.fields=[];
    $http.get('solr/proxy/alfresco/admin/luke?numTerms=0&wt=json').success((data:any) => {
        $scope.fields = Object.keys(data.fields);
    });

    $scope.result="";
    $scope.action=(operation)=>{
        var url = "solr/proxy/admin/cores?wt=json&action=" + operation;
        if($scope.nodeid) url += "&nodeid=" + $scope.nodeid;
        console.log(url);
        $http.get(url).success((data:any) => {
            $scope.result = data;
        });
    };

    $scope.errorFix=(exception,operation)=>{
        var url = "solr/errors/nodes/fix/"+exception+"?action="+operation;
        console.log(url);
        $http.post(url,{}).success((data:any) => {
            $scope.result = data;
        });
    }
});