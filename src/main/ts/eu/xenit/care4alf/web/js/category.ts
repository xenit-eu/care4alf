/// <reference path="care4alf.ts" />

care4alf.controller('category',['$scope', '$http', function ($scope, $http: ng.IHttpService) {
    $scope.go = function() {
        var fd = new FormData();
        fd.append('file', $scope.files[0]);
        fd.append('name', 'testname');
        fd.append('namespace', "testSpace");
        $scope.uploadFile(fd).then((response) => {
            $scope.succes = "JSON succesfully uploaded."
        });
    };

    $scope.category = {};

    $scope.setFiles = (element) => {
        $scope.$apply(($scope) => {
            console.log('files:', element.files);
            // Turn the FileList object into an Array
            $scope.files = [];
            for (var i = 0; i < element.files.length; i++) {
                $scope.files.push(element.files[i])
            }
        });
    };

    $scope.uploadFile = function($data) {
        // $http() returns a $promise that we can add handlers with .then()
        return $http({
            method: 'POST',
            url: "category",
            data: $data,
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}
        })
    }
}]);
