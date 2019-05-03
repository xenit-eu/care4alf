/// <reference path="care4alf.ts" />

care4alf.filter('checkmark',  () => {
    return (input) => {
        return input ? '\u2713' : '\u2718';
    };
}).filter('stripPrefix',  () => {
    return (input) => {
        return input.replace('cm:', '');
    };
}).filter('humanBytes',  () => {
    return (fileSizeInBytes) => {
        var i = -1;
        var byteUnits = [' kB', ' MB', ' GB', ' TB', 'PB', 'EB', 'ZB', 'YB'];
        do {
            fileSizeInBytes = fileSizeInBytes / 1024;
            i++;
        } while (fileSizeInBytes > 1024);

        return Math.max(fileSizeInBytes, 0.1).toFixed(1) + byteUnits[i];
    }
}).filter("hash", function () {
    return function (input) {
        return input.replace(/^(workspace|archive|system|user):\/\/(\w+)\/(.+)$/, "$1+$2+$3");
    }
});
