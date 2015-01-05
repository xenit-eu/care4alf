/// <reference path="care4alf.ts" />

care4alf.directive('bindHtmlUnsafe',  () => {
    return{
        restrict: 'A',
        link: (scope: ng.IScope,element: any, attr) => {
            var html = scope.$eval(attr.bindHtmlUnsafe);
            try {
                var root = element[0].webkitCreateShadowRoot();
                root.innerHTML = html;
            } catch (x) {
                element[0].innerHTML = html;
            }
        }
    }
}).directive('anchor', () => {
    return {
        restrict: 'A',
        link: function(scope, elem, attrs) {
            return elem.bind('click',  () => {
                var el = document.getElementById(attrs['anchor']);
                return el.scrollIntoView();
            });
        }
    };
}).directive('ngEnter', () => {
    return function (scope, element, attrs) {
        element.bind('keydown', function (event) {
            if(event.which === 13) {
                scope.$apply(function (){
                    scope.$eval(attrs.ngEnter, {$event:event});
                });
                event.preventDefault();
            }
        });
    };
});