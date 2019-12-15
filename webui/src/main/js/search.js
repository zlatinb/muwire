class SearchStatus {
	constructor(xmlNode) {
		this.revision = xmlNode.getElementsByTagName("Revision")[0].childNodes[0].nodeValue
		this.query = xmlNode.getElementsByTagName("Query")[0].childNodes[0].nodeValue
		this.uuid = xmlNode.getElementsByTagName("uuid")[0].childNodes[0].nodeValue
		this.senders = xmlNode.getElementsByTagName("Senders")[0].childNodes[0].nodeValue
		this.results = xmlNode.getElementsByTagName("Results")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		
		var trimmedQuery = this.query 
		if (this.query.length > 16) 
			trimmedQuery = this.query.slice(0, 16) + "..."
			
		var queryLink = new Link(trimmedQuery, "refresh" + refreshType, [this.uuid])
		var stopLink = new Link("X", "stopSearch", [this.uuid])
		var queryHtml = "<table><tr><td>" + queryLink.render() + " <b>("+this.results+")</b>" + "</td><td><p align='right'>[" + stopLink.render() + "]</p></td></tr></table>"
		mapping.set("Query", queryHtml)
		
		return mapping
	}
}


class Sender {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue
		this.b64 = xmlNode.getElementsByTagName("B64")[0].childNodes[0].nodeValue
		this.trust = xmlNode.getElementsByTagName("Trust")[0].childNodes[0].nodeValue
		this.browse = xmlNode.getElementsByTagName("Browse")[0].childNodes[0].nodeValue
		this.browsing = xmlNode.getElementsByTagName("Browsing")[0].childNodes[0].nodeValue
		this.results = xmlNode.getElementsByTagName("Results")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		mapping.set("Sender", this.getSenderLink())
		mapping.set("Results", this.results)
		
		var trustHtml = this.trust + this.getTrustLinks()
		mapping.set("Trust", trustHtml)
		
		mapping.set("Browse", this.getBrowseBlock())
		
		return mapping
	}
	
	getSenderLink() {
		return "<a href='#' onclick='window.refreshResultsFromSender(\"" + this.b64 + "\");return false'>" + this.name + "</a>"
	}
	
	getTrustLinks() {
		if (this.trust == "NEUTRAL")
			return this.getTrustLink() + this.getDistrustLink()
		else if (this.trust == "TRUSTED")
			return this.getNeutralLink() + this.getDistrustLink()
		else
			return this.getTrustLink() + this.getNeutralLink()
	}
	
	getTrustLink() {
		return "<span id='trusted-link-" + this.b64 + "'>" +
				"<a href='#' onclick='window.markTrusted(\"" + 
				this.b64 + "\"); return false;'>" + _t("Mark Trusted") + "</a></span><span id='trusted-" + 
				this.b64 + "'></span>"
	}
	
	getNeutralLink() {
		return "<a href'#' onclick='window.markNeutral(\"" + this.b64 + "\"); return false;'>" + _t("Mark Neutral") + "</a>"
	}
	
	getDistrustLink() {
		return "<span id='distrusted-link-" + this.b64 + "'>" +
				"<a href='#' onclick='window.markDistrusted(\"" + 
				this.b64 + "\"); return false;'>" + _t("Mark Distrusted") + "</a></span><span id='distrusted-" + 
				this.b64 + "'></span>"
	}
	
	getBrowseBlock() {
		if (this.browse == "false")
			return ""
		if (this.browsing == "true")
			return _t("Browsing")
		var link = "<a href='#' onclick='window.browse(\"" + this.b64 + "\"); return false;'>" + _t("Browse") + "</a>"
		var block = "<span id='browse-link-" + this.b64 + "'>" + link + "</span>"
		return block
	}
}

class Senders {
	constructor(xmlNode) {
		this.senders = []
		this.sendersMap = new Map()
		var senderNodes = xmlNode.getElementsByTagName("Sender")
		var i
		for (i = 0; i < senderNodes.length; i++) {
			var sender = new Sender(senderNodes[i])
			this.senders.push(sender)
			this.sendersMap.set(sender.b64, sender)
		}
	}
	
	render() {
		var newOrder
		if (sendersSortOrder == "descending")
			newOrder = "ascending"
		else if (sendersSortOrder == "ascending")
			newOrder = "descending"
		var table = new Table(["Sender", "Browse", "Results", "Trust"], "sortSendersTable", sendersSortKey, newOrder, null)
		var i
		for (i = 0; i < this.senders.length; i++) {
			table.addRow(this.senders[i].getMapping())
		}
		return table.render()
	}
}

class ResultFromSender {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue
		this.size = xmlNode.getElementsByTagName("Size")[0].childNodes[0].nodeValue
		this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue
		this.downloading = xmlNode.getElementsByTagName("Downloading")[0].childNodes[0].nodeValue
		this.comment = null
		try {
			this.comment = xmlNode.getElementsByTagName("Comment")[0].childNodes[0].nodeValue
		} catch (ignored) {}
		this.certificates = xmlNode.getElementsByTagName("Certificates")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		
		var nameHtml = this.name
		nameHtml += this.getCommentBlock()
		nameHtml += this.getCertificatesBlock()
		mapping.set("Name", nameHtml)
		mapping.set("Size", this.size)
		mapping.set("Download", this.getDownloadBlock())
		
		return mapping
	}
	
	getCommentBlock() {
		if (this.comment == null)
			return ""
		if (expandedComments.get(this.infoHash)) {
			var html = "<div id='comment-link-" + this.infoHash + "'>"
			html += "<a href='#' onclick='window.hideComment(\"" + this.infoHash + "\");return false;'>"
			html += _t("Hide Comment")
			html += "</a>"
			html += "</div>"
			html += "<div id='comment-" + this.infoHash + "'>"
			html += "<pre>" + this.comment + "</pre>"
			html += "</div>"
			return html
		} else {
			var html = "<div id='comment-link-" + this.infoHash + "'>"
			html += "<a href='#' onclick='window.showCommentBySender(\"" + this.infoHash + "\");return false;'>"
			html += _t("Show Comment")
			html += "</a>"
			html += "</div>"
			html += "<div id='comment-" + this.infoHash + "'></div>"
			return html
		}
	}
	
	getCertificatesBlock() {
		if (this.certificates == "0")
			return ""
		var id = currentSender + "_" + this.infoHash
		var linkText = _t("View {0} Certificates", this.certificates)
		var link = "<a href='#' onclick='viewCertificatesBySender(\"" + this.infoHash + "\",\"" + this.certificates + "\");return false'>" + linkText + "</a>"
		var linkBlock = "<div id='certificates-link-" + id + "'>" + link + "</div>"
		var certBlock = "<div id='certificates-" + id + "'></div>"
		return linkBlock + certBlock
	}
	
	getDownloadBlock() {
		if (this.downloading == "true")
			return _t("Downloading")
		var link = "<a href='#' onclick='window.download(\"" + this.infoHash + "\");return false;'>" + _t("Download") + "</a>"
		var block = "<span id='download-" + this.infoHash + "'>" + link + "</span>"
		return block
	}
}

class ResultsFromSender {
	constructor(xmlNode) {
		this.resultsFromSender = []
		this.resultsMap = new Map()
		var resultNodes = xmlNode.getElementsByTagName("Result")
		var i
		for (i = 0; i < resultNodes.length; i++) {
			var result = new ResultFromSender(resultNodes[i])
			this.resultsFromSender.push(result)
			this.resultsMap.set(result.infoHash, result)
		}
	}
	
	render() {
		var newOrder
		if (resultsFromSenderSortOrder == "descending")
			newOrder = "ascending"
		else if (resultsFromSenderSortOrder == "ascending")
			newOrder = "descending"
		var table = new Table(["Name","Size","Download"], "sortResultsFromSenderTable", resultsFromSenderSortKey, newOrder, null)
		var i
		for (i = 0 ; i < this.resultsFromSender.length; i++) {
			table.addRow(this.resultsFromSender[i].getMapping())
		}
		return table.render()
	}
}

class Result {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue
		this.size = xmlNode.getElementsByTagName("Size")[0].childNodes[0].nodeValue
		this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue
		this.downloading = xmlNode.getElementsByTagName("Downloading")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		mapping.set("Name", this.getNameBlock())
		mapping.set("Size", this.size)
		mapping.set("Download", this.getDownloadBlock())
		return mapping
	}
	
	getNameBlock() {
		return "<a href='#' onclick='window.refreshSendersForResult(\"" + this.infoHash + "\");return false;'>" + this.name + "</a>"
	}
	getDownloadBlock() {
		if (this.downloading == "true")
			return _t("Downloading")
		var link = "<a href='#' onclick='window.download(\"" + this.infoHash + "\");return false;'>" + _t("Download") + "</a>"
		var block = "<span id='download-" + this.infoHash + "'>" + link + "</span>"
		return block
	}
}

class Results {
	constructor(xmlNode) {
		this.results = []
		this.resultsMap = new Map()
		var resultNodes = xmlNode.getElementsByTagName("Result")
		var i
		for (i = 0; i < resultNodes.length; i++) {
			var result = new Result(resultNodes[i])
			this.results.push(result)
			this.resultsMap.set(result.infoHash, result)
		}
	}
	
	render() {
		var newOrder
		if(resultsSortOrder == "descending")
			newOrder = "ascending"
		else if (resultsSortOrder == "ascending")
			newOrder = "descending"
		var table = new Table(["Name","Size","Download"], "sortResultsTable", resultsSortKey, newOrder, null)
		var i
		for (i = 0; i < this.results.length; i++) {
			table.addRow(this.results[i].getMapping())
		}
		return table.render()
	}
}

class SenderForResult {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue
		this.b64 = xmlNode.getElementsByTagName("B64")[0].childNodes[0].nodeValue
		this.browse = xmlNode.getElementsByTagName("Browse")[0].childNodes[0].nodeValue
		this.browsing = xmlNode.getElementsByTagName("Browsing")[0].childNodes[0].nodeValue
		this.trust = xmlNode.getElementsByTagName("Trust")[0].childNodes[0].nodeValue
		this.comment = null
		try {
			this.comment = xmlNode.getElementsByTagName("Comment")[0].childNodes[0].nodeValue
		} catch (ignored) {}
		this.certificates = xmlNode.getElementsByTagName("Certificates")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		mapping.set("Sender", this.getNameBlock())
		mapping.set("Browse", this.getBrowseBlock())
		mapping.set("Trust", this.getTrustBlock())
		return mapping
	}
	
	getNameBlock() {
		return this.name + this.getCommentBlock() + this.getCertificatesBlock()
	}
	
	getCommentBlock() {
		if (this.comment == null)
			return ""
		if (expandedComments.get(this.b64)) {
			var html = "<div id='comment-link-" + this.b64 + "'>"
			html += "<a href='#' onclick='window.hideComment(\"" + this.b64 + "\");return false;'>"
			html += _t("Hide Comment")
			html += "</a>"
			html += "</div>"
			html += "<div id='comment-" + this.b64 + "'>"
			html += "<pre>" + this.comment + "</pre>"
			html += "</div>"
			return html
		} else {
			var link = "<a href='#' onclick='showCommentByFile(\"" + this.b64 + "\");return false;'>" + _t("Show Comment") + "</a>"
			var block = "<div id='comment-link-" + this.b64 + "'>" + link + "</div>"
			block += "<div id='comment-" + this.b64 + "'></div>"
			return block
		}
	}
	
	getCertificatesBlock() {
		if (this.certificates == "0")
			return ""
		var id = this.b64 + "_" + currentResult
		var linkText =  _t("View {0} Certificates", this.certificates)
		var link = "<a href='#' onclick='window.viewCertificatesByFile(\"" + this.b64 + "\",\"" + this.certificates + "\");return false;')>" + linkText + "</a>"
		var linkBlock = "<div id='certificates-link-" + id + "'>" + link + "</div>"
		var certBlock = "<div id='certificates-" + id + "'></div>"
		return linkBlock + certBlock 
	}
	
	getBrowseBlock() {
		if (this.browse != "true")
			return ""
		if (this.browsing == "true")
			return _t("Browsing")
		var link = "<a href='#' onclick='window.browse(\"" + this.b64 + "\");return false;'>" + _t("Browse") + "</a>"
		var block = "<span id='browse-link-" + this.b64 + "'>" + link + "</span>"
		return block
	}
	
	getTrustBlock() {
		return this.trust + this.getTrustLinks()
	}
	
	getTrustLinks() {
		if (this.trust == "NEUTRAL")
			return this.getTrustLink() + this.getDistrustLink()
		else if (this.trust == "TRUSTED")
			return this.getNeutralLink() + this.getDistrustLink()
		else
			return this.getTrustLink() + this.getNeutralLink()
	}
	
	getTrustLink() {
		return "<span id='trusted-link-" + this.b64 + "'>" +
				"<a href='#' onclick='window.markTrusted(\"" + 
				this.b64 + "\"); return false;'>" + _t("Mark Trusted") + "</a></span><span id='trusted-" + 
				this.b64 + "'></span>"
	}
	
	getNeutralLink() {
		return "<a href'#' onclick='window.markNeutral(\"" + this.b64 + "\"); return false;'>" + _t("Mark Neutral") + "</a>"
	}
	
	getDistrustLink() {
		return "<span id='distrusted-link-" + this.b64 + "'>" +
				"<a href='#' onclick='window.markDistrusted(\"" + 
				this.b64 + "\"); return false;'>" + _t("Mark Distrusted") + "</a></span><span id='distrusted-" + 
				this.b64 + "'></span>"
	}
}

class SendersForResult {
	constructor(xmlNode) {
		this.sendersForResult = []
		this.sendersMap = new Map()
		var senderNodes = xmlNode.getElementsByTagName("Sender")
		var i
		for (i = 0; i < senderNodes.length; i++) {
			var sender = new SenderForResult(senderNodes[i])
			this.sendersForResult.push(sender)
			this.sendersMap.set(sender.b64, sender)
		}
	}
	
	render() {
		var newOrder
		if (sendersForResultSortOrder == "descending")
			newOrder = "ascending"
		else if (sendersForResultSortOrder == "ascending")
			newOrder = "descending"
		var table = new Table(["Sender", "Browse", "Trust"], "sortSendersForResultTable", sendersForResultSortKey, newOrder, null)
		var i
		for (i = 0; i < this.sendersForResult.length; i++) {
			table.addRow(this.sendersForResult[i].getMapping())
		}
		return table.render()
	}
}


// sort fields
var sendersSortKey
var sendersSortOrder
var resultsFromSenderSortKey
var resultsFromSenderSortOrder

var resultsSortKey
var resultsSortOrder
var sendersForResultSortKey
var sendersForResultSortOrder

var statusKey = null
var statusOrder = null

// global fields
var senders
var currentSender
var resultsFromSender
var results
var currentResult
var sendersForResult

// expanded comments
var expandedComments = new Map()

// status fields
var uuid = null;
var statusByUUID = new Map()

// pointers based on current view type
var refreshFunction = null
var refreshType = null


function sortSendersTable(key, order) {
	sendersSortKey = key
	sendersSortOrder = order
	refreshSender(uuid)
}

function sortResultsTable(key, order) {
	resultsSortKey = key
	resultsSortOrder = order
	refreshFile(uuid)
}

function sortResultsFromSenderTable(key, order) {
	resultsFromSenderSortKey = key
	resultsFromSenderSortOrder = order
	refreshResultsFromSender(currentSender)
}

function sortSendersForResultTable(key, order) {
	sendersForResultSortKey = key
	sendersForResultSortOrder = order
	refreshSendersForResult(currentResult)
}

function showCommentBySender(infoHash) {
	
	expandedComments.set(infoHash, true)
	
	var commentText = resultsFromSender.resultsMap.get(infoHash).comment
	
	var commentDiv = document.getElementById("comment-" + infoHash);
	var comment = "<pre>"+ commentText + "</pre>";
	commentDiv.innerHTML = comment
	
	
	var hideLink = "<a href='#' onclick='window.hideComment(\"" + infoHash + "\");return false;'>" + _t("Hide Comment") + "</a>";
    var linkSpan = document.getElementById("comment-link-" + infoHash);
	linkSpan.innerHTML = hideLink;
}

function showCommentByFile(b64) {
	
	expandedComments.set(b64, true)
	
	var commentText = sendersForResult.sendersMap.get(b64).comment
	
	var commentDiv = document.getElementById("comment-" + b64)
	var comment = "<pre>" + commentText + "</pre>"
	commentDiv.innerHTML = comment
	
	var hideLink = "<a href='#' onclick='window.hideComment(\"" + b64 + "\");return false;'>" + _t("Hide Comment") + "</a>";
	var linkSpan = document.getElementById("comment-link-" + b64);
	linkSpan.innerHTML = hideLink;
}

function hideComment(id) {
	expandedComments.delete(id)
	
	var commentDiv = document.getElementById("comment-" + id);
	commentDiv.innerHTML = ""
	
	var showLink = "<a href='#' onclick='window.showCommentBy" + refreshType + "(\"" + id + "\"); return false;'>" + _t("Show Comment") + "</a>";
	var linkSpan = document.getElementById("comment-link-"+id);
	linkSpan.innerHTML = showLink;
}

function download(resultInfoHash) {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var resultSpan = document.getElementById("download-"+resultInfoHash);
			resultSpan.innerHTML = _t("Downloading");
		}
	}
	xmlhttp.open("POST", "/MuWire/Download", true);
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=start&infoHash="+resultInfoHash+"&uuid="+uuid));
}

function markTrusted(host) {
	var linkSpan = document.getElementById("trusted-link-"+host)
	linkSpan.innerHTML = ""
	
	var textAreaSpan = document.getElementById("trusted-"+host)
	
	var textbox = "<textarea id='trust-reason-" + host + "'></textarea>"
	var submitLink = "<a href='#' onclick='window.submitTrust(\"" + host + "\");return false;'>" + _t("Submit") + "</a>"
	var cancelLink = "<a href='#' onclick='window.cancelTrust(\"" + host + "\");return false;'>" + _t("Cancel") + "</a>"
	
	var html = "<br/>Enter Reason (Optional)<br/>" + textbox + "<br/>" + submitLink + " " + cancelLink + "<br/>"
	
	textAreaSpan.innerHTML = html
}

function markNeutral(host) {
	publishTrust(host, "", "neutral")
}

function markDistrusted(host) {
	var linkSpan = document.getElementById("distrusted-link-"+host)
	linkSpan.innerHTML = ""
	
	var textAreaSpan = document.getElementById("distrusted-"+host)
	
	var textbox = "<textarea id='distrust-reason-" + host + "'></textarea>"
	var submitLink = "<a href='#' onclick='window.submitDistrust(\"" + host + "\");return false;'>" + _t("Submit") + "</a>"
	var cancelLink = "<a href='#' onclick='window.cancelDistrust(\"" + host + "\");return false;'>" + _t("Cancel") + "</a>"
	
	var html = "<br/>Enter Reason (Optional)<br/>" + textbox + "<br/>" + submitLink + " " + cancelLink + "<br/>"
	
	textAreaSpan.innerHTML = html
}

function submitTrust(host) {
	var reason = document.getElementById("trust-reason-"+host).value
	publishTrust(host, reason, "trust")
}

function submitDistrust(host) {
	var reason = document.getElementById("trust-reason-"+host).value
	publishTrust(host, reason, "distrust")
}

function cancelTrust(host) {
	var textAreaSpan = document.getElementById("trusted-" + host)
	textAreaSpan.innerHTML = ""
	
	var linkSpan = document.getElementById("trusted-link-"+host)
	var html = "<a href='#' onclick='markTrusted(\"" + host + "\"); return false;'>" + _t("Mark Trusted") + "</a>"
	linkSpan.innerHTML = html
}

function cancelDistrust(host) {
	var textAreaSpan = document.getElementById("distrusted-" + host)
	textAreaSpan.innerHTML = ""
	
	var linkSpan = document.getElementById("distrusted-link-"+host)
	var html = "<a href='#' onclick='markDistrusted(\"" + host + "\"); return false;'>" + _t("Mark Distrusted") + "</a>"
	linkSpan.innerHTML = html
}

function publishTrust(host, reason, trust) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			if (refreshType == "Sender")
				refreshSender(uuid, true)
			else if (refreshType == "File")
				refreshFile(uuid, true)
		}
	}
	xmlhttp.open("POST","/MuWire/Trust", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=" + trust + "&reason=" + reason + "&persona=" + host)
}

function browse(host) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var linkSpan = document.getElementById("browse-link-"+host)
			linkSpan.innerHTML = _t("Browsing");
		}
	}
	xmlhttp.open("POST", "/MuWire/Browse", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=browse&host="+host)
}

function viewCertificatesByFile(fileSenderB64, count) {
	var fetch = new CertificateFetch(fileSenderB64, currentResult)
	certificateFetches.set(fetch.divId, fetch)
	
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var linkSpan = document.getElementById("certificates-link-" + fetch.divId)
			var hideLink = "<a href='#' onclick='window.hideCertificatesByFile(\"" + fileSenderB64 + "\",\"" + count + "\");return false;'>" + _t("Hide Certificates") + "</a>"
			linkSpan.innerHTML = hideLink
			
			var fetchSpan = document.getElementById("certificates-" + fetch.divId)
			fetchSpan.innerHTML = _t("Fetching Certificates")
		}	
	}
	xmlhttp.open("POST", "/MuWire/Certificate", true)	
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=fetch&user=" + fileSenderB64 + "&infoHash=" + currentResult)
}

function hideCertificatesByFile(fileSenderB64, count) {
	var id = fileSenderB64 + "_" + currentResult
	certificateFetches.delete(id)  // TODO: propagate cancel to core
	
	var fetchSpan = document.getElementById("certificates-" + id)
	fetchSpan.innerHTML = ""
	
	var linkSpan = document.getElementById("certificates-link-" + id)
	var linkText = _t("View {0} Certificates", count)
	var showLink = "<a href='#' onclick='window.viewCertificatesByFile(\"" + fileSenderB64 + "\",\"" + count + "\");return false;'>" + linkText + "</a>"
	linkSpan.innerHTML = showLink
}

function viewCertificatesBySender(fileInfoHash, count) {
	var fetch = new CertificateFetch(currentSender, fileInfoHash)
	certificateFetches.set(fetch.divId, fetch)
	
	
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var linkSpan = document.getElementById("certificates-link-" + fetch.divId)
			var hideLink = "<a href='#' onclick='window.hideCertificatesBySender(\"" + fileInfoHash + "\",\"" + count + "\");return false;'>" +
				_t("Hide Certificates") + "</a>"
			linkSpan.innerHTML = hideLink
			
			var fetchSpan = document.getElementById("certificates-" + fetch.divId)
			fetchSpan.innerHTML = _t("Fetching Certificates")
			
		}
	}
	xmlhttp.open("POST", "/MuWire/Certificate", true)	
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=fetch&user=" + currentSender + "&infoHash=" + fileInfoHash)
		
}

function hideCertificatesBySender(fileInfoHash, count) {
	var id = currentSender + "_" + fileInfoHash
	certificateFetches.delete(id) // TODO: propagate cancel to core
	
	var fetchSpan = document.getElementById("certificates-" + id)
	fetchSpan.innerHTML = ""
	
	var linkSpan = document.getElementById("certificates-link-" + id)
	var linkText = _t("View {0} Certificates", count)
	var showLink = "<a href='#' onclick='window.viewCertificatesBySender(\"" + fileInfoHash + "\",\"" + count + "\");return false;'>" +
		linkText + "</a>"
	linkSpan.innerHTML = showLink
}

function refreshResultsFromSender(sender) {
	currentSender = sender
	
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			resultsFromSender = new ResultsFromSender(this.responseXML)
			var tableHtml = resultsFromSender.render()
			
			var bottomTableDiv = document.getElementById("bottomTable")
			bottomTableDiv.innerHTML = tableHtml
		}
	}
	var sortParam = "&key=" + resultsFromSenderSortKey + "&order=" + resultsFromSenderSortOrder
	xmlhttp.open("GET", "/MuWire/Search?section=resultsFromSender&uuid=" + uuid + "&sender=" + sender + sortParam)
	xmlhttp.send()
}

function refreshSendersForResult(result) {
	currentResult = result
	
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			sendersForResult = new SendersForResult(this.responseXML)
			var tableHtml = sendersForResult.render()
			
			var bottomTableDiv = document.getElementById("bottomTable")
			bottomTableDiv.innerHTML = tableHtml
		}
	}
	var sortParam = "&key=" + sendersForResultSortKey + "&order=" + sendersForResultSortOrder
	xmlhttp.open("GET", "/MuWire/Search?section=sendersForResult&uuid=" + uuid + "&infoHash=" + currentResult + sortParam)
	xmlhttp.send()
}

function refreshSender(searchUUID) {
	uuid = searchUUID
	
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			senders = new Senders(this.responseXML)
			var tableHtml = senders.render()
			
			var topTableDiv = document.getElementById("topTable")
			topTableDiv.innerHTML = tableHtml
			
			if (currentSender != null)
				refreshResultsFromSender(currentSender)
		}
	}
	var sortParam = "&key=" + sendersSortKey + "&order=" + sendersSortOrder
	xmlhttp.open("GET", "/MuWire/Search?section=senders&uuid=" + uuid + sortParam, true)
	xmlhttp.send()
	
}

function refreshFile(searchUUID) {
	uuid = searchUUID
	
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			results = new Results(this.responseXML)
			var tableHtml = results.render()
			
			var topTableDiv = document.getElementById("topTable")
			topTableDiv.innerHTML = tableHtml
			
			if (currentResult != null)
				refreshSendersForResult(currentResult)
		}
	}
	var sortParam = "&key=" + resultsSortKey + "&order=" + resultsSortOrder
	xmlhttp.open("GET", "/MuWire/Search?section=results&uuid=" + uuid + sortParam, true)
	xmlhttp.send()
}

function refreshStatus() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var currentSearch = null
			if (uuid != null)
				currentSearch = statusByUUID.get(uuid)
			statusByUUID.clear()
			
			var statuses = []
			var activeSearches = this.responseXML.getElementsByTagName("Search")
			var i
			for(i = 0; i < activeSearches.length; i++) {
				var searchStatus = new SearchStatus(activeSearches[i])
				statusByUUID.set(searchStatus.uuid, searchStatus)
				statuses.push(searchStatus)
			}
			
			
			var newOrder
			if (statusOrder == "descending")
				newOrder = "ascending"
			else
				newOrder = "descending"
			var table = new Table(["Query"], "sortStatuses", statusKey, newOrder, null)
			for (i = 0; i < statuses.length; i++) {
				var status = statuses[i]
				table.addRow(status.getMapping())				
			}
			
			var activeDiv = document.getElementById("activeSearches")
			activeDiv.innerHTML = table.render()
			
			if (uuid != null) {
				var newStatus = statusByUUID.get(uuid)
				if (newStatus.revision > currentSearch.revision)
					refreshFunction(uuid)
			}
		}
	}
	var params = "section=status&key=" + statusKey + "&order=" + statusOrder
	xmlhttp.open("GET", "/MuWire/Search?" + params, true)
	xmlhttp.send()
}

function sortStatuses(key, order) {
	statusKey = key
	statusOrder = order
	refreshStatus()
}

function stopSearch(searchUUID) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshStatus()
		}
	}
	xmlhttp.open("POST", "/MuWire/Search", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=stop&uuid=" + searchUUID)
}

function initGroupBySender() {
	refreshFunction = refreshSender
	refreshType = "Sender"
	setInterval(refreshStatus, 3000);
	setTimeout(refreshStatus, 1);
}

function initGroupByFile() {
	refreshFunction = refreshFile
	refreshType = "File"
	setInterval ( refreshStatus, 3000);
	setTimeout ( refreshStatus, 1);
}
