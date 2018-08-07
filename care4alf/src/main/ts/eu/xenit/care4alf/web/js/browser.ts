/// <reference path="care4alf.ts" />

care4alf.controller('browser', ($scope,$upload, $http, $routeParams,$window: Window, DataLists) => {
    $scope.onFileSelect = function($files) {
        //$files: an array of files selected, each file has name, size, and type.
        for (var i = 0; i < $files.length; i++) {
            var file = $files[i];
            console.log("Upload file %o", file);
            $scope.upload = $upload.upload({
                url: serviceUrl + '/xenit/care4alf/browser/upload', //upload.php script, node.js route, or servlet url
                //method: 'POST' or 'PUT',
                //headers: {'header-key': 'header-value'},
                //withCredentials: true,
                data: {myObj: $scope.myModelObj},
                file: file // or list of files ($files) for html5 only
                //fileName: 'doc.jpg' or ['1.jpg', '2.jpg', ...] // to modify the name of the file(s)
                // customize file formData name ('Content-Disposition'), server side file variable name.
                //fileFormDataName: myFile, //or a list of names for multiple files (html5). Default is 'file'
                // customize how data is added to formData. See #40#issuecomment-28612000 for sample code
                //formDataAppender: function(formData, key, val){}
            }).progress(function(evt) {
                var percentage = 100.0 * evt.loaded / evt.total;
                console.log('percent: ' + percentage);
            }).success(function(data, status, headers, config) {
                // file is uploaded successfully
                console.log(data);
            });
            //.error(...)
            //.then(success, error, progress);
            // access or attach event listeners to the underlying XMLHttpRequest.
            //.xhr(function(xhr){xhr.upload.addEventListener(...)})
        }
    };

    $scope.addProperty = (qname, value) => {
        $http.put(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/properties/" + qname, {value: value}).success(() => {
            //$scope.node.properties[qname]=value;
        });
    };

    $scope.deleteProperty = (property) => {
        if ($window.confirm("Are you sure you want to delete this property?")) {
            $http.delete(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/properties/" + property).then(() => {
                delete $scope.node.properties[property];
            })
        }
    };

    $scope.saveProperty = (property) => {
        $http.put(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/properties/" + property, {value: $scope.node.properties[property]});
    };

    $scope.addAspect = (aspect) => {
        $http.post(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/aspects", {aspect: aspect}).success(() => {
            $scope.node.aspects.push(aspect);
        });
    };

    $scope.removeAspect = (aspect) => {
        $http.delete(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/aspects/" + aspect).success(() => {
           $scope.node.aspects = _.without($scope.node.aspects, aspect);
        });
    };

    $scope.setType = (newType) => {
        $http.put(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/type", {type: newType});
    };

    $scope.deleteNode = (node) => {
        if ($window.confirm("Are you sure you want to delete " + node.name + " ?")) {
            $http.delete(serviceUrl + "/xenit/care4alf/browser/" + node.noderef).success(() => {
                $scope.node.children = _.without($scope.node.children, node);
            });
            $window.history.back();
        }
    };

    $scope.deleteAssoc = (assoc) => {
        if ($window.confirm("Are you sure you want to delete the association with ID " + assoc.id + " ?")) {
            $http.delete(serviceUrl + "/xenit/care4alf/browser/assoc/" + assoc.id).success(() => {
               $scope.node.sourceAssocs = _.filter($scope.node.sourceAssocs, (sourceAssoc: any) => {
                   return sourceAssoc.id == assoc.id;
               });

               $scope.node.targetAssocs = _.filter($scope.node.targetAssocs, (targetAssoc: any) => {
                   return targetAssoc.id == assoc.id;
               });
            });
        }
    };

    $scope.deleteChild = (child) => {
        $http.post(serviceUrl + "/xenit/care4alf/browser/deletechild", {parent: $scope.node.noderef, child: child.noderef}).success(() => {
            $scope.node.children = _.without($scope.node.children, child);
        });
    };

    $scope.gotoContentUrl = (node) => {
        var store = node.properties["sys:store-identifier"];
        var space = node.properties["sys:store-protocol"];
        var uuid = node.properties["sys:node-uuid"];
        var name = node.properties["cm:name"];
        window.location.href = serviceUrl.slice(0,-10) + "share/page/document-details?nodeRef=" + space + "://" + store + "/" + uuid;
    };

    $scope.addChild = (newChildRef) => {
        $http.post(serviceUrl + "/xenit/care4alf/browser/child", {parent: $scope.node.noderef, child: newChildRef}).success(() => {
            $scope.node.children.push({noderef:newChildRef});
        });
    };

    var LS_QUERY = "care4alfquery";

    $scope.search = () => {
        var query: string = $scope.searchModel.query;
        var startTime = new Date();

        if (query == null || query.length == 0) {
            $http.get(serviceUrl + "/xenit/care4alf/browser/rootNodes").success((rootNodes) => {
                $scope.results = rootNodes;
            });
            $window.localStorage.removeItem(LS_QUERY)
        } else {
            // Allow pasting in a noderef
            $window.localStorage.setItem(LS_QUERY, query);

            $http.post(serviceUrl + "/xenit/care4alf/browser/find", query)
                .success((matches) => {
                    $scope.results = matches;
                    $scope.results.success = true;
                    $scope.times = {"start": startTime, "end": new Date()};
                })
                .error((err) => {
                    $scope.results.success = false;
                });
        }
    };

    $scope.searchModel = {};
    $scope.serviceUrl = serviceUrl;

    $scope.showhelp = false;
    $scope.toggleHelp = () => $scope.showhelp = !$scope.showhelp;

    if (angular.isDefined($routeParams.subtoken)) {
        var noderef = $routeParams.subtoken.replace(/^(\w+)\+(\w+)\+(.+)$/, "$1://$2/$3");
        $http.get(serviceUrl + "/xenit/care4alf/browser/details", {params: {noderef: noderef}}).success((result) => {
            $scope.node = result;
        });

        DataLists.getAspects().then((aspects) => {
            $scope.aspects = aspects;
        });

        DataLists.getTypes().then((types) => {
            $scope.types = types;
        });
    } else {
        $scope.searchModel.query = $window.localStorage.getItem(LS_QUERY);

        $scope.search();
    }
});
