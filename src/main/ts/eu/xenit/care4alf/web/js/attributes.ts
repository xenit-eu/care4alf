/// <reference path="care4alf.ts" />

care4alf.controller('attributes', function($scope, $http) {
    $scope.attributes = {};

    var getAttributes = () => {
        $http.get("attributes/").success(function(attributes) {
                $scope.attributes = attributes;
        })
    };

    getAttributes();

    $scope.remove = function(keys) {
        $http.delete("attributes/?keys=" + keys.slice(0,3).join(";")).success(() => {
            getAttributes();
        });
    };
});