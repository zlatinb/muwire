class Feed {
	constructor(xmlNode) {
		this.publisher = xmlNode.getElementsByTagName("Publisher")[0].childNodes[0].nodeValue
		this.publisherB64 = xmlNode.getElementsByTagName("PublisherB64")[0].childNodes[0].nodeValue
		this.files = xmlNode.getElementsByTagName("Files")[0].childNodes[0].nodeValue
		this.revision = parseInt(xmlNode.getElementsByTagName("Revision")[0].childNodes[0].nodeValue)
		this.status = xmlNode.getElementsByTagName("Status")[0].childNodes[0].nodeValue
		this.active = xmlNode.getElementsByTagName("Active")[0].childNodes[0].nodeValue
		this.lastUpdated = xmlNode.getElementsByTagName("LastUpdated")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		var publisherLink = new Link(this.publisher, "displayFeed", [this.publisher])
		var updateHTML = ""
		if (this.active != "true") {
			var updateLink = new Link(_t("Update"), "forceUpdate", [this.publisherB64])
			updateHTML = updateLink.render()
		}
		var unsubscribeLink = new Link(_t("Unsubscribe"), "unsubscribe", [this.publisherB64])
		var configureLink = new Link(_t("Configure"), "configure", [this.publisherB64])
		
		var publisherHTML = publisherLink.render() + "<span class='right'>" + updateHTML + "  " +
			unsubscribeLink.render() + "   " +
			configureLink.render() +
			"</span>"
		
		mapping.set("Publisher", publisherHTML)
		mapping.set("Files", this.files)
		mapping.set("Last Updated", this.lastUpdated)
		mapping.set("Status", this.status)
		
		return mapping
	}
}


class Item {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue
		this.resultStatus = xmlNode.getElementsByTagName("ResultStatus")[0].childNodes[0].nodeValue
		this.size = xmlNode.getElementsByTagName("Size")[0].childNodes[0].nodeValue
		this.timestamp = xmlNode.getElementsByTagName("Timestamp")[0].childNodes[0].nodeValue
		this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue
		this.certificates = xmlNode.getElementsByTagName("Certificates")[0].childNodes[0].nodeValue
		try {
			this.comment = xmlNode.getElementsByTagName("Comment")[0].childNodes[0].nodeValue
		} catch (ignore) {
			this.comment = null
		}
	}
	
	getCommentBlock() {
		if (this.comment == null)
			return ""
		if (expandedComments.get(this.infoHash)) {
			var hideCommentLink = new Link(_t("Hide Comment"), "hideComment", [this.infoHash])
			var html = "<div id='comment-link-" + this.infoHash + ">" + hideCommentLink.render() + "</div>"
			html += "<div id='comment-" + this.infoHash + "'>"
			html += "<pre class='comment'>" + this.comment + "</pre>"
			html += "</div>"
			return html
		} else {
			var showCommentLink = new Link(_t("Show Comment"), "showComment", [this.infoHash])
			var html = "<div id='comment-link-" + this.infoHash + "'>" + showCommentLink.render() + "</div>"
			html += "<div id='comment-" + this.infoHash + "'></div>"
			return html		
		}
	}
	
	getCertificatesBlock() {
		if (this.certificates == "0")
			return ""
			
		var linkText
		if (this.certificates == "1")
			linkText = _t("View 1 Certificate")
		else
			linkText = _t("View {0} Certificates", this.certificates)
		var b64 = feeds.get(currentFeed).publisherB64
		var link = new Link(linkText, "showCertificates", [b64, this.infoHash])
		var id = b64 + "_" + this.infoHash
		
		return "<div id='certificates-link-" + id +"'>'" + link.render() + "</div>" +
			"<div id='certificates-" + id + "'></div>"
		
	}
	
	getDownloadBlock() {
		if (this.resultStatus == "DOWNLOADING")
			return "<a href='/MuWire/Downloads'>" + _t("Downloading") + "</a>"
		if (this.resultStatus == "SHARED")
			return "<a href='/MuWire/SharedFiles'>" + _t("Downloaded") + "</a>"
		var downloadLink = new Link(_t("Download"), "download", [this.infoHash])
		return "<span id='download-" + this.infoHash + "'>" + downloadLink.render() + "</span>"
	}
	
	getMapping() {
		var mapping = new Map()
		
		var nameHtml = this.name
		nameHtml += this.getCommentBlock()
		nameHtml += this.getCertificatesBlock()
		mapping.set("Name", nameHtml)
		mapping.set("Size", this.size)
		mapping.set("Download", this.getDownloadBlock())
		mapping.set("Published", this.timestamp)
		
		return mapping
	}
}


function initFeeds() {
	setTimeout(refreshFeeds, 1)
	setInterval(refreshFeeds, 3000)
}


function refreshFeeds() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			feeds.clear()
			var listOfFeeds = []
			var feedNodes = this.responseXML.getElementsByTagName("Feed")
			var i
			for (i = 0; i < feedNodes.length; i++) {
				var feed = new Feed(feedNodes[i])
				feeds.set(feed.publisher, feed)
				listOfFeeds.push(feed)
			}
			
			var newOrder
			if (feedsSortOrder == "descending")
				newOrder = "ascending"
			else if (feedsSortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Publisher", "Files", "Last Updated", "Status"], "sortFeeds", feedsSortKey, newOrder, null)
			
			for (i = 0; i < listOfFeeds.length; i++) {
				table.addRow(listOfFeeds[i].getMapping())
			}
			
			var feedsDiv = document.getElementById("feedsTable")
			if (listOfFeeds.length > 0)
				feedsDiv.innerHTML = table.render()
			else
				feedsDiv.innerHTML = ""
				
			if (currentFeed != null) {
				var updatedFeed = feeds.get(currentFeed)
				if (updatedFeed == null) {
					currentFeed = null
					document.getElementById("itemsTable").innerHTML = ""
				} else if (updatedFeed.revision > currentFeed.revision)
					displayFeed(currentFeed)
			} else
				document.getElementById("itemsTable").innerHTML = ""
			
		}
	}
	var sortParam = "&key=" + feedsSortKey + "&order=" + feedsSortOrder
	xmlhttp.open("GET", encodeURI("/MuWire/Feed?section=feeds" + sortParam), true)
	xmlhttp.send()
}

function displayFeed(feed) {
	currentFeed = feed
	var b64 = feeds.get(currentFeed).publisherB64
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			itemsByInfoHash.clear()
			
			var items = []
			var itemNodes = this.responseXML.getElementsByTagName("Item")
			var i
			for (i = 0; i < itemNodes.length; i++) {
				var item = new Item(itemNodes[i])
				items.push(item)
				itemsByInfoHash.set(item.infoHash, item)
			}
				
			var newOrder
			if (itemsSortOrder == "descending")
				newOrder = "ascending"
			else if (itemsSortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Name", "Size", "Download", "Published"], "sortItems", itemsSortKey, newOrder, null)
			for (i = 0; i < items.length; i++) {
				table.addRow(items[i].getMapping())
			}
			
			var itemsDiv = document.getElementById("itemsTable")
			if (items.length > 0)
				itemsDiv.innerHTML = table.render()
			else
				itemsDiv.innerHTML = ""
		}
	}
	var sortParam = "&key=" + itemsSortKey + "&order=" + itemsSortOrder
	xmlhttp.open("GET", encodeURI("/MuWire/Feed?section=items&publisher=" + b64 + sortParam), true)
	xmlhttp.send()
}

function sortFeeds(key, order) {
	feedsSortKey = key
	feedsSortOrder = order
	refreshFeeds()
}

function sortItems(key, order) {
	itemsSortKey = key
	itemsSortOrder = order
	displayFeed(currentFeed)
}

function forceUpdate(b64) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshFeeds()
		}
	}
	xmlhttp.open("POST","/MuWire/Feed", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=update&host=" + b64)
}

function unsubscribe(b64) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshFeeds()
		}
	}
	xmlhttp.open("POST","/MuWire/Feed", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=unsubscribe&host=" + b64)
}

function configure(b64) {
	// TODO: implement
}

function showComment(infoHash) {
	expandedComments.set(infoHash, true)
	
	var commentText = itemsByInfoHash.get(infoHash).comment
	
	var commentDiv = document.getElementById("comment-" + infoHash);
	var comment = "<pre class='comment'>"+ commentText + "</pre>";
	commentDiv.innerHTML = comment
	
	var hideLink = new Link(_t("Hide Comment"), "hideComment", [infoHash])
	var linkSpan = document.getElementById("comment-link-" + infoHash)
	linkSpan.innerHTML = hideLink.render()
}

function hideComment(infoHash) {
	expandedComments.delete(infoHash)
	
	var commentDiv = document.getElementById("comment-" + infoHash);
	commentDiv.innerHTML = ""
	
	var showLink = new Link(_t("Show Comment"), "showComment", [infoHash])
	var linkSpan = document.getElementById("comment-link-" + infoHash);
	linkSpan.innerHTML = showLink.render();
}

function showCertificates(hostB64, infoHash) {
	var fetch = new CertificateFetch(hostB64, infoHash)
	certificateFetches.set(fetch.divId, fetch)
	
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var hideLink = new Link(_t("Hide Certificates"), "hideCertificates", [hostB64, infoHash])
			var hideLinkSpan = document.getElementById("certificates-link-" + fetch.divId)
			hideLinkSpan.innerHTML = hideLink.render()
			
			var certSpan = document.getElementById("certificates-" + fetch.divId)
			certSpan.innerHTML = _t("Fetching Certificates")
		}
	}
	xmlhttp.open("POST", "/MuWire/Certificate", true)	
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=fetch&user=" + hostB64 + "&infoHash=" + infoHash)
}

function hideCertificates(hostB64, infoHash) {
	var id = hostB64 + "_" + infoHash
	certificateFetches.delete(id)
	
	var certSpan = document.getElementById("certificates-" + id)
	certSpan.innerHTML = ""
	
	var item = itemsByInfoHash.get(infoHash)
	var showLinkText
	if (item.certificates == "1")
		showLinkText = _t("View 1 Certificate")
	else
		showLinkText = _t("View {0} Certificates", item.certificates)
	
	var showLink = new Link(showLinkText, "showCertificates", [hostB64, infoHash])
	var linkSpan = document.getElementById("certificates-link-" + id)
	linkSpan.innerHTML = showLink.render()
}

function download(infoHash) {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var resultSpan = document.getElementById("download-" + infoHash);
			resultSpan.innerHTML = "<a href='/MuWire/Downloads'>" + _t("Downloading") + "</a>"
		}
	}
	
	var hostB64 = feeds.get(currentFeed).publisherB64
	xmlhttp.open("POST", "/MuWire/Feed", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=download&host=" + hostB64 + "&infoHash=" + infoHash)
}

var feeds = new Map()
var currentFeed = null

var itemsByInfoHash = new Map()

var feedsSortKey = "Publisher"
var feedsSortOrder = "descending"
var itemsSortKey = "Name"
var itemsSortOrder = "descending"

var expandedComments = new Map()