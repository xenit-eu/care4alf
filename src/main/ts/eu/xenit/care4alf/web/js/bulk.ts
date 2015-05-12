care4alf.controller('bulk', ($scope,$http: ng.IHttpService) => {
    $scope.form = {};
    $scope.form.query = "PATH:\"/app:company_home/st:sites/cm:swsdp/cm:documentLibrary/cm:Agency_x0020_Files//*\" AND TYPE:\"cm:content\"";
    $scope.form.batchsize = 20;
    $scope.form.threads = 2;
    $scope.form.action = "setproperty";
    $scope.actions = {
        delete:[],
        archive:[],
        settype:["Type"],
        setproperty:["Property","Value"]
    };
    $scope.parameters = {};

    $scope.result = "";

    $scope.form.getActions = function(){
        return Object.keys($scope.actions);
    };

    $scope.form.getParameters = function(action){
        return  $scope.actions[action];
    };

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