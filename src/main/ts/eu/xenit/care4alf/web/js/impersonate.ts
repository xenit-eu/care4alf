/// <reference path="care4alf.ts" />

care4alf.controller('impersonate', ($scope, $http:ng.IHttpService) => {
    $scope.genTicketLoading = false;
    $scope.impuser = {};
    $scope.testFrameSrc = "/";
    $scope.testCmName = {};

    $scope.getTicketForUser = (user) => {
        $scope.genTicketLoading = true;
        $http.get("impersonate/ticket/" + user).success((data:{name:string,noderef:string,ticket:string}) => {
            $scope.impuser = data;
            $scope.userNode = data.noderef;
        }).error(() => alert("Failed to retrieve ticket for user. Details in the network tab or server logs."));
        $scope.genTicketLoading = false;
    }

    $scope.testInFrame = (node, ticket) => {
        // Reset the src first, because reload
        $scope.testFrameSrc = "/";
        // Parse the noderef
        var parsedNode = parseNodeRef(node);
        var space = parsedNode[1];
        var store = parsedNode[2];
        var nodeid = parsedNode[3];
        // Navigate the iframe to the Alfresco API with the ticket auth
        $scope.testFrameSrc = "/alfresco/s/api/node/" + space + "/" + store + "/" + nodeid + "/content?alf_ticket=" + $scope.impuser.ticket;
    }

    $scope.testCmName = (node, userid) => {
        var parsedNode = parseNodeRef(node);
        var space = parsedNode[1];
        var store = parsedNode[2];
        var nodeid = parsedNode[3];
        $http.get("impersonate/prop/" + space + "/" + store + "/" + nodeid + "/" + userid).success((data:{"cm:name":string}) => {
            $scope.testCmName.success = true;
            $scope.testCmName.message = "Successfully retrieved name: " + data["cm:name"];
        }).error((error, status) => {
            $scope.testCmName.success = false;
            $scope.testCmName.message = "Failed. Error is: ";
            $scope.testCmName.error = error;
        });
    }
});

function parseNodeRef(node: string): string[] {
    return node.match(/([^:]+):\/\/([^\/]+)\/(.*)/);
}
