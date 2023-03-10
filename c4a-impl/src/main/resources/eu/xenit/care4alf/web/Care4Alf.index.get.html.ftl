<!DOCTYPE html>
<html lang="en" ng-app="care4alf" ng-csp>
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="Content-Security-Policy"
          content="default-src 'self' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; script-src 'self';">
    <title>Care4Alf</title>
    <#assign cached = url.serviceContext + "/xenit/care4alf/cached/" + versionDate?c!"1">
    <#assign resources = url.serviceContext + "/xenit/care4alf/resources">
    <script type="text/javascript" src="${resources}/js/upload/angular-file-upload-shim.min.js"></script>
    <script type="text/javascript" src="${resources}/js/underscore.1.5.2.cached.min.js"></script>
    <script type="text/javascript" src="${resources}/js/moment.1.7.2.cached.min.js"></script>
    <script type="text/javascript" src="${resources}/js/angular/angular.js"></script>
    <script type="text/javascript" src="${resources}/js/angular-route/angular-route.min.js"></script>
    <script type="text/javascript" src="${resources}/js/angular-sanitize/angular-sanitize.min.js"></script>
    <script type="text/javascript" src="${resources}/js/angular-resource/angular-resource.min.js"></script>
    <script type="text/javascript" src="${resources}/js/angular-animate/angular-animate.min.js"></script>
    <script type="text/javascript" src="${resources}/js/angularjs-toaster/toaster.js"></script>
    <script type="text/javascript" src="${resources}/js/upload/angular-file-upload.js"></script>
    <script type="text/javascript" src="${resources}/js/ui-bootstrap-tpls-1.1.2.cached.min.js"></script>
    <script type="text/javascript" src="${resources}/js/angular-loading-bar/loading-bar.js"></script>
    <script type="text/javascript" src="${cached}/js/care4alf.js"></script>
    <script type="text/javascript" src="${resources}/js/c4a/c4aModules.js"></script>
    <link rel="stylesheet" href="${resources}/css/bootstrap.3.1.1.cached.min.css">
    <link rel="stylesheet" href="${resources}/js/angularjs-toaster/toaster.css">
    <link rel="stylesheet" href="${resources}/js/angular-loading-bar/loading-bar.css">
    <link rel="stylesheet" href="${resources}/js/angular/angular-csp.css">
    <link rel="stylesheet" href="${cached}/css/care4alf.css">
    <!-- Icon by DinoSoftLabs on Flaticon.com -->
    <link rel="icon" href="${resources}/img/fire-extinguisher16.png" sizes="16x16" type="image/png" />
    <link rel="icon" href="${resources}/img/fire-extinguisher32.png" sizes="32x32" type="image/png" />
    <link rel="icon" href="${resources}/img/fire-extinguisher128.png" sizes="128x128" type="image/png" />
</head>
<body>
<div class="title">
    <span class="version">
        <span class="version-date" title="build time">${versionDate?number_to_datetime}</span>
        <span class="version-num" title="version">${version}</span>
    </span>
    <a href="#/"><h1>Care4Alf</h1></a>
</div>
<ul class="menu">
<#list modules as module>
    <li><a popover-placement="right" popover="${module.description}" popover-trigger="mouseenter" href="#${module.id}">${module.id}</a></li>
</#list>
</ul>
<div class="body">
    <toaster-container></toaster-container>
    <ng-view />
</div>
</body>
</html>
