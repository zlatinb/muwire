class SearchBySender {
	constructor(xmlNode) {
		this.uuid = xmlNode.getElementsByTagName("uuid")[0].childNodes[0].nodeValue;
		this.query = xmlNode.getElementsByTagName("Query")[0].childNodes[0].nodeValue;
		this.resultBatches = new Map();
		
		var resultsBySender = xmlNode.getElementsByTagName("ResultsBySender")[0];
		var resultsFromSenders = resultsBySender.getElementsByTagName("ResultsFromSender");
		var i;
		for (i = 0; i < resultsFromSenders.length; i++) {
			var results = new ResultsBySender(resultsFromSenders[i]);
			this.resultBatches.set(results.sender, results);
		}
	}
}

class SearchByFile {
	constructor(xmlNode) {
		this.uuid = xmlNode.getElementsByTagName("uuid")[0].childNodes[0].nodeValue;
		this.query = xmlNode.getElementsByTagName("Query")[0].childNodes[0].nodeValue;
		this.resultBatches = new Map();
		
		var resultsByFile = xmlNode.getElementsByTagName("ResultsByFile")[0];
		var resultsForFile = resultsByFile.getElementsByTagName("ResultsForFile");
		var i;
		for (i = 0; i < resultsForFile.length; i++) {
			var results = new ResultsByFile(resultsForFile[i]);
			this.resultBatches.set(results.infoHash, results);
		}
	}
}

class ResultsBySender {
	constructor(xmlNode) {
		this.sender = xmlNode.getElementsByTagName("Sender")[0].childNodes[0].nodeValue;
		this.senderB64 = xmlNode.getElementsByTagName("SenderB64")[0].childNodes[0].nodeValue;
		this.browse = xmlNode.getElementsByTagName("Browse")[0].childNodes[0].nodeValue;
		this.results = new Map();
		var resultNodes = xmlNode.getElementsByTagName("Result");
		var i;
		for (i = 0 ; i < resultNodes.length; i ++) {
			var result = new ResultBySender(resultNodes[i]);
			this.results.set(result.infoHash,result);
		}
	}
}

class ResultsByFile {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue;
		this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue;
		this.size = xmlNode.getElementsByTagName("Size")[0].childNodes[0].nodeValue;
		this.downloading = xmlNode.getElementsByTagName("Downloading")[0].childNodes[0].nodeValue;
		this.results = new Map();
		var resultNodes = xmlNode.getElementsByTagName("Result");
		var i;
		for (i = 0; i < resultNodes.length; i++) {
			var result = new ResultByFile(resultNodes[i]);
			this.results.set(result.sender, result);
		}
	}
}

class ResultBySender {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue;
		this.size = xmlNode.getElementsByTagName("Size")[0].childNodes[0].nodeValue;
		this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue;
		this.downloading = xmlNode.getElementsByTagName("Downloading")[0].childNodes[0].nodeValue;
		this.comment = null;
		var comment = xmlNode.getElementsByTagName("Comment")
		if (comment.length == 1) 
			this.comment = comment[0].childNodes[0].nodeValue;
	}
}

class ResultByFile {
	constructor(xmlNode) {
		this.sender = xmlNode.getElementsByTagName("Sender")[0].childNodes[0].nodeValue;
		this.senderB64 = xmlNode.getElementsByTagName("SenderB64")[0].childNodes[0].nodeValue;
		this.browse = xmlNode.getElementsByTagName("Browse")[0].childNodes[0].nodeValue;
		this.comment = null;
		var comment = xmlNode.getElementsByTagName("Comment")
		if (comment.length == 1) 
			this.comment = comment[0].childNodes[0].nodeValue;
	}
}

var searches = new Map();
var expandedComments = new Map();

var uuid = null;
var sender = null;
var lastXML = null;
var infoHash = null;

function showCommentBySender(divId, spanId) {
	var split = divId.split("_");
	var commentDiv = document.getElementById(divId);
	var comment = "<pre>"+ searches.get(split[1]).resultBatches.get(split[2]).results.get(split[3]).comment + "</pre>";
	commentDiv.innerHTML = comment
	expandedComments.set(divId, comment);
	var hideLink = "<a href='#' onclick='window.hideComment(\""+divId+"\",\""+spanId+"\",\"Sender\");return false;'>Hide Comment</a>";
    var linkSpan = document.getElementById(spanId);
	linkSpan.innerHTML = hideLink;
}

function showCommentByFile(divId, spanId) {
	var split = divId.split("_");
	var commentDiv = document.getElementById(divId);
	var comment = "<pre>"+searches.get(split[1]).resultBatches.get(split[2]).results.get(split[3]).comment + "</pre>";
	commentDiv.innerHTML = comment
	expandedComments.set(divId, comment);
	var hideLink = "<a href='#' onclick='window.hideComment(\""+divId+"\",\""+spanId+"\",\"File\");return false;'>Hide Comment</a>";
    var linkSpan = document.getElementById(spanId);
	linkSpan.innerHTML = hideLink;
}

function hideComment(divId, spanId, byFile) {
	expandedComments.delete(divId);
	var commentDiv = document.getElementById(divId);
	commentDiv.innerHTML = ""
	var showLink = "<a href='#' onclick='window.showCommentBy"+byFile+"(\"" + divId + "\",\"" + spanId + "\"); return false;'>Show Comment</a>";
	var linkSpan = document.getElementById(spanId);
	linkSpan.innerHTML = showLink;
}

function download(resultInfoHash) {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var resultSpan = document.getElementById("download-"+resultInfoHash);
			resultSpan.innerHTML = "Downloading";
		}
	}
	xmlhttp.open("POST", "/MuWire/Download", true);
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=start&infoHash="+resultInfoHash+"&uuid="+uuid));
}

function updateSender(senderName) {
	sender = senderName;
	
	var resultsFromSpan = document.getElementById("resultsFrom");
	resultsFromSpan.innerHTML = "Results From "+sender;
	
	var resultsDiv = document.getElementById("bottomTable");
	var table = "<table><thead><tr><th>Name</th><th>Size</th><th>Download</th></tr></thead><tbody>"
	var x = searches.get(uuid)
	x = x.resultBatches.get(sender).results;
	for (var [resultInfoHash, result] of x) {
		table += "<tr>";
		table += "<td>";
		table += result.name;
		if (result.comment != null) {
			var divId = "comment_" + uuid + "_" + senderName + "_" + resultInfoHash;
			var spanId = "comment-link-"+resultInfoHash + senderName + uuid;
			var comment = expandedComments.get(divId);
			if (comment != null) {
				var link = "<a href='#' onclick='window.hideComment(\""+divId +"\",\"" + spanId + "\",\"Sender\");return false;'>Hide Comment</a>";
				table += "<br/><span id='"+spanId+"'>" + link + "</span><br/>";
				table += "<div id='" + divId + "'>"+comment+"</div>";				
			} else {
				var link = "<a href='#' onclick='window.showCommentBySender(\"" + divId +
					"\",\""+spanId+"\");"+
					"return false;'>Show Comment</a>"; 			
				table += "<br/><span id='"+spanId+"'>"+link+"</span>";
				table += "<div id='"+divId+"'></div>";
			}
		}
		table += "</td>";
		table += "<td>";
		table += result.size;
		table += "</td>";
		table += "<td>";
		if (result.downloading == "false") {
			table += "<span id='download-"+ resultInfoHash+"'><a href='#' onclick='window.download(\"" + resultInfoHash + "\");return false;'>Download</a></span>";
		} else {
			table += "Downloading";
		}
		table += "</td>";
		table += "</tr>";
	}
	table += "</tbody></table>";
	if (x.size > 0)
		resultsDiv.innerHTML = table;
}

function updateFile(fileInfoHash) {
	infoHash = fileInfoHash;
	
	var searchResults = searches.get(uuid).resultBatches.get(infoHash);
	
	var resultsFromSpan = document.getElementById("resultsFrom");
	resultsFromSpan.innerHTML = "Results For "+searchResults.name;
	
	var resultsDiv = document.getElementById("bottomTable");
	var table = "<table><thead><tr><th>Sender</th></tr></thead><tbody>";
	var i;
	for (var [senderName, result] of searchResults.results) {
		table += "<tr>";
		table += "<td>";
		table += senderName
		if (result.comment != null) {
			var divId = "comment_" + uuid + "_" + fileInfoHash + "_" + senderName;
			var spanId = "comment-link-" + fileInfoHash + senderName + uuid;
			var comment = expandedComments.get(divId);
			if (comment != null) {
				var link = "<a href='#' onclick='window.hideComment(\""+divId +"\",\"" + spanId + "\",\"File\");return false;'>Hide Comment</a>";
				table += "<br/><span id='"+spanId+"'>" + link + "</span><br/>";
				table += "<div id='" + divId + "'>"+comment+"</div>";
			} else {
				var link = "<a href='#' onclick='window.showCommentByFile(\"" + divId +
					"\",\""+spanId+"\");"+
					"return false;'>Show Comment</a>"; 			
				table += "<br/><span id='"+spanId+"'>"+link+"</span>";
				table += "<div id='"+divId+"'></div>";
			}
		}
		table += "</td>";
		table += "</tr>";
	}
	table += "</tbody></table>";
	if (searchResults.results.size > 0)
		resultsDiv.innerHTML = table;
}			

function updateUUIDBySender(resultUUID) {
	uuid = resultUUID;
	
	var currentSearchSpan = document.getElementById("currentSearch");
	currentSearchSpan.innerHTML = searches.get(uuid).query + " Results";
	
	var sendersDiv = document.getElementById("topTable");
	var table = "<table><thead><tr><th>Sender</th></tr></thead><tbody>";
	var x = searches.get(uuid).resultBatches;
	for (var [senderName, ignored] of x) {
		table += "<tr><td><a href='#' onclick='updateSender(\""+senderName+"\");return false;'>"
		table += senderName;
		table += "</a></td></tr>";
	}
	table += "</tbody></table>";
	if (x.size > 0)
		sendersDiv.innerHTML = table;
	if (sender != null)
		updateSender(sender);
}

function updateUUIDByFile(resultUUID) {
	uuid = resultUUID;
	
	var currentSearchSpan = document.getElementById("currentSearch");
	currentSearchSpan.innerHTML = searches.get(uuid).query + " Results";
	
	var topTableDiv = document.getElementById("topTable");
	var table = "<table><thead><tr><th>Name</th><th>Size</th><th>Download</th></tr></thead><tbody>";
	var x = searches.get(uuid).resultBatches;
	for (var [fileInfoHash, file] of x) {
		table += "<tr><td><a href='#' onclick='updateFile(\""+fileInfoHash+"\");return false;'>";
		table += file.name;
		table += "</a></td>";
		table += "<td>";
		table += file.size;
		table += "</td>";
		table += "<td>";
		if (file.downloading == "false") 
			table += "<span id='download-"+fileInfoHash+"'><a href='#' onclick='window.download(\""+fileInfoHash+"\"); return false;'>Download</a></span>";
		else
			table += "Downloading";
		table += "</td></tr>";
	}
	table += "</tbody></table>";
	if (x.size > 0) 
		topTableDiv.innerHTML = table;
	if (infoHash != null)
		updateFile(infoHash);
}

function refreshGroupBySender() {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function () {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML;
			lastXML = xmlDoc;
			searches.clear();
			var i;
			var x = xmlDoc.getElementsByTagName("Search");
			for (i = 0; i < x.length; i++) {
				var search = new SearchBySender(x[i]);
				searches.set(search.uuid, search);
			}
			
			var table = "<table><thead><tr><th>Search</th><th>Senders</th><th>Results</th></tr></thead><tbody>";
			var activeSearchesDiv = document.getElementById("activeSearches");
			for (var [resultsUUID, search] of searches) {
				table += "<tr><td><a href='#' onclick='updateUUIDBySender(\"" + resultsUUID+ "\");return false;'>"
				table += search.query;
				table += "</a></td>";
				table += "<td>"
				table += search.resultBatches.size;
				table += "</td>";
				
				var map = new Map();
				for ( var [sender, results] of search.resultBatches ) {
					for (var [fileInfoHash, resultFromSender] of results.results)
						map.set(resultFromSender.infoHash, 1);
				}
				table += "<td>";
				table += map.size;
				table += "</td>"
				table += "</tr>"
			}
			table += "</tbody></table>"
			if (x.length > 0) 
				activeSearchesDiv.innerHTML = table;
			if (uuid != null)
				updateUUIDBySender(uuid);
			
		}
	}
	xmlhttp.open("GET", "/MuWire/Search?section=groupBySender", true);
	xmlhttp.send();
}

function refreshGroupByFile() {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function () {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML;
			lastXML = xmlDoc;
			searches.clear();
			var i;
			var x = xmlDoc.getElementsByTagName("Search");
			for (i = 0; i < x.length; i++) {
				var search = new SearchByFile(x[i]);
				searches.set(search.uuid, search);
			}
			
			var table = "<table><thead><tr><th>Search</th><th>Senders</th><th>Results</th></tr></thead><tbody>";
			var activeSearchesDiv = document.getElementById("activeSearches");
			for (var [resultsUUID, search] of searches) {
				table += "<tr><td><a href='#' onclick='updateUUIDByFile(\"" + resultsUUID+ "\");return false;'>"
				table += search.query;
				table += "</a></td>";
				
				var map = new Map()
				for (var [fileInfoHash, result] of search.resultBatches) {
					for (var [senderName, resultFromSender] of result.results)
						map.set(senderName, 1)
				}
				table += "<td>"
				table += map.size;
				table += "</td>";
				
				
				table += "<td>";
				table += search.resultBatches.size;
				table += "</td>"
				table += "</tr>"
			}
			table += "</tbody></table>"
			if (x.length > 0) 
				activeSearchesDiv.innerHTML = table;
			if (uuid != null)
				updateUUIDByFile(uuid);
			
		}
	}
	xmlhttp.open("GET", "/MuWire/Search?section=groupByFile", true);
	xmlhttp.send();
}

function initGroupBySender() {
	setInterval(refreshGroupBySender, 3000);
	setTimeout(refreshGroupBySender, 1);
}

function initGroupByFile() {
	setInterval(refreshGroupByFile, 3000);
	setTimeout(refreshGroupByFile, 1);
}