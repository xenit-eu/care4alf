<!DOCTYPE html>
<html lang="en" ng-app="care4alf">
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>Care4Alf</title>
    <script type="text/javascript" src="${url.serviceContext}/xenit/care4alf/resources/js/underscore.1.5.2.cache.min.js"></script>
    <script type="text/javascript" src="${url.serviceContext}/xenit/care4alf/resources/js/angular.1.2.7.cached.js"></script>
    <script type="text/javascript" src="${url.serviceContext}/xenit/care4alf/resources/js/angular-resource.1.2.7.cached.js"></script>
    <script type="text/javascript" src="${url.serviceContext}/xenit/care4alf/resources/js/angular-route.1.2.7.cached.js"></script>
    <script type="text/javascript" src="${url.serviceContext}/xenit/care4alf/resources/js/care4alf.js"></script>
    <script type="text/javascript">
    	var care4alfModules = [<#list modules as module>{id: '${module.id}', description: '${module.description}'}<#if module_has_next>,</#if></#list>];
    </script>
    <link rel="stylesheet" href="${url.serviceContext}/xenit/care4alf/resources/css/bootstrap.3.0.0.cache.min.css">
    <link rel="stylesheet" href="${url.serviceContext}/xenit/care4alf/resources/css/care4alf.css">
</head>
<body>
<div class="title">
	<a href="#/"><h1>Care4Alf</h1></a>
</div>
<ul class="menu">
<#list modules as module>
	<li title="${module.description}"><a href="#${module.id}">${module.id}</a></li>
</#list>
</ul>
<div class="body">
    <ng-view />
</div>
</body>
</html>