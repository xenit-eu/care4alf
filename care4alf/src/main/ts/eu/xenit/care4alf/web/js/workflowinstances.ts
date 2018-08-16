/// <reference path="care4alf.ts" />

care4alf.controller('workflowinstances', function($scope, $resource: ng.resource.IResourceService, $http: ng.IHttpService, $window: ng.IWindowService) {
    $scope.idpattern = /^\w+\$\d+$/;

    $scope.taskPropQname = ""
    $scope.taskPropType = ""
    $scope.taskPropVal = ""

    var instanceResultHandler = function (instances) {
        $scope.instances = instances;
    };

    $scope.cancelWorkflow = function(instance) {
        $http.delete('workflow/instances/' + instance.id + '/cancel').success(() => {
            $scope.instances.splice($scope.instances.indexOf(instance), 1);
        }).error(function(error) {
            $window.alert(error);
        });
    };

    $scope.deleteWorkflow = (instance) => {
        $http.delete('workflow/instances/' + instance.id + '/delete').success(() => {
            $scope.instances.splice($scope.instances.indexOf(instance), 1);
        }).error(function(error) {
            $window.alert(error);
        });
    };

    $scope.loadTasks = (instance) => {
        $http.get('workflow/instances/' + instance.id + "/tasks").success((tasks) => {
            instance.tasks = tasks;
        });
    };

    $scope.releaseTask = (instance, taskId) => {
        $http.post('workflow/instances/tasks/' + taskId + '/release', '').success((task:{id:string}) => {
            // Update matching task in frontend
            instance.tasks = instance.tasks.map((t) => t.id == task.id ? task : t);
        }).error((error) => {
            $window.alert(error);
        });
    };

    $scope.setTaskProperty = (instance, taskId, propQname, propType, propVal) => {
        $http.post('workflow/instances/tasks/' + taskId + '/setProperty',
            {
                'qname': propQname,
                'type': propType,
                'value': propVal
            }
        ).success((task:{id:string}) => {
            instance.tasks = instance.tasks.map((t) => t.id == task.id ? task : t);
        }).error((error) => {
            $window.alert(error);
        });
    };


    $scope.findInstances = () => {
        if (angular.isDefined($scope.taskid) && $scope.taskid.length > 0) {
            $http.get('workflow/instances/find/task/' + $scope.taskid).success(instanceResultHandler);
        } else if (angular.isDefined($scope.instanceid) && $scope.instanceid.length > 0) {
            $http.get('workflow/instances/find/instance/' + $scope.instanceid).success(instanceResultHandler);
        }
    };

    $scope.findAllActive = () => {
        $http.get('workflow/instances/active').success(instanceResultHandler);
    };

    $scope.deleteAllActive = () => {
        if ($window.confirm('Are you sure you want to delete all active workflows ?')) {
            $http.delete('workflow/instances/active');
        }
    };
});