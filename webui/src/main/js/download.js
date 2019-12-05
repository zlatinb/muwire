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
		}
	}
	xmlhttp.open("POST", "/MuWire/Download", true);
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=cancel&infoHash="+infoHash));
}

function updateDownloader(infoHash) {
	var selected = downloaders.get(infoHash);
	
	var downloadDetailsDiv = document.getElementById("downloadDetails");
	downloadDetailsDiv.innerHTML = "<p>Details for "+selected.name+"</p>"
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
			
			var table = "<table><thead><tr><th>Name</th><th>State</th><th>Speed</th><th>ETA</th><th>Progress</th><th>Cancel</th></tr></thead></tbody>";
			var downloadsDiv = document.getElementById("downloads");
			for (var [infoHash, download] of downloaders) {
				table += "<tr><td><a href='#' onclick='updateDownloader(\""+infoHash+"\");return false;'>";
				table += download.name;
				table += "</a></td>";
				table += "<td>"+download.state+"</td>";
				table += "<td>"+download.speed+"</td>";
				table += "<td>"+download.ETA+"</td>";
				table += "<td>"+download.progress+"</td>";
				
				table += "<td><span id='download-"+infoHash+"'><a href='#' onclick='window.cancelDownload(\""+infoHash+"\");return false;'>Cancel</a></span></td>";
				
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