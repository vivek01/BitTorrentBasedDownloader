	function timer(){
		var status=myobject.status;
		peerApi.getTorrents(status);
	}

	peerApi.getDefaultDirectory();
	
	/*torrent object*/
	
	var myobject={
		status:"all",
		torrentsList:'',
		currentFile:"",
		currentFileId:"",
		selectedFile:"",
		currentTorrentState:''
	};
	
	
	if(myobject.currentFile==""){}
	$('#torrent-info').hide();
	$('#page-wrapper').click(function(e){
		$('#torrent-info').hide();
		$('#torrent-rows > tr').removeClass('info');
		$('.dropdown-menu').hide();
		myobject.selectedFile="";
		myobject.currentFile="";
		if(myobject.currentFile==""){
			$('#Resume > .panel-back').css('background-color','#D1D0CE');
	        $('#Resume').css('pointer-events','none');
	        
			$('#Pause > .panel-back').css('background-color','#D1D0CE');
	        $('#Pause').css('pointer-events','none');
	        
	        $('#Delete > .panel-back').css('background-color','#D1D0CE');
	        $('#Delete').css('pointer-events','none');
		}
		
		
		myobject.currentTorrentState="";
		e.stopPropagation();

	});
	
	$('body').click(function(e){
		$('#torrent-info').hide();
		$('#torrent-rows > tr').removeClass('info');
		myobject.selectedFile="";
		myobject.currentFile="";
		myobject.currentTorrentState="";
		e.stopPropagation();
	});
	
	$('document').click(function (e) {
		$('.dropdown-menu').hide();
		myobject.currentFile="";
		myobject.currentTorrentState="";
	});

$(function(){
	timer();
	setInterval(timer, 3000);
	
	//On click of a row
	
	$("#torrent_table").on('click','#torrent-rows > tr',function(e){

		var current_row=$(this).attr('id');

		var row_object=myobject.torrentsList[current_row];
		row_object['id']=current_row;
		var active_row=row_object.id;
		myobject.currentFile=row_object;
		console.log(row_object);
		
		$('#torrent-rows > tr').removeClass('info');
		if(active_row!==""){
			$('#'+active_row).removeClass('info');
		}
		$('#'+active_row).addClass('info');

		
		
		$('#torrent-info').show();
		myobject.selectedFile=$(this).attr('id');
		

		var seeds=row_object.seeds;
		var peers=row_object.peers;
		var uploadSpeed=row_object.uploadSpeed;
		var downloadSpeed=row_object.downloadSpeed;
		var elapsedTime=row_object.elapsedTime;
		var remainingTime=row_object.eta;
		
		var status;
		if(row_object.paused==true){
			status="Paused";
			if(status=="Paused"){
				$('#Resume > .panel-back').css('background-color','');
		        $('#Resume').css('pointer-events','');
		        
				$('#Pause > .panel-back').css('background-color','#D1D0CE');
		        $('#Pause').css('pointer-events','none');
		        
		        $('#Delete > .panel-back').css('background-color','');
		        $('#Delete').css('pointer-events','');
			}
			$("#home").find("#time_elapsed").html('');
			$("#home").find("#remaining_time").html('');
		}
		
		if(row_object.error==true){
			status="Error";
		}

		if(row_object.status=="Downloading"){
			status="Downloading";
			$('#Resume > .panel-back').css('background-color','#D1D0CE');
	        $('#Resume').css('pointer-events','none');
	        
			$('#Pause > .panel-back').css('background-color','');
	        $('#Pause').css('pointer-events','');
	        
	        $('#Delete > .panel-back').css('background-color','');
	        $('#Delete').css('pointer-events','');
		}
		
		if(row_object.status=="Seeding"){
			status="Seeding";
			$('#Resume > .panel-back').css('background-color','#D1D0CE');
	        $('#Resume').css('pointer-events','none');
	        
			$('#Pause > .panel-back').css('background-color','');
	        $('#Pause').css('pointer-events','');
	        
	        $('#Delete > .panel-back').css('background-color','');
	        $('#Delete').css('pointer-events','');
		}
		

		$("#home").find("#status").html(status);
		$("#home").find("#peers_count").html(peers);
		$("#home").find("#seeds_count").html(seeds);
		$("#home").find("#upload_speed").html(uploadSpeed);
		$("#home").find("#download_speed").html(downloadSpeed);
		if(row_object.paused!==true){
			$("#home").find("#time_elapsed").html('<br>'+elapsedTime);
			$("#home").find("#remaining_time").html('<br>'+remainingTime);
		}else{
			$("#home").find("#time_elapsed").html('');
			$("#home").find("#remaining_time").html('');
		}
		
		
		
		e.stopPropagation();
	});
			
	$('#default-path').click(function(e){
		$('#default-path-modal').modal('show');
		
		$("#resource-submit").click(function(e){
			var path=$("#resource-url").val();
			if(path!==""){
				peerApi.setDefaultDirectory(path);
			}
		});
		e.stopPropagation();
	});
    
    $('li').click(function(){
  		$('#main-menu li').each(function(){
    		if($(this).children().attr('class')=="active-menu"){
    			$(this).children().removeClass("active-menu");
    		}
    	});
    	
    	$(this).children().addClass("active-menu");
    	// console.log($(this).children().addClass('active-menu'));
    });
    
    $('#downloading_files').click(function(){
    	myobject.status='Downloading';
    	setInterval(timer, 3000);
    	timer();
    });
    
    $('#seeding_files').click(function(){
    	myobject.status='Seeding';
    	setInterval(timer, 3000);
    	timer();
    });
    
    $('#all_files').click(function(){
    	myobject.status='all';
    	setInterval(timer, 3000);
    	timer();
    });

	
		$('#create_torrent').change(function(e){
			$in=$(this);
			console.log($in.val());
			$('#tracker_url_modal').modal('show');
			var filename=$in.val().replace(/C:\\fakepath\\/i, '');
			
			if(filename.length>0){
	    		$('#url-submit').click(function(){
	        		var trackerurl=$('#tracker_url').val();
	    	       	 if(trackerurl!==""){
	    	       		 peerApi.createTorrent(filename,trackerurl);
	    	       	 }
	        	});
	    	}
			$in.val("");
			
			e.stopPropagation();
		});
    
	
	
		$('#file_input').change(function(e){
			$('#upload_torrent').modal('hide'); 
			$('#tracker_url_modal').modal('show');
			var folder_name=$('#dir-tree').find('li:first-child > a').html();
			$('#url-submit').click(function(){
	    		var trackerurl=$('#tracker_url').val();
		       	 if(trackerurl!==""){
		       		 peerApi.createTorrent(folder_name,trackerurl);
		       	 }
	    	});
			$('#dir-tree').empty();
			e.stopPropagation();
		});
	

    $('input[type=file]#download_torrent').change(function(e){
    	$in=$(this);    	
    	var filename=$in.val().replace(/C:\\fakepath\\/i, '');
    	
    	peerApi.downloadTorrent(filename);
    	$in.val("");
    	e.stopPropagation();    	
    });
    
    $('#Resume').click(function(e){
    	console.log(myobject.currentFile);

    	if(myobject.currentFile!==""){
    		var uuid=myobject.currentFile.uuid;
    		peerApi.startTorrent(uuid);
    	}
    	e.stopPropagation();
    });
    
    
    $('#Pause').click(function(){
    	if(myobject.currentFile!==""){
    		var uuid=myobject.currentFile.uuid;
    		peerApi.pauseTorrent(uuid);
    	}
    });
    
    $('#Delete').click(function(e){
    	if(myobject.currentFile!==""){
    		if(confirm("Please confirm to delete the file")==true){
    			var uuid=myobject.currentFile.uuid;
        		peerApi.deleteTorrent(uuid);
    		}
    	}
    	e.stopPropagation();
    });
        
});
