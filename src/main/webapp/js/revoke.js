		  // Simple JavaScript Templating
		  // John Resig - http://ejohn.org/ - MIT Licensed
		  (function(){
		    var cache = {};

		    this.tmpl = function tmpl(str, data) {
		      // Figure out if we're getting a template, or if we need to
		      // load the template - and be sure to cache the result.
		      var fn = !/\W/.test(str) ?
		        cache[str] = cache[str] ||
		          tmpl(document.getElementById(str).innerHTML) :

		        // Generate a reusable function that will serve as a template
		        // generator (and which will be cached).
		        new Function("obj",
		          "var p=[],print=function(){p.push.apply(p,arguments);};" +

		          // Introduce the data as local variables using with(){}
		          "with(obj){p.push('" +

		          // Convert the template into pure JavaScript
		          str
		            .replace(/[\r\t\n]/g, " ")
		            .split("<%").join("\t")
		            .replace(/((^|%>)[^\t]*)'/g, "$1\r")
		            .replace(/\t=(.*?)%>/g, "',$1,'")
		            .split("\t").join("');")
		            .split("%>").join("p.push('")
		            .split("\r").join("\\'")
		        + "');}return p.join('');");

		      // Provide some basic currying to the user
		      return data ? fn( data ) : fn;
		    };
		  })();

		  window.URL = window.URL ? window.URL :
		               window.webkitURL ? window.webkitURL : window;

		  function Tree(selector) {
		    this.$el = $(selector);
		    this.fileList = [];
		    var html_ = [];
		    var tree_ = {};
		    var pathList_ = [];
		    var self = this;

		    this.render = function(object) {
		      if (object) {
		        for (var folder in object) {
		          if (!object[folder]) { // file's will have a null value
		            html_.push('<li><a href="#" data-type="file">', folder, '</a></li>');
		          } else {
		            html_.push('<li><a href="#">', folder, '</a>');
		            html_.push('<ul>');
		            self.render(object[folder]);
		            html_.push('</ul>');
		          }
		        }
		      }
		    };

		    this.buildFromPathList = function(paths) {
		      for (var i = 0, path; path = paths[i]; ++i) {
		        var pathParts = path.split('/');
		        var subObj = tree_;
		        for (var j = 0, folderName; folderName = pathParts[j]; ++j) {
		          if (!subObj[folderName]) {
		            subObj[folderName] = j < pathParts.length - 1 ? {} : null;
		          }
		          subObj = subObj[folderName];
		        }
		      }
		      return tree_;
		    }

		    this.init = function(e) {
		      // Reset
		      html_ = [];
		      tree_ = {};
		      pathList_ = [];
		      self.fileList = e.target.files;

		      // TODO: optimize this so we're not going through the file list twice
		      // (here and in buildFromPathList).
		      for (var i = 0, file; file = self.fileList[i]; ++i) {
		        pathList_.push(file.webkitRelativePath);
		      }

		      self.render(self.buildFromPathList(pathList_));

		      self.$el.html(html_.join('')).tree({
		        expanded: 'li:first'
		      });

		      // Add full file path to each DOM element.
		      var fileNodes = self.$el.get(0).querySelectorAll("[data-type='file']");
		      for (var i = 0, fileNode; fileNode = fileNodes[i]; ++i) {
		        fileNode.dataset['index'] = i;
		      }
		    }
		  };

		  var tree = new Tree('#dir-tree');

		  $('#file_input').change(tree.init);

		  // Initial resize to force scrollbar in when file loads
		  $('#container div:first-of-type').css('height', (document.height - 20) + 'px');
		  window.addEventListener('resize', function(e) {
		    $('#container div:first-of-type').css('height', (e.target.innerHeight - 20) + 'px');
		  });

		  function revokeFileURL(e) {
		    var thumb = document.querySelector('.thumbnail');
		    if (thumb) {
		      thumb.style.opacity = 1;
		    }
		    window.URL.revokeObjectURL(this.src);
		  };

		  tree.$el.click(function(e) {
		    if (e.target.nodeName == 'A' && e.target.dataset['type'] == 'file') {
		      var file = tree.fileList[e.target.dataset['index']];

		      var thumbnails = document.querySelector('#thumbnails');

		      if (!file.type.match(/image.*/)) {
		        thumbnails.innerHTML = '<h3>Please select an image!</h3>';
		        return;
		      }

		      thumbnails.innerHTML = '<h3>Loading...</h3>';

		      var thumb = document.querySelector('.thumbnail');
		      if (thumb) {
		        thumb.style.opacity = 0;
		      }

		      var data = {
		        'file': {
		          'name': file.name,
		          'src': window.URL.createObjectURL(file),
		          'fileSize': file.fileSize,
		          'type': file.type,
		        }
		      };

		      // Render thumbnail template with the file info (data object).
		      //thumbnails.insertAdjacentHTML('afterBegin', tmpl('thumbnail_template', data));
		      thumbnails.innerHTML = tmpl('thumbnail_template', data);
		    }
		  });