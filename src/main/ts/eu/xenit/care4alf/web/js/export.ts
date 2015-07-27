care4alf.controller('export', ($scope,$http: ng.IHttpService) => {
    $scope.form = {};
    $scope.form.query = "PATH:\"/app:company_home/cm:Projects_x0020_Home//*\" AND TYPE:\"cm:content\"";
    $scope.form.columns = "cm:name,cm:creator,type,path";
    $scope.form.separator = ",";
    $scope.form.nullValue = "null";
    $scope.form.documentName = "export.csv";
    $scope.form.amountDoc = 100;
    $scope.loading = false;

    var url = window.location.href;
    var website = url.substring(0, url.length-8) + "export/query";
    $scope.result = website;

    $scope.execute = () => {
        $scope.loading = true;
        var extra = "?";
        extra += "query=" + encodeURIComponent($scope.form.query);
        extra += "&columns=" + encodeURIComponent($scope.form.columns.toLowerCase());
        extra += "&separator=" + encodeURIComponent($scope.form.separator);
        extra += "&nullValue=" + encodeURIComponent($scope.form.nullValue);
        extra += "&amountDoc=" + encodeURIComponent($scope.form.amountDoc);
        extra += "&documentName=" + encodeURIComponent($scope.form.documentName);
        $scope.result = website + extra;
        window.open(website + extra);
        $scope.loading = false;
    };
});