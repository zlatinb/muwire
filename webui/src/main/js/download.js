class Downloader {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue;
		this.state = xmlNode.getElementsByTagName("State")[0].childNodes[0].nodeValue;
		this.speed = xmlNode.getElementsByTagName("Speed")[0].childNodes[0].nodeValue;
		this.ETA = xmlNode.getElementsByTagName("ETA")[0].childNodes[0].nodeValue;
		this.progress = xmlNode.getElementsByTagName("Progress")[0].childNodes[0].nodeValue;
		this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue;
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
			downloaders.clear();
			var i;
			var x = xmlDoc.getElementsByTagName("Download");
			for (i = 0; i < x.length; i ++) {
				var download = new Downloader(x[i]);
				downloaders.set(download.infoHash, download);
			}
			
			var table = "<table><thead><tr><th>" + _t("Name") + "</th><th>" + _t("State") + "</th><th>" + _t("Speed") + "</th><th>" + _t("ETA") + "</th><th>" + _t("Progress") + "</th><th>" + _t("Cancel") + "</th></tr></thead></tbody>";
			var downloadsDiv = document.getElementById("downloads");
			for (var [infoHash, download] of downloaders) {
				table += "<tr><td><a href='#' onclick='updateDownloader(\""+infoHash+"\");return false;'>";
				table += download.name;
				table += "</a></td>";
				table += "<td>"+download.state+"</td>";
				table += "<td>"+download.speed+"</td>";
				table += "<td>"+download.ETA+"</td>";
				table += "<td>"+download.progress+"</td>";
				
				if (download.state != "CANCELLED")
					table += "<td><span id='download-"+infoHash+"'><a href='#' onclick='window.cancelDownload(\""+infoHash+"\");return false;'>" + _t("Cancel") + "</a></span></td>";
				else
					table += "<td></td>";
				table += "</tr>";
			}
			table += "</tbody></table>";
			if (downloaders.size > 0)
				downloadsDiv.innerHTML = table;
			if (downloader != null)
				updateDownloader(downloader);
		}
	}
	xmlhttp.open("GET", "/MuWire/Download", true);
	xmlhttp.send();
}

function initDownloads() {
	setInterval(refreshDownloader, 3000)
	setTimeout(refreshDownloader,1);
}
