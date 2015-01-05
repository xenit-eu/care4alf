/// <reference path="care4alf.ts" />

care4alf.controller('spring', ($http: ng.IHttpService,$scope,$routeParams) => {
    var subToken = $routeParams.subtoken;
    var subUrl = '';
    if (angular.isDefined(subToken)) {
        subUrl = '/' + subToken;
    }
    $http.get('spring/beannames' + subUrl).success((beans) => {
        $scope.beans = beans;
    });
});