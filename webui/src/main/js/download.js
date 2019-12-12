class Downloader {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue;
		this.state = xmlNode.getElementsByTagName("State")[0].childNodes[0].nodeValue;
		this.speed = xmlNode.getElementsByTagName("Speed")[0].childNodes[0].nodeValue;
		this.ETA = xmlNode.getElementsByTagName("ETA")[0].childNodes[0].nodeValue;
		this.progress = xmlNode.getElementsByTagName("Progress")[0].childNodes[0].nodeValue;
		this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue;
	}
	
	getMapping() {
		var mapping = new Map()
		mapping.set("Name", this.getNameBlock())
		mapping.set("State", this.state)
		mapping.set("Speed", this.speed)
		mapping.set("ETA", this.ETA)
		mapping.set("Progress", this.progress)
		mapping.set("Cancel", this.getCancelBlock())
		return mapping
	}
	
	getNameBlock() {
		return "<a href='#' onclick='window.updateDownloader(\"" + this.infoHash + "\");return false'>" + this.name + "</a>"
	}
	
	getCancelBlock() {
		if (this.state == "CANCELLED" || this.state == "FINISHED")
			return ""
		var linkText = _t("Cancel")
		var link = "<a href='#' onclick='window.cancelDownload(\"" + this.infoHash + "\");return false;'>" + linkText + "</a>"
		var block = "<span id='download-" + this.infoHash + "'>" + link + "</span>"
		return block
	}
}

var downloader = null;
var downloaders = new Map()

function cancelDownload(infoHash) {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var downloadSpan = document.getElementById("download-"+infoHash);
			downloadSpan.innerHTML = "";
			refreshDownloader();
		}
	}
	xmlhttp.open("POST", "/MuWire/Download", true);
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=cancel&infoHash="+infoHash));
}

function updateDownloader(infoHash) {
	var selected = downloaders.get(infoHash);
	
	var downloadDetailsDiv = document.getElementById("downloadDetails");
	downloadDetailsDiv.innerHTML = "<p>" + _t("Details for {0}", selected.name) + "</p>"
}

function refreshDownloader() {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML;
			
			var downloaderList = []
			downloaders.clear();
			var i;
			var x = xmlDoc.getElementsByTagName("Download");
			for (i = 0; i < x.length; i ++) {
				var download = new Downloader(x[i]);
				downloaderList.push(download)
				downloaders.set(download.infoHash, download);
			}
			
			var newOrder
			if (downloadsSortOrder == "descending")
				newOrder = "ascending"
			else if (downloadsSortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Name","State","Speed","ETA","Progress","Cancel"], "sortDownloads", downloadsSortKey, newOrder)
			
			for(i = 0; i < downloaderList.length; i++) {
				table.addRow(downloaderList[i].getMapping())
			}
			
			var downloadsDiv = document.getElementById("downloads");
			if (downloaders.size > 0)
				downloadsDiv.innerHTML = table.render();
			if (downloader != null)
				updateDownloader(downloader);
		}
	}
	var sortParam = "key=" + downloadsSortKey + "&order=" + downloadsSortOrder
	xmlhttp.open("GET", "/MuWire/Download?" + sortParam, true);
	xmlhttp.send();
}

var downloadsSortKey
var downloadsSortOrder

function sortDownloads(key, order) {
	downloadsSortKey = key
	downloadsSortOrder = order
	refreshDownloader()
}

function initDownloads() {
	setInterval(refreshDownloader, 3000)
	setTimeout(refreshDownloader,1);
}
