care4alf.controller('behaviourlist', ($scope,$http: ng.IHttpService) => {
    getData();

    function getData(){
        $http.get('behaviour/list').success((policies) => {
            $scope.policies = policies;
        });
    }
});