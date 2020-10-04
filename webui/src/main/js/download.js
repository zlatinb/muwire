class Downloader {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue;
		this.state = xmlNode.getElementsByTagName("State")[0].childNodes[0].nodeValue;
		this.stateString = xmlNode.getElementsByTagName("StateString")[0].childNodes[0].nodeValue;
		this.speed = xmlNode.getElementsByTagName("Speed")[0].childNodes[0].nodeValue;
		this.ETA = xmlNode.getElementsByTagName("ETA")[0].childNodes[0].nodeValue;
		this.progress = xmlNode.getElementsByTagName("Progress")[0].childNodes[0].nodeValue;
		this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue;
	}
	
	getMapping() {
		var mapping = new Map()
		var speed = this.speed
		var ETA = this.ETA
		var finished = (this.state == "FINISHED")
		if (finished) {
			speed = ""
			ETA = ""
		}
		mapping.set("Name", this.getNameBlock())
		mapping.set("State", this.stateString)
		
		mapping.set("Speed", speed)
		mapping.set("ETA", ETA)
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
		if (this.state == "FINISHED" || this.state == "CANCELLED" || this.state == "HOPELESS")
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
			downloadSpan.textContent = "";
			refreshDownloader();
		}
	}
	xmlhttp.open("POST", "/MuWire/Download", true);
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=cancel&infoHash="+infoHash));
}

function updateDownloader(infoHash) {
	downloader = infoHash
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var path = this.responseXML.getElementsByTagName("Path")[0].childNodes[0].nodeValue
			var pieceSize = this.responseXML.getElementsByTagName("PieceSize")[0].childNodes[0].nodeValue
			var sequential = this.responseXML.getElementsByTagName("Sequential")[0].childNodes[0].nodeValue
			var knownSources = this.responseXML.getElementsByTagName("KnownSources")[0].childNodes[0].nodeValue
			var activeSources = this.responseXML.getElementsByTagName("ActiveSources")[0].childNodes[0].nodeValue
			var hopelessSources = this.responseXML.getElementsByTagName("HopelessSources")[0].childNodes[0].nodeValue
			var totalPieces = this.responseXML.getElementsByTagName("TotalPieces")[0].childNodes[0].nodeValue
			var donePieces = this.responseXML.getElementsByTagName("DonePieces")[0].childNodes[0].nodeValue
			
			var html = "<table>"
			html += "<tr>"
			html += "<td>" + _t("Download Location") + "</td>"
			html += "<td>" + "<p align='right'>" + path + "</p>" + "</td>"
			html += "</tr>"
			html += "<tr>"
			html += "<td>" + _t("Sequential") + "</td>"
			html += "<td>" + "<p align='right'>" + sequential + "</p>" + "</td>"
			html += "</tr>"
			html += "<tr>"
			html += "<td>" + _t("Known Sources") + "</td>"
			html += "<td>" + "<p align='right'>" + knownSources + "</p>" + "</td>"
			html += "</tr>"
			html += "<tr>"
			html += "<td>" + _t("Active Sources") + "</td>"
			html += "<td>" + "<p align='right'>" + activeSources + "</p>" + "</td>"
			html += "</tr>"
			html += "<tr>"
			html += "<td>" + _t("Hopeless Sources") + "</td>"
			html += "<td>" + "<p align='right'>" + hopelessSources + "</p>" + "</td>"
			html += "</tr>"
			html += "<tr>"
			html += "<td>" + _t("Piece Size") + "</td>"
			html += "<td>" + "<p align='right'>" + pieceSize + "</p>" + "</td>"
			html += "</tr>"
			html += "<tr>"
			html += "<td>" + _t("Total Pieces") + "</td>"
			html += "<td>" + "<p align='right'>" + totalPieces + "</p>" + "</td>"
			html += "</tr>"
			html += "<tr>"
			html += "<td>" + _t("Downloaded Pieces") + "</td>"
			html += "<td>" + "<p align='right'>" + donePieces + "</p>" + "</td>"
			html += "</tr>"
			html += "</table>"
			var downloadDetailsDiv = document.getElementById("downloadDetails");
			downloadDetailsDiv.innerHTML = html
		}
	}
	xmlhttp.open("GET", "/MuWire/Download?section=details&infoHash=" + infoHash)
	xmlhttp.send()
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
				downloadsDiv.textContent = ""
				clearDiv.textContent = ""
				downloader = null
				var downloadDetailsDiv = document.getElementById("downloadDetails");
				downloadDetailsDiv.textContent = ""
			}
			if (downloader != null)
				updateDownloader(downloader);
		}
	}
	var sortParam = "&key=" + downloadsSortKey + "&order=" + downloadsSortOrder
	xmlhttp.open("GET", "/MuWire/Download?section=list" + sortParam, true);
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

var downloadsSortKey = "Name"
var downloadsSortOrder = "descending"

function sortDownloads(key, order) {
	downloadsSortKey = key
	downloadsSortOrder = order
	refreshDownloader()
}

function initDownloads() {
	setInterval(refreshDownloader, 3000)
	setTimeout(refreshDownloader,1);
}

document.addEventListener("DOMContentLoaded", function() {
   initDownloads();
}, true);
