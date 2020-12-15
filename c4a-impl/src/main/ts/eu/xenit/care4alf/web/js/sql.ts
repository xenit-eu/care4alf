/// <reference path="care4alf.ts" />

care4alf.controller('sql', ($scope, $http: ng.IHttpService, $q) => {
    $scope.sql = {};
    $scope.sql.query = "SELECT 1";
    $scope.query = () => {
        var query: string = $scope.sql.query;
        var queryRequest = executeQueryRequest(query);
        queryRequest.then((results) => {
            $scope.results = results;
            $scope.results.success = true;
        },(err) => {
            $scope.results.success = false;
            $scope.results.error = err;
        });
    };

    getData();

    function getData() {
        $http.get('queries').success((results) => {
            $scope.queries = results;
        });
    }

    $scope.update = () => {
        $scope.sql.query = $scope.selectedQuery.query;
    };

    function executeQueryRequest(query) {
        var timeout = $q.defer(),
            httpRequest;
        setTimeout(handleTimeout(timeout), 5 * 60 * 1000);
        httpRequest = $http({method: 'post', url: 'sql', data: {query: query}, cache: false, timeout: timeout.promise});
        return httpRequest;
    }

    function handleTimeout(timeout) {
        console.log("Timeout triggered, aborting query on remote");
        timeout.resolve()
    }

});
