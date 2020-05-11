class Upload {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue;
		this.progress = xmlNode.getElementsByTagName("Progress")[0].childNodes[0].nodeValue;
		this.speed = xmlNode.getElementsByTagName("Speed")[0].childNodes[0].nodeValue;
		this.downloader = xmlNode.getElementsByTagName("Downloader")[0].childNodes[0].nodeValue
		this.remotePieces = xmlNode.getElementsByTagName("RemotePieces")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		mapping.set("Name", this.name)
		mapping.set("Speed", this.speed)
		mapping.set("Progress", this.progress)
		mapping.set("Downloader", this.downloader)
		mapping.set("Remote Pieces", this.remotePieces)
		return mapping
	}
}

function refreshUploads() {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML;
			
			var uploaderList = []
			var i;
			var x = xmlDoc.getElementsByTagName("Upload");
			for (i = 0; i < x.length; i ++) {
				var upload = new Upload(x[i]);
				uploaderList.push(upload)
			}
			
			var newOrder
			if (uploadsSortOrder == "descending")
				newOrder = "ascending"
			else if (uploadsSortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Name","Progress","Downloader","Remote Pieces","Speed"], "sortUploads", uploadsSortKey, newOrder, null)
			
			for(i = 0; i < uploaderList.length; i++) {
				table.addRow(uploaderList[i].getMapping())
			}
			
			var uploadsDiv = document.getElementById("uploads");
			var clearDiv = document.getElementById("clearFinished")
			if (uploaderList.length > 0) {
				uploadsDiv.innerHTML = table.render();
				var clearLink = new Link(_t("Clear Finished"), "clear", ["ignored"])
				clearDiv.innerHTML = clearLink.render()
			} else {
				uploadsDiv.textContent = ""
				clearDiv.textContent = ""
			}
		}
	}
	var sortParam = "key=" + uploadsSortKey + "&order=" + uploadsSortOrder
	xmlhttp.open("GET", "/MuWire/Upload?" + sortParam, true);
	xmlhttp.send();
}

function clear(ignored) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshUploads()
		}
	}
	xmlhttp.open("POST", "/MuWire/Upload")
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=clear"));
}

var uploadsSortKey = "Name"
var uploadsSortOrder = "descending"

function sortUploads(key, order) {
	uploadsSortKey = key
	uploadsSortOrder = order
	refreshUploads()
}

function initUploads() {
	setInterval(refreshUploads, 3000)
	setTimeout(refreshUploads,1);
}
