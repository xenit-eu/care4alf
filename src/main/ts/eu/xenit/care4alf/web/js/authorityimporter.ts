///<reference path="care4alf.ts"/>

care4alf.controller('authorityimporter', ($scope,$http: ng.IHttpService) => {

    $scope.input = '';
    $scope.inputParsed = null;

    $scope.input = '[{"name":"GROUP_ALFRESCO_ADMINISTRATORS","groups":[],"users":["admin"]},{"name":"GROUP_ALFRESCO_SEARCH_ADMINISTRATORS","groups":[],"users":["admin"]},{"name":"GROUP_EMAIL_CONTRIBUTORS","groups":[],"users":["admin"]},{"name":"GROUP_EXAMPLE","groups":[{"name":"GROUP_EXAMPLE_SUB","groups":[],"users":["jantje"]}],"users":[]},{"name":"GROUP_SITE_ADMINISTRATORS","groups":[],"users":["admin"]}]';
    $scope.inputParsed = [{"name":"GROUP_ALFRESCO_ADMINISTRATORS","groups":[],"users":["admin"]},{"name":"GROUP_ALFRESCO_SEARCH_ADMINISTRATORS","groups":[],"users":["admin"]},{"name":"GROUP_EMAIL_CONTRIBUTORS","groups":[],"users":["admin"]},{"name":"GROUP_EXAMPLE","groups":[{"name":"GROUP_EXAMPLE_SUB","groups":[],"users":["jantje"]}],"users":[]},{"name":"GROUP_SITE_ADMINISTRATORS","groups":[],"users":["admin"]}];

    $scope.doInput = function doInput(input) {
        try {
            $scope.inputParsed = JSON.parse(input);
            console.log($scope.inputParsed);
        } catch (e) {
            $scope.inputParsed = null;
        }
    };

    $scope.submitInput = function submitInput() {

        console.log('submitting...');

        $http.post('authorityimporter/import', $scope.inputParsed).then((res) => {
            console.log(res);
        })
            .catch((e) => console.error(e))
            .finally(() => {
                console.log('finished');
            })

    }

});

class AuthorityObject {

}