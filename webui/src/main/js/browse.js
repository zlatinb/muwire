class Result {
	constructor(name, size, comment, infoHash) {
		this.name = name
		this.size = size
		this.infoHash = infoHash
		this.comment = comment
	}
}

class Browse {
	constructor(host, hostB64, status, totalResults, receivedResults) {
		this.host = host
		this.hostB64 = hostB64
		this.totalResults = totalResults
		this.receivedResults = receivedResults
		this.status = status
	}
}

function initBrowse() {
	setTimeout(refreshActive, 1)
	setInterval(refreshActive, 3000)
}

var browsesByHost = new Map()
var resultsByInfoHash = new Map()

function refreshActive() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML
			var browses = xmlDoc.getElementsByTagName("Browse")
			var i
			for (i = 0;i < browses.length; i++) {
				var host = browses[i].getElementsByTagName("Host")[0].childNodes[0].nodeValue;
				var hostB64 = browses[i].getElementsByTagName("HostB64")[0].childNodes[0].nodeValue;
				var status = browses[i].getElementsByTagName("BrowseStatus")[0].childNodes[0].nodeValue;
				var totalResults = browses[i].getElementsByTagName("TotalResults")[0].childNodes[0].nodeValue;
				var count = browses[i].getElementsByTagName("ResultsCount")[0].childNodes[0].nodeValue;
				
				var browse = new Browse(host, hostB64, status, totalResults, count)
				browsesByHost.set(host, browse)
			}
			
			var tableHtml = "<table><thead><tr><th>Host</th><th>Status</th><th>Results</th></tr></thead></tbody>";
			for (var [host, browse] of browsesByHost) {
				var browseLink = getBrowseLink(host, host)
				
				tableHtml += "<tr>"
				tableHtml += "<td>" + browseLink + "</td>"
				tableHtml += "<td>" + browse.status + "</td>"
				
				var percent = browse.receivedResults + "/" + browse.totalResults
				tableHtml += "<td>"+percent+"</td>"
				
				tableHtml += "</tr>"
			}
			tableHtml += "</tbody></table>"
			
			var tableDiv = document.getElementById("activeBrowses")
			tableDiv.innerHTML = tableHtml
		}
	}
	xmlhttp.open("GET", "/MuWire/Browse?section=status", true)
	xmlhttp.send()
}

function getBrowseLink(host, text) {
	return "<a href='#' onclick='window.showResults(\"" + host + "\");return false;'>" + text + "</a>"
}

function showResults(host) {
	
	var refreshLink = getBrowseLink(host, "Refresh")
	var linkDiv = document.getElementById("refresh-link")
	linkDiv.innerHTML = refreshLink
	
	var browse = browsesByHost.get(host)
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML
			
			resultsByInfoHash.clear()
			
			var results = xmlDoc.getElementsByTagName("Result")
			var i
			for (i = 0; i < results.length; i++) {
				var name = results[i].getElementsByTagName("Name")[0].childNodes[0].nodeValue
				var size = results[i].getElementsByTagName("Size")[0].childNodes[0].nodeValue
				var infoHash = results[i].getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue
				var comment = results[i].getElementsByTagName("Comment")
				if (comment != null && comment.length == 1)
					comment = comment[0].childNodes[0].nodeValue
				else
					comment = null
					
				var result = new Result(name, size, comment, infoHash)
				resultsByInfoHash.set(infoHash, result)
			}
			
			var tableHtml = "<table><thead><tr><th>Name</th><th>Size</th></tr></thead><tbody>"
			
			for (var [infoHash, result] of resultsByInfoHash) {
				
				tableHtml += "<tr>"
				tableHtml += "<td>" + result.name + "</td>"
				tableHtml += "<td>" + result.size + "</td>"
				// TODO: download and show comment link
				tableHtml += "</tr>"
			}
			
			tableHtml += "</tbody></table>"
			
			var tableDiv = document.getElementById("resultsTable")
			tableDiv.innerHTML = tableHtml
		}
	}
	xmlhttp.open("GET", "/MuWire/Browse?section=results&host="+browse.hostB64, true)
	xmlhttp.send()
}