/// <reference path="care4alf.ts" />

care4alf.controller('actions', ($scope,$http: ng.IHttpService) => {
    $http.get('actions/').success((actions) => {
        $scope.actions = actions;
    });

    $scope.execute = (action) => {
        $http.post('actions/' + action.name + '/run', {noderef: $scope.noderef}).error((error) => {
            alert(error.message);
            console.log("failed to execute %o", error);
        });
    };
});