angular.module('peercds.routes',[]).config(function($routeProvider){
	$routeProvider.when("/",
		{
			controller:	"mainCntrl"
		}
	)
	.otherwise({ redirectTo:"/"});
});