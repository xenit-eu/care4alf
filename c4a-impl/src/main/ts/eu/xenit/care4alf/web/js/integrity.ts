/// <reference path="care4alf.ts" />

interface Report {
    scannedNodes: number;
    "runtime (ms)": number;
    startTime: string;
    endTime: string;
    nodeProblems: { [node: string]:[{
        message: string;
        extraMessage?: string;
        property?: string;
    }]; };
    fileProblems: { [file: string]:[{
        message: string;
    }]}
}

care4alf.controller('integrity', function($scope, $http, $window, $routeParams, $location) {
    $scope.hasReport = false;
    $scope.reportLoaded = false;
    $scope.scanRunning = false;
    $scope.report = "";
    $scope.summary = "";

    $scope.hasSubsetReport = false;
    $scope.subsetReport = ""
    $scope.subsetScanRunning = false;

    $scope.progress = {'nodeProgress': 0, 'fileProgress': 0}



    let makeReport = function(data) {
        let report:Report = data;
        // WONTFIX angular bug relating to keys starting with dollar signs https://github.com/angular/angular.js/issues/6266
        // we clean the keys up first by prepending \0 to them.
        let fixPrefixes = (obj) => Object.keys(obj).filter((key) => key != undefined && key[0] == '$').forEach((key) => {
            Object.defineProperty(obj, '\0' + key, Object.getOwnPropertyDescriptor(obj, key));
            delete obj[key];
        });
        fixPrefixes(report.nodeProblems);
        fixPrefixes(report.fileProblems);
        return report;
    }

    let load = function() {
        $http.get("integrity/summary").then((resp) => {
            $scope.hasReport = true;
            $scope.summary = resp.data;
        }, (resp) => {
            $scope.hasReport = false;
        });
        $http.get("integrity/progress").then((resp) => $scope.progress = resp.data);
        $http.get("/alfresco/s/xenit/care4alf/scheduled/executing").then((resp) => {
            resp.data.forEach(job => {
                // These strings are defined in the @ScheduledJob annotation in IntegrityScanner.java
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

    $scope.loadReport = function() {
        $http.get("integrity/report").then((resp) => {
            $scope.hasReport = true;
            $scope.reportLoaded = true;
            $scope.report = makeReport(resp.data);
        }, (resp) => {
            $scope.hasReport = false;
        });
    }

    $scope.scanSubset = function(noderefString:string, fileString:string) {
        $scope.subsetScanRunning = true;
        let clean = (s:string) => s.split(",").map((x) => x.trim()).filter((x) => x != "");
        let noderefs = clean(noderefString || "");
        let files = clean(fileString || "");
        $http.post("integrity/subset", {'nodes': noderefs, 'files' : files}).then((resp) => {
            $scope.subsetScanRunning = false;
            $scope.hasSubsetReport = true;
            $scope.subsetReport = makeReport(resp.data);
        }, (resp) => {
            $scope.subsetScanRunning = false;
            $scope.hasSubsetReport = false;
        });
    }

    $scope.cancel = function() {
        if ($window.confirm("Are you sure you want to cancel all scans? You will not get a report from them")) {
            $http.post("integrity/cancel")
        }
    }

    load();
}).directive('reportSummary', function() {
    return {
        restrict: 'E',
        scope: {
            summary: '=summary'
        },
        templateUrl: 'resources/partials/integrity-summary.html'
    }
}).directive('reportRenderer', function() {
    return {
        restrict: 'E',
        scope: {
            report: '=report',
            renderInline: '=renderInline',
            downloadEmbedded: '=downloadEmbedded'
        },
        templateUrl: 'resources/partials/integrity-renderer.html',
        controller: ($scope) => {
            $scope.doRender = () => $scope.renderInline = true;
            $scope.isEmpty = (obj) => Object.keys(obj).length === 0;
            $scope.countProblems = (problemObj) => Object.keys(problemObj).map((k) => problemObj[k].length).reduce((a,b) => a + b)
        }
    }
}).filter('nodelink', () => (noderef:string) => noderef.replace('workspace://SpacesStore/',
        '/alfresco/s/xenit/care4alf/#/browser/workspace+SpacesStore+')
).config(['$compileProvider', ($compileProvider) => $compileProvider.aHrefSanitizationWhitelist(/^\s*(https?|ftp|mailto|data)/)]);