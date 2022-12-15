/// <reference path="care4alf.ts" />

care4alf.controller('dictionary', function($scope,$http: ng.IHttpService) {
    $http.get(serviceUrl + '/api/dictionary').success(function(types) {
        $scope.types = types;
    });
});