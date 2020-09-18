class Upload {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue;
		this.progress = xmlNode.getElementsByTagName("Progress")[0].childNodes[0].nodeValue;
		this.speed = xmlNode.getElementsByTagName("Speed")[0].childNodes[0].nodeValue;
		this.downloader = xmlNode.getElementsByTagName("Downloader")[0].childNodes[0].nodeValue
		this.downloaderB64 = xmlNode.getElementsByTagName("DownloaderB64")[0].childNodes[0].nodeValue
		this.remotePieces = xmlNode.getElementsByTagName("RemotePieces")[0].childNodes[0].nodeValue
		this.browse = xmlNode.getElementsByTagName("Browse")[0].childNodes[0].nodeValue
		this.browsing = xmlNode.getElementsByTagName("Browsing")[0].childNodes[0].nodeValue
		this.feed = xmlNode.getElementsByTagName("Feed")[0].childNodes[0].nodeValue
		this.subscribed = xmlNode.getElementsByTagName("Subscribed")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		mapping.set("Name", this.name)
		mapping.set("Speed", this.speed)
		mapping.set("Progress", this.progress)
		mapping.set("Downloader", this.downloader)
		mapping.set("Remote Pieces", this.remotePieces)
		mapping.set("Browse", this.getBrowseBlock())
		mapping.set("Feed", this.getFeedBlock())
		return mapping
	}
	
	getBrowseBlock() {
		if (this.browse == "false")
			return ""
		if (this.browsing == "true")
			return "<a href='/MuWire/BrowseHost?currentHost=" + this.downloaderB64 + "'>" + _t("Browsing") + "</a>"
		var link = new Link(_t("Browse"), "browse", [this.downloaderB64])
		var block = "<span id='browse-link-" + this.downloaderB64 + "'>" + link.render() + "</span>"
		return block
	}
	
	getFeedBlock() {
		if (this.feed == "false")
			return ""
		if (this.subscribed == "true")
			return "<a href='/MuWire/Feeds'>" + _t("Subscribed") + "</a>"
		var link = new Link(_t("Subscribe"), "subscribe", [this.downloaderB64])
		return "<span id='subscribe-link-" + this.downloaderB64 + "'>" + link.render() + "</span>"
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
			var table = new Table(["Name","Progress","Downloader","Browse","Feed","Remote Pieces","Speed"], "sortUploads", uploadsSortKey, newOrder, null)
			
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

function browse(host) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var linkSpan = document.getElementById("browse-link-"+host)
			linkSpan.innerHTML = "<a href='/MuWire/BrowseHost?currentHost=" + host+ "'>" + _t("Browsing") + "</a>"
		}
	}
	xmlhttp.open("POST", "/MuWire/Browse", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=browse&host="+host)
}

function subscribe(host) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var linkSpan = document.getElementById("subscribe-link-" + host)
			linkSpan.innerHTML = "<a href='/MuWire/Feeds'>" + _t("Subscribed") + "</a>"
		}
	}
	xmlhttp.open("POST", "/MuWire/Feed", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=subscribe&host=" + host)
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

document.addEventListener("DOMContentLoaded", function() {
   initUploads();
}, true);
