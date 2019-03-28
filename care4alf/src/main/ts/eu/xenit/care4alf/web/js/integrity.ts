/// <reference path="care4alf.ts" />

care4alf.controller('integrity', function($scope, $http, $routeParams, $location) {
    $scope.hasReport = false;
    $scope.scanRunning = false;
    $scope.hasNodeProblems = false;
    $scope.hasFileProblems = false;
    $scope.report = "";


    $scope.isEmpty = (obj) => Object.keys(obj).length === 0;

    let load = function() {
        $http.get("/alfresco/s/xenit/care4alf/integrity/report").then((resp) => {
            $scope.hasReport = true;
            $scope.report = resp.data;
            $scope.hasNodeProblems = Object.keys(resp.data.nodeProblems).length !== 0;
            $scope.hasFileProblems = Object.keys(resp.data.fileProblems).length !== 0;
            console.log(resp.data);
        }, (resp) => {
            $scope.hasReport = false;
            console.log(resp.data);
        });
        $http.get("/alfresco/s/xenit/care4alf/scheduled/executing").then((resp) => {
            resp.data.forEach(job => {
                // These strings are define in the @ScheduledJob annotation in IntegrityScanner.java
                if (job.group == 'integrityscan' && job.name == 'IntegrityScan') {
                    $scope.scanRunning = true;
                    $scope.scanRunningSince = job.firetime;
                    return;
                }
            });
        });
        // no early return due to running job => no job is running
        $scope.scanRunning = false;
        $scope.scanRunningSince = null;
    };

    load();
}).filter('nodelink', () => (noderef:string) => noderef.replace('workspace://SpacesStore/',
        '/alfresco/s/xenit/care4alf/#/browser/workspace+SpacesStore+'));