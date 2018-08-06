/// <reference path="care4alf.ts" />

class DataLists {
    constructor(private $q: ng.IQService, private $http: ng.IHttpService) {}

    private aspects: any;
    private types: any;

    getAspects(): ng.IPromise<any> {
        return this.$q.when(this.aspects || this.$http.get(serviceUrl + "/xenit/care4alf/browser/aspects").then((result) => {
            this.aspects = result.data;
            return this.aspects;
        }))
    }

    getTypes(): ng.IPromise<any> {
        return this.$q.when(this.types || this.$http.get(serviceUrl + "/xenit/care4alf/browser/types").then((result) => {
            this.types = result.data;
            return this.types;
        }))
    }
}

care4alf.service("DataLists", DataLists);
