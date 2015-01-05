/// <reference path="care4alf.ts" />

care4alf.controller('tickets', ($scope,$http: ng.IHttpService) => {
    var getUsers = () => {
        $http.get('tickets/list').success((users) => {
            $scope.users = users;
        });
    };

    getUsers();

    $scope.expire = (username) => {
        $http.delete('tickets/expire/' + username).success(() => {
            getUsers();
        })
    };
});