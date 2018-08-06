/// <reference path="care4alf.ts" />

care4alf.controller('messages', ($scope, $http: ng.IHttpService) => {
    $http.get('messages/bundles').success((result) => {
        $scope.bundles = result;
    });

    $scope.translate = (raw) => {
      $http.post('messages/translate', {key: raw}).success((result) => {
        $scope.translated = result;
      });
    }
});
