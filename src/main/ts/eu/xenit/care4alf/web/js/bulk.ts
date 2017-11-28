care4alf.controller('bulk', ($scope, $http:ng.IHttpService, $timeout) => {
    $scope.form = {};
    $scope.form.query = "PATH:\"/app:company_home/st:sites/cm:swsdp/cm:documentLibrary/cm:Agency_x0020_Files//*\" AND TYPE:\"cm:content\"";
    $scope.form.batchsize = 20;
    $scope.form.threads = 2;
    $scope.form.batchnumber = 10;
    $scope.form.maxlag = 180;
    $scope.form.disableauditablepolicies = false;
    $scope.form.action = "";
    $scope.actions = {};
    $scope.canceled = "";

    $scope.data = {
        availableOptions: [
            {id: 'search', name: 'Search'},
            {id: 'file', name: 'File'}
        ],
        selectedOption: {id: 'search', name: 'Search'} //This sets the default value of the select in the ui
    };

    $http.get("bulk/listActions").success(function (data) {
        $scope.actions = data;
        var keys = Object.keys(data);
        if (keys.length > 0)
            $scope.form.action = keys[0];

    });
    $scope.form.parameters = {};
    $scope.loading = false;

    $scope.result = "";

    $scope.form.getActions = function () {
        return Object.keys($scope.actions);
    };

    $scope.form.getParameters = function (action) {
        return $scope.actions[action];
    };

    $http.get("bulk/stores")
        .success(function (data) {
            $scope.form.stores = data;
            $scope.form.store = "workspace://SpacesStore";
        });

    $scope.jobs = {};
    $scope.loadJobs = () => {
        $http.get("bulk/processors").success((data:any) => {
            $scope.jobs = data;
        })
    };
    $scope.clearJobs = () => {
        $http.delete("bulk/processors").success(()=> {
            $scope.loadJobs();
        });
    };
    $scope.cancelJobs = (index:number) => {
        $http.delete("bulk/cancel/"+index).success(()=> {
            $scope.canceled = "Current jobs succesfully cancelled";
            $timeout(() => {
                $scope.canceled = "";
            }, 2000);
        })
    };
    $timeout($scope.loadJobs, 5000);

    $scope.execute = () => {
        $scope.loading = true;
        $http.post("bulk/form/action/" + $scope.form.action, $scope.form, {headers: {'Content-Type': 'application/json'}})
            .then(function (response) {
                $scope.result = response.data;
                $scope.loading = false;
            }, function (error) {
                $scope.result = "Error: " + error;
                $scope.loading = false;
            });
    };

    var actionParameters = () => {
        var json = JSON.stringify($scope.form.parameters);
        return json;
    };


    $scope.executeFile = () => {
        $scope.loading = true;
        let callInfo =
            $http({
                method: 'POST',
                url: "bulk/form/action/" + $scope.form.action,
                headers: {
                    'Content-Type': 'multipart/form-data'
                },
                data: {
                    type: 'file',
                    file: $scope.form.file,
                    batchsize: $scope.form.batchsize,
                    threads: $scope.form.threads,
                    batchnumber: $scope.form.batchnumber,
                    maxlag: $scope.form.maxlag,
                    disableauditablepolicies : $scope.form.disableauditablepolicies,
                    parameters: actionParameters()
                },
                transformRequest: function (data, headersGetter) {
                    var formData = new FormData();
                    angular.forEach(data, function (value, key) {
                        formData.append(key, value);
                    });

                    var headers = headersGetter();
                    delete headers['Content-Type'];

                    return formData;
                }
            })
                .success(function (data) {
                    $scope.result = data;
                    $scope.loading = false;
                })
                .error(function (data, status) {
                    $scope.result = "Error: " + data;
                    $scope.loading = false;
                });
    };

    $scope.executeSearch = () => {
        $scope.loading = true;
        let callInfo =
            $http({
                method: 'POST',
                url: "bulk/form/action/" + $scope.form.action,
                headers: {
                    'Content-Type': 'multipart/form-data'
                },
                data: {
                    type: 'search',
                    workspace: $scope.form.store,
                    query: $scope.form.query,
                    batchsize: $scope.form.batchsize,
                    threads: $scope.form.threads,
                    batchnumber: $scope.form.batchnumber,
                    maxlag: $scope.form.maxlag,
                    disableauditablepolicies : $scope.form.disableauditablepolicies,
                    parameters: actionParameters()
                },
                transformRequest: function (data, headersGetter) {
                    var formData = new FormData();
                    angular.forEach(data, function (value, key) {
                        formData.append(key, value);
                    });

                    var headers = headersGetter();
                    delete headers['Content-Type'];

                    return formData;
                }
            })
                .success(function (data) {
                    $scope.result = data;
                    $scope.loading = false;
                })
                .error(function (data, status) {
                    $scope.result = "Error: " + data;
                    $scope.loading = false;
                });
    };

});


care4alf.directive('fileModel', ['$parse', function ($parse) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var model = $parse(attrs.fileModel);
            var modelSetter = model.assign;

            element.bind('change', function () {
                scope.$apply(function () {
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
}]);
