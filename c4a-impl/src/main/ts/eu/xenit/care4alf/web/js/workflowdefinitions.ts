/// <reference path="care4alf.ts" />

care4alf.controller('workflowdefinitions', function($scope,$resource: ng.resource.IResourceService,$http: ng.IHttpService) {
    var Definition = $resource('workflow/definitions/:workflowId', {workflowId:'@id'});

    $scope.definitions = Definition.query();

    $scope.deleteDefinition = function(definition) {
        $http.delete('workflow/definitions', {params: {workflowId: definition.id}}).then( () => {
            $scope.definitions.splice($scope.definitions.indexOf(definition), 1);
        }, function(error) {
            alert(error.data);
        });
    };
});