/// <reference path="care4alf.ts" />

care4alf.controller("logging", ($scope, $http: ng.IHttpService) => {
    $http.get(serviceUrl + "/xenit/care4alf/logging/all").success((result) => {
        $scope.loggers = result;
        angular.forEach($scope.loggers, (logger: any) => {
            logger.original = logger.level;
        });
    });

    var  push = (logger) => {
        return $http.post(serviceUrl + "/xenit/care4alf/logging", logger).then(() => {
            logger.original = logger.level;
        });
    };
    $scope.apply = (logger) => {
        push(logger);
    };

    $scope.add = (logger) => {
        push(logger).then(() => {
            $scope.name = "";
            $scope.level = "";
            $scope.loggers.push(logger);
        });
    }
});