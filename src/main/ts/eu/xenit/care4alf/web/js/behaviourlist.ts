care4alf.controller('behaviourlist', ($scope,$http: ng.IHttpService) => {
    console.log("We are inside");
    getData();

    function getData(){
        $http.get('behaviour/list').success((behaviours) => {
            $scope.behaviours = behaviours;
        });
    }
});
