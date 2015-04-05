angular.module('peercds.services',[])
.service('getTorrents',function($http){
	this.getTorrentsList=$http.get('http://localhost:8080/peercds/service/gettorrents').
	success(function(res){
		return res;
	});
});