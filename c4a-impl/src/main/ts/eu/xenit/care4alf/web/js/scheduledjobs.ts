care4alf.controller('scheduledjobs', ($scope,$http: ng.IHttpService) => {
    $scope.orderByField = 'NextFireTime';
    $scope.reverseSort = true;
    $scope.selectedGroup = {name:"DEFAULT"};
    $scope.jobcategory = 'available';

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

    $scope.switchCategory = (category) => {
        getData();
        if ($scope.jobcategory != category) {
            $scope.jobcategory = category;
        }
    }

    function getData(){
        $http.get('scheduled/job?groupname='+$scope.selectedGroup.name).success((jobs) => {
            $scope.jobs = jobs;
        });
        $http.get('scheduled/executing').success((executing) => {
            $scope.executing = executing;
        });
    }

    function generateDropdown(){
        $http.get('scheduled/groups').success((groups) => {
            $scope.groups = groups;
        });
    }
});