care4alf.controller('bulk', ($scope,$http: ng.IHttpService) => {
    $scope.form = {};
    $scope.form.store = "workspace://SpacesStore";
    $scope.form.query = "PATH:\"/app:company_home/st:sites/cm:swsdp/cm:documentLibrary/cm:Agency_x0020_Files//*\" AND TYPE:\"cm:content\"";
    $scope.form.batchsize = 20;
    $scope.form.threads = 2;
    $scope.form.action = "delete";

    $scope.result = "";

    $scope.execute = () => {
        $http.post("bulk/"+$scope.form.action, $scope.form, {headers: {'Content-Type': 'application/json'} })
            .then(function (response) {
                $scope.result = response.data;
            });
    };
});