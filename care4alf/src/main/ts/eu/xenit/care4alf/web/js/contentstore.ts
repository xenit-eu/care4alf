/// <reference path="care4alf.ts" />

care4alf.controller('contentstore', ($scope,$http: ng.IHttpService) => {
    var get = () => {
        $http.get('contentstore/diskusagebyowner').success((usage: any[]) => {
            $scope.sortedUsage = _.sortBy(usage, 'workspace');
        });
    };

    get();

    $scope.updateStats = () => {
        $http.put('contentstore/updatediskusage', null).success(() => {
            get();
        });
    };

    $scope.checkIntegrity = () => {
        $http.get('contentstore/checkintegrity').success((missing) => {
            $scope.missing = missing;
        })
    };
});