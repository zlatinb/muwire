class SearchEntry {
	constructor(xmlNode) {
		this.persona = xmlNode.getElementsByTagName("Persona")[0].childNodes[0].nodeValue
		this.timestamp = xmlNode.getElementsByTagName("Timestamp")[0].childNodes[0].nodeValue
		this.query = xmlNode.getElementsByTagName("Query")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		mapping.set("Searcher", this.persona)
		mapping.set("Timestamp", this.timestamp)
		mapping.set("Query", this.query)
		return mapping
	}
}

class DownloadEntry {
	constructor(xmlNode) {
		this.persona = xmlNode.getElementsByTagName("Persona")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		mapping.set("Downloader", this.persona)
		return mapping
	}
}

class CertificateEntry {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue
		this.timestamp = xmlNode.getElementsByTagName("Timestamp")[0].childNodes[0].nodeValue
		this.issuer = xmlNode.getElementsByTagName("Issuer")[0].childNodes[0].nodeValue
		try {
			this.comment = xmlNode.getElementsByTagName("Comment")[0].childNodes[0].nodeValue
		} catch (ignored) {
			this.comment = null
		}
	}
		
	getMapping() {
		var mapping = new Map()
		// TODO: comments
		mapping.set("Name", this.name)
		mapping.set("Timestamp", this.timestamp)
		mapping.set("Issuer", this.issuer)
		return mapping
	}
}

function initFileDetails() {
	setTimeout(refreshAll, 1)
	setInterval(refreshAll, 3000)
}

function refreshAll() {
	refreshSearchers()
	refreshDownloaders()
	refreshCertificates()
}

function refreshSearchers() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var searchers = []
			var searchNodes = this.responseXML.getElementsByTagName("SearchEntry")
			var i
			for (i = 0; i < searchNodes.length; i++) {
				searchers.push(new SearchEntry(searchNodes[i]))
			}
			
			var newOrder
			if (searchersSortOrder == "descending")
				newOrder = "ascending"
			else if (searchersSortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Searcher", "Timestamp", "Query"], "sortSearchers", searchersSortKey, newOrder, null)
			
			for (i = 0; i < searchers.length; i++) {
				table.addRow(searchers[i].getMapping())
			}
			
			var hitsDiv = document.getElementById("hitsTable")
			if (searchNodes.length > 0)
				hitsDiv.innerHTML = table.render()
			else
				hitsDiv.innerHTML = ""
		}
	}
	var sortParam = "&key=" + searchersSortKey + "&order=" + searchersSortOrder
	xmlhttp.open("GET", encodeURI("/MuWire/FileInfo?path=" + path + "&section=searchers" + sortParam))
	xmlhttp.send() 
}

function refreshDownloaders() {
	
}

function refreshCertificates() {
	
}

function sortSearchers(key, order) {
	searchersSortKey = key
	searchersSortOrder = order
	refreshSearchers()
}

var path = null

var expandedComments = new Map()

var searchersSortKey = "searcher"
var searchersSortOrder = "descending"
var downloadersSortKey = "downloader"
var downloadersSortOrder = "descending"
var certificatesSortKey = "name"
var certificatesSortOrder = "descending"
