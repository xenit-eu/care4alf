/// <reference path="care4alf.ts" />

care4alf.controller('tail', ['$scope', '$timeout', '$http', ($scope, $timeout, $http:ng.IHttpService) => {
    
    var loop = true;
    $scope.tail = [];
    $scope.logloading = true;
    $scope.responseStatus = 'waiting';
    $scope.path = "/opt/alfresco/tomcat/logs/catalina.out";
    var getlog = function () {
        if(loop == true) {
            $http.get("tail/tails?n=" + $scope.lines.count + "&path=" + $scope.path).success((response:string) => {
                $scope.tail = angular.fromJson(response);
                $scope.responseStatus = 'success';
                $timeout(getlog, 5000);
            }).error((response) => {
                $scope.tail = response.data;
                $scope.responseStatus = 'error';
                $scope.logloading = false;
            });
        }
    };
    
    $scope.stop = function(){
          loop = false;
    };

    $scope.lineOptions = [{count: 100, number: "100"}, {count: 200, number: "200"},
        {count: 300, number: "300"}, {count: 400, number: "400"}, {count: 500, number: "500"}];

    $scope.lines = {count: 200, number: "200"};
    getlog();
    $scope.reloadcontent = function(){
      getlog();
    };
    document.getElementById('logtailbottom').scrollIntoView();
}]);

care4alf.filter('reverse', function () {
    return function (items) {
        return items.slice().reverse();
    };
});

