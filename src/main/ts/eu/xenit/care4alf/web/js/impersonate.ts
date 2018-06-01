/// <reference path="care4alf.ts" />

care4alf.controller('impersonate', ($scope, $http:ng.IHttpService) => {
    $scope.genTicketLoading = false;
    $scope.impuser= {};
    $scope.testFrameSrc = "/";

    $scope.getTicketForUser = (user) => {
        $scope.genTicketLoading = true;
        $http.get("impersonate/ticket/" + user).success((data:{name:string,noderef:string,ticket:string}) => {
            $scope.impuser = data;
            $scope.userNode = data.noderef;
        });
        $scope.genTicketLoading = false;
    }

    $scope.testInFrame = (node, ticket) => {
        // Reset the src first, because reload
        $scope.testFrameSrc = "/";
        // Parse the noderef
        var m = node.match(/([^:]+):\/\/([^\/]+)\/(.*)/);
        var ws = m[1];
        var store = m[2];
        var nodeid = m[3];
        // Navigate the iframe to the Alfresco API with the ticket auth
        $scope.testFrameSrc = "/alfresco/s/api/node/" + ws + "/" + store + "/" + nodeid + "/content?alf_ticket=" + $scope.impuser.ticket;
    }
});
