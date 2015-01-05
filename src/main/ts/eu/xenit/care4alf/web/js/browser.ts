/// <reference path="care4alf.ts" />

care4alf.controller('browser', ($scope,$upload, $http, $routeParams) => {
    if (angular.isDefined($routeParams.subtoken)) {
        var noderef = $routeParams.subtoken.replace(/^(\w+)\+(\w+)\+(.+)$/, "$1://$2/$3");
        $http.get(serviceUrl + "/xenit/care4alf/browser/details", {params: {noderef: noderef}}).success((result) => {
            $scope.node = result;
        });
        $http.get(serviceUrl + "/xenit/care4alf/browser/aspects").success((aspects) => {
            $scope.aspects = aspects;
        });
    } else {
        $http.get(serviceUrl + "/xenit/care4alf/browser/rootNodes").success((rootNodes) => {
            $scope.results = rootNodes;
        });
    }

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

    $scope.deleteProperty = (property) => {
        $http.delete(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/properties/" + property).then(() => {
            delete $scope.node.properties[property];
        })
    };

    $scope.saveProperty = (property) => {
        $http.put(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/properties/" + property, {value: $scope.node.properties[property]});
    };

    $scope.addAspect = (aspect) => {
        $http.post(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/aspects/" + aspect).success(() => {
            $scope.node.aspects.push(aspect);
        });
    };

    $scope.removeAspect = (aspect) => {
        $http.delete(serviceUrl + "/xenit/care4alf/browser/" + $scope.node.noderef + "/aspects/" + aspect).success(() => {
           $scope.node.aspects = _.without($scope.node.aspects, aspect);
        });
    };
});
