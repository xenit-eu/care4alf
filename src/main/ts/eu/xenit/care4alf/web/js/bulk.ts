care4alf.controller('bulk', ($scope,$http: ng.IHttpService) => {
    $scope.form = {};
    $scope.form.query = "PATH:\"/app:company_home/st:sites/cm:swsdp/cm:documentLibrary/cm:Agency_x0020_Files//*\" AND TYPE:\"cm:content\"";
    $scope.form.batchsize = 20;
    $scope.form.threads = 2;
    $scope.form.actions = ["delete","archive"];
    $scope.form.action = "delete";
    $scope.result = "";

    $http.get("bulk/stores")
        .success(function (data) {
            $scope.form.stores = data;
            $scope.form.store = "workspace://SpacesStore";
        });

    $scope.execute = () => {
        $http.post("bulk/action/"+$scope.form.action, $scope.form, {headers: {'Content-Type': 'application/json'} })
            .then(function (response) {
                $scope.result = response.data;
            });
    };
});