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
		return mapping
	}
	
	getNameBlock() {
		var html = "<a href='#' onclick='window.updateDownloader(\"" + this.infoHash + "\");return false'>" + this.name + "</a>"
		html += "<div>" + this.getCancelBlock() + "  " + this.getPauseResumeRetryBlock() + "</div>"
		return html
	}
	
	getCancelBlock() {
		if (this.state == "CANCELLED" || this.state == "FINISHED")
			return ""
		var linkText = _t("Cancel")
		var link = "<a href='#' onclick='window.cancelDownload(\"" + this.infoHash + "\");return false;'>" + linkText + "</a>"
		var block = "<span id='download-" + this.infoHash + "'>" + link + "</span>"
		return block
	}
	
	getPauseResumeRetryBlock() {
		if (this.state == "FINISHED" || this.state == "CANCELLED")
			return ""
		if (this.state == "FAILED") {
			var retryLink = new Link(_t("Retry"), "resumeDownload", [this.infoHash])
			return retryLink.render()
		} else if (this.state == "PAUSED") {
			var resumeLink = new Link(_t("Resume"), "resumeDownload", [this.infoHash])
			return resumeLink.render()
		} else {
			var pauseLink = new Link(_t("Pause"), "pauseDownload", [this.infoHash])
			return pauseLink.render()
		}
	}
}

var downloader = null;
var downloaders = new Map()

function resumeDownload(infoHash) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshDownloader()
		}
	}
	xmlhttp.open("POST", "/MuWire/Download", "true")
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=resume&infoHash=" + infoHash))
}

function pauseDownload(infoHash) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshDownloader()
		}
	}
	xmlhttp.open("POST", "/MuWire/Download", "true")
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=pause&infoHash=" + infoHash))
}

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
			var table = new Table(["Name","State","Speed","ETA","Progress"], "sortDownloads", downloadsSortKey, newOrder, null)
			
			for(i = 0; i < downloaderList.length; i++) {
				table.addRow(downloaderList[i].getMapping())
			}
			
			var downloadsDiv = document.getElementById("downloads");
			var clearDiv = document.getElementById("clearFinished")
			if (downloaders.size > 0) {
				downloadsDiv.innerHTML = table.render();
				
				var clearLink = new Link(_t("Clear Finished"), "clear", ["ignored"])
				clearDiv.innerHTML = clearLink.render()
			} else {
				downloadsDiv.innerHTML = ""
				clearDiv.innerHTML = ""
			}
			if (downloader != null)
				updateDownloader(downloader);
		}
	}
	var sortParam = "key=" + downloadsSortKey + "&order=" + downloadsSortOrder
	xmlhttp.open("GET", "/MuWire/Download?" + sortParam, true);
	xmlhttp.send();
}

function clear(ignored) {
	xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshDownloader()
		}
	}
	xmlhttp.open("POST", "/MuWire/Download", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=clear"));
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
