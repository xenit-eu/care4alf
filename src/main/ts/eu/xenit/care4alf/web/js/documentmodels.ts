care4alf.controller('documentmodels', function($scope,$http: ng.IHttpService, $q: ng.IQService) {
    $scope.getInvalid =  () => {
        $http.get('documentmodels/invalidtypes').success(function(invalidTypes) {
            $scope.invalidTypes = invalidTypes;
        });
    };

    $scope.delete = function(document) {
        var deferred = $q.defer();
        $http.delete('documentmodels/node/' + document.id).success(function () {
            $scope.invalidTypes.splice($scope.invalidTypes.indexOf(document), 1);
            deferred.resolve();
        }).error(function(data) {
            console.log("failed to delete %o because of %o", document, data);
            document.error = data;
            deferred.resolve();
        });
        return deferred.promise;
    };

    $scope.deleteAll =  () => {
        var index = 0;
        while ($scope.invalidTypes.length > 0) {
            if (angular.isUndefined($scope.invalidTypes[index].error)) {
                $scope.delete($scope.invalidTypes[index]).then($scope.deleteAll);
                break;
            } else {
                index++;
            }
        }
    };

    $http.get('documentmodels/models').success(function(models) {
        $scope.models = models;
    });

    $scope.removeModel = function(model) {
        $http.delete('documentmodels/model?modelQName=' + model).success(function () {
            $scope.models.splice($scope.models.indexOf(model), 1);
        }).error(function(error) {
            alert("Failed to remove model: " + error);
        });
    };
});