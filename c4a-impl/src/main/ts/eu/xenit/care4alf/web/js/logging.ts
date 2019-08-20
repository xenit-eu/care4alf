/// <reference path="care4alf.ts" />

care4alf.controller("logging", ($scope, $http: ng.IHttpService) => {
    $http.get(serviceUrl + "/xenit/care4alf/logging/all").success((result) => {
        $scope.loggers = result;
        angular.forEach($scope.loggers, (logger: any) => {
            logger.touched = false;
        });
    });

    var  push = (logger) => {
        return $http.post(serviceUrl + "/xenit/care4alf/logging", logger).then(() => {
            logger.touched = false;
        });
    };
    $scope.apply = (logger) => {
        push(logger);
    };
    $scope.touch = (logger) => {
        logger.touched = true;
    }
    $scope.add = (logger) => {
        push(logger).then(() => {
            $scope.name = "";
            $scope.level = "";
            $scope.loggers.push(logger);
        });
    }
});