/// <reference path="care4alf.ts" />

care4alf.controller('dummymail', ($scope, $http: ng.IHttpService) => {
    $http.get('smtp/list').success((messages) => {
        $scope.messages = messages;
    });

    $scope.clear = () => {
        $http.delete('smtp/list').success(() => {
            $scope.messages = [];
        });
    };

    $http.get("smtp/config").success((config) => {
        $scope.config = config;
    })
});