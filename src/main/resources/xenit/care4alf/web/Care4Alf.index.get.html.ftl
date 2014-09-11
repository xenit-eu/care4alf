<!DOCTYPE html>
<html lang="en" ng-app="care4alf">
<head>
	<#assign cached = url.serviceContext + "/xenit/care4alf/cached/" + version?c!"1">
	<#assign resources = url.serviceContext + "/xenit/care4alf/resources">
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Care4Alf</title>
    <script type="text/javascript" src="${resources}/js/underscore.1.5.2.cached.min.js"></script>
    <script type="text/javascript" src="${resources}/js/angular.js"></script>
    <script type="text/javascript" src="${resources}/js/angular-resource.js"></script>
    <script type="text/javascript" src="${resources}/js/angular-route.js"></script>
    <script type="text/javascript" src="${resources}/js/angular-sanitize.js"></script>
    <script type="text/javascript" src="${resources}/js/ui-bootstrap-tpls-0.10.0.min.js"></script>
    <script type="text/javascript" src="${cached}/js/care4alf.js"></script>
    <script type="text/javascript">
    	var care4alfModules = [<#list modules as module>{id: '${module.id}', description: '${module.description}'}<#if module_has_next>,</#if></#list>];
	    var serviceUrl = "${url.serviceContext}";
    </script>
    <link rel="stylesheet" href="${resources}/css/bootstrap.3.1.1.cached.min.css">
    <link rel="stylesheet" href="${cached}/css/care4alf.css">
</head>
<body>
<div class="title">
	<span class="version" title="build time">${version?number_to_datetime}</span>
	<a href="#/"><h1>Care4Alf</h1></a>
</div>
<ul class="menu">
<#list modules as module>
	<li title="${module.description}"><a href="#${module.id}">${module.id}</a></li>
</#list>
</ul>
<div class="body">
	<div ng-if="requestError.message" class="alert alert-danger">{{requestError.message}}</div>
    <ng-view />
</div>
</body>
</html>