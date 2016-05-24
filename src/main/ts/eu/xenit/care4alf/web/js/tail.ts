/// <reference path="care4alf.ts" />

care4alf.controller('tail', ['$scope', '$timeout', '$http', ($scope, $timeout, $http:ng.IHttpService) => {

    $scope.tail = [];
    $scope.responseStatus = 'waiting';
    var getlog = function () {
        $http.get("tail/tails?n=" + $scope.lines.count).success((response:string) => {
            $scope.tail = angular.fromJson(response);
            $scope.responseStatus = 'success';
            $timeout(getlog, 5000);
        }).error((response) => {
            $scope.tail = response.data;
            $scope.responseStatus = 'error';
        });
    };

    $scope.lineOptions = [{count: 100, number: "100"}, {count: 200, number: "200"},
        {count: 300, number: "300"}, {count: 400, number: "400"}, {count: 500, number: "500"}];

    $scope.lines = {count: 200, number: "200"};
    getlog();
}]);

