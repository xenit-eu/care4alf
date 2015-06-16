care4alf.controller('scheduledjobs', ($scope,$http: ng.IHttpService) => {
    $scope.orderByField = 'NextFireTime';
    $scope.reverseSort = true;

    $http.get('scheduled/job').success((jobs) => {
        $scope.jobs = jobs;
    });

    $scope.execute = (name) => {
        $http.post('scheduled/job/' + name + '/execute', {}).error((error) => {
            alert(error.message);
            console.log("failed to execute %o", error);
        });
    };
});