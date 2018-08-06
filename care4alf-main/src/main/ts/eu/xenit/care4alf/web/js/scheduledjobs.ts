care4alf.controller('scheduledjobs', ($scope,$http: ng.IHttpService) => {
    $scope.orderByField = 'NextFireTime';
    $scope.reverseSort = true;
    $scope.selectedGroup = {name:"DEFAULT"};

    generateDropdown();
    getData();

    $scope.execute = (name, group) => {
        $http.post('scheduled/job/' + name + '/' + group.name + '/execute', {}).error((error) => {
            alert(error.message);
            console.log("failed to execute %o", error);
        });
    };

    $scope.update = () =>{
        getData();
    };

    function getData(){
        $http.get('scheduled/job?groupname='+$scope.selectedGroup.name).success((jobs) => {
            $scope.jobs = jobs;
        });
    }

    function generateDropdown(){
        $http.get('scheduled/groups').success((groups) => {
            $scope.groups = groups;
        });
    }
});