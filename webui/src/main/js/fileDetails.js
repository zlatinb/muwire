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
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var downloaders = []
			var downloadNodes = this.responseXML.getElementsByTagName("Downloader")
			var i
			for (i = 0;i < downloadNodes.length; i++ ) {
				downloaders.push(new DownloadEntry(downloadNodes[i]))
			}
			
			var newOrder
			if (downloadersSortOrder == "descending")
				newOrder = "ascending"
			else if (downloadersSortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Downloader"], "sortDownloaders", downloadersSortKey, newOrder, null)
			
			for (i = 0; i < downloaders.length; i++) {
				table.addRow(downloaders[i].getMapping())
			}
			
			var downloadersDiv = document.getElementById("downloadersTable")
			if (downloaders.length > 0)
				downloadersDiv.innerHTML = table.render()
			else
				downloadersDiv.innerHTML = ""
		}
	}
	var sortParam = "&key=" + downloadersSortKey + "&order=" + downloadersSortOrder
	xmlhttp.open("GET", encodeURI("/MuWire/FileInfo?path=" + path + "&section=downloaders" + sortParam))
	xmlhttp.send()
}

function refreshCertificates() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var certificates = []
			var certNodes = this.responseXML.getElementsByTagName("Certificate")
			var i
			for (i = 0; i < certNodes.length; i++) {
				certificates.push(new CertificateEntry(certNodes[i]))
			}
			
			var newOrder
			if (certificatesSortOrder == "descending")
				newOrder = "ascending"
			else if (certificatesSortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Name","Timestamp","Issuer"], "sortCertificates", certificatesSortKey, newOrder, null)
			
			for (i = 0; i < certificates.length; i++) {
				table.addRow(certificates[i].getMapping())
			}
			
			var certsDiv = document.getElementById("certificatesTable")
			if (certificates.length > 0)
				certsDiv.innerHTML = table.render()
			else
				certsDiv.innerHTML = ""
		}
	}
	var sortParam = "&key=" + certificatesSortKey + "&order=" + certificatesSortOrder
	xmlhttp.open("GET", encodeURI("/MuWire/FileInfo?path=" + path + "&section=certificates" + sortParam))
	xmlhttp.send()
}

function sortSearchers(key, order) {
	searchersSortKey = key
	searchersSortOrder = order
	refreshSearchers()
}

function sortDownloaders(key, order) {
	downloadersSortKey = key
	downloadersSortOrder = order
	refreshDownloaders()
}

function sortCertificates(key, order) {
	certificatesSortKey = key
	certificatesSortOrder = order
	refreshCertificates()
}

var path = null

var expandedComments = new Map()

var searchersSortKey = "Searcher"
var searchersSortOrder = "descending"
var downloadersSortKey = "Downloader"
var downloadersSortOrder = "descending"
var certificatesSortKey = "Name"
var certificatesSortOrder = "descending"
