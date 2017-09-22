care4alf.controller('permissionimport',($scope,$http: ng.IHttpService) => {
    $scope.form = {};
    $scope.loading = false;

    $scope.result = "";

    $scope.execute = () => {
        $scope.loading = true;
        $scope.result = "Loading..."
        var formData = new FormData();
        formData.append('file', $scope.form.file);
        $http.post("permissionimport/importpermissions", formData, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        })
            .then(function (response){
                $scope.result = response.data;
                $scope.loading = false;
            },function (){
                $scope.result = "failed";
                $scope.loading = false;
            });

    };
});