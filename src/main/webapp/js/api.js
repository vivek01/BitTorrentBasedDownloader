var peerApi={
		uuid:'',

		getTorrents:function(status){
			$.ajax({
				url:"http://localhost:8080/peercds/service/gettorrents",
				type:"GET",
				success:function(res){
					myobject.torrentsList=res;
					$('#torrent-rows').empty();
					for(var i=0;i<res.length;i++){
						if(res[i].error==true){
							$('col-md-3 col-sm-6 col-xs-4').hide();
							myobject.currentTorrentState="error";
							console.log(myobject.currentTorrentState);
							var table='<tr class="file" id='+i+'>';
							table=table+'<td class="uuid" style="display:none">'+res[i].uuid+'</td>';
							table=table+'<td class="fid">'+i+'</td>';
							table=table+'<td class="file_name">'+res[i].fileName+'</td>';
							table=table+'<td class="status">Error</td>';
							table=table+'<td class="prog"></td>';
							table=table+'<td class="eta"></td>';
							table=table+'<td class="size"></td>';
							$('#torrent-rows').append(table);
							
							
							if(myobject.selectedFile!=""){
								$('#torrent-rows tr').eq(myobject.selectedFile).addClass('info');
							}

						} else if(res[i].paused==true){
							myobject.currentTorrentState="paused";
							var sno=i+1;
							var table='<tr class="file" id='+i+'>';
							table=table+'<td class="uuid" style="display:none">'+res[i].uuid+'</td>';
							table=table+'<td class="fid">'+sno+'</td>';
							table=table+'<td class="file_name">'+res[i].fileName+'</td>';
							table=table+'<td class="status">Paused</td>';
							table=table+'<td class="prog">N/A</td>';
							table=table+'<td class="eta">N/A</td>';
							table=table+'<td class="size">N/A</td>';
							table=table+'</tr>';
							$('#torrent-rows').append(table);

							
							if(myobject.selectedFile!=""){
								$('#torrent-rows tr').eq(myobject.selectedFile).addClass('info');
							}

						}else{

							if(status==res[i].status || status=="all"){
								var sno=i+1;
								var table='<tr class="file" id='+i+'>';
								table=table+'<td class="uuid" style="display:none">'+res[i].uuid+'</td>';
								table=table+'<td class="fid">'+sno+'</td>';
								table=table+'<td class="file_name">'+res[i].fileName+'</td>';
								table=table+'<td class="status">'+res[i].status+'</td>';
								table=table+'<td class="prog">'+ '<progress value='+'"'+res[i].progress+'"'+ 'max="100"></progress>'+'</td>';
								table=table+'<td class="eta">'+res[i].eta+'</td>';
								table=table+'<td class="size">'+res[i].size+'</td>';
								table=table+'</tr>';
								$('#torrent-rows').append(table);
								
								//console.log(myobject.selectedFile);
								if(myobject.selectedFile!=""){
									$('#torrent-rows tr').eq(myobject.selectedFile).addClass('info');
								}
							}
						}
					}
				},
				error:function(error){
					console.log(error);
				}
			});
		},

		setDefaultDirectory:function(path){
			var data=JSON.stringify({"defaultDirectory":path});
			console.log(data);
			$.ajax({
				url:"http://localhost:8080/peercds/service/defaultdirectory",
				contentType: "application/json; charset=utf-8",
				dataType: "json",
				type:"POST",
				data:data,
				success:function(res){
					console.log('success');
					if(res.message!=undefined){
						$('#common-alert-modal').modal('show');
						$('#common-alert-modal').on('shown.bs.modal',function(){
							$('#common-alert-modal').find('#message').html(res.message);
						});
					}
				},
				error:function(){
					alert("error");
				}
			});

		},

		getDefaultDirectory:function(){
			$.ajax({

				url:"http://localhost:8080/peercds/service/defaultdirectory",
				type:"GET",
				contentType: "application/json",
				success:function(res){
					$('#resource-url').val(res.defaultDirectory);
				},
				error:function(){

				}
			});
		},

		createTorrent:function(filename,trackerurl){
			console.log(filename);
			console.log(trackerurl);
			var data=JSON.stringify({"filename":filename,"trackerurl":trackerurl});

			$.ajax({
			url:"http://localhost:8080/peercds/service/createtorrent",
			type:"POST",
			data:data,
			contentType:"application/json",
			success:function(res){
				$('#common-alert-modal').modal('show');
				$('#common-alert-modal').on('shown.bs.modal',function(){
					$('#common-alert-modal').find('#message').html(res.message);
				});
			},
			error:function(){

			}
			});
		},

		
		downloadTorrent:function(filename){
			var data=JSON.stringify({"filename":filename});
			console.log('filename reached here');
			$.ajax({
				url:"http://localhost:8080/peercds/service/downloadtorrent",
				type:"POST",
				data:data,
				contentType: "application/json",
				success:function(res){
					if(res.message!=undefined){
						//pop up
						$('#common-alert-modal').modal('show');
						$('#common-alert-modal').on('shown.bs.modal',function(){
							$('#common-alert-modal').find('#message').html(res.message);
						});
					}
				},
				error:function(){

				}
			});
		},
		
		startTorrent:function(uuid){
			console.log('started Torrent');
			var data=JSON.stringify({"uuid":uuid});
			$.ajax({
				url:"http://localhost:8080/peercds/service/starttorrent",
				type:"POST",
				data:data,
				contentType: "application/json",
				success:function(res){
					console.log(res.message);
					if(res.message!=undefined){
						$('#common-alert-modal').modal('show');
						$('#common-alert-modal').on('shown.bs.modal',function(){
							$('#common-alert-modal').find('#message').html(res.message);
						});
					}
				},
				error:function(){

				}
			});
		},

		pauseTorrent:function(uuid){
			var data=JSON.stringify({"uuid":uuid});
			$.ajax({
			url:"http://localhost:8080/peercds/service/pausetorrent",
			type:"POST",
			data:data,
			contentType: "application/json",
			success:function(res){
				console.log(res.message);
				if(res.message!=undefined){
					//pop message
					$('#common-alert-modal').modal('show');
					$('#common-alert-modal').on('shown.bs.modal',function(){
						$('#common-alert-modal').find('#message').html(res.message);
					});
				}
			},
			error:function(){

			}
			});
		},

		deleteTorrent:function(uuid){
			console.log('reached delete');
			var data=JSON.stringify({"uuid":uuid});
			$.ajax({
				url:"http://localhost:8080/peercds/service/deletetorrent",
				type:"POST",
				data:data,
				contentType: "application/json",
				success:function(res){
					console.log(res);
					if(res.message!=undefined){
						//pop message
						$('#common-alert-modal').modal('show');
						$('#common-alert-modal').on('shown.bs.modal',function(){
							$('#common-alert-modal').find('#message').html(res.message);
						});
					}
				},
				error:function(){

				}
			});
		},

		
};