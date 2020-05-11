var certificateFetches = new Map()
var expandedCertificateComments = new Map()
var sortParams = new Map()

class SortParams {
	constructor(key, order) {
		this.key = key
		this.order = order
	}
}

class Certificate {
	constructor(xmlNode, divId) {
		this.divId = divId
		this.issuer = xmlNode.getElementsByTagName("Issuer")[0].childNodes[0].nodeValue
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue
		this.comment = null
		try {
			this.comment = xmlNode.getElementsByTagName("Comment")[0].childNodes[0].nodeValue
		} catch(ignore) {}
		this.timestamp = xmlNode.getElementsByTagName("Timestamp")[0].childNodes[0].nodeValue
		this.base64 = xmlNode.getElementsByTagName("Base64")[0].childNodes[0].nodeValue
		this.imported = xmlNode.getElementsByTagName("Imported")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		mapping.set("Issuer", this.issuer + this.getViewCommentBlock())
		mapping.set("Name", this.name)
		mapping.set("Timestamp", this.timestamp)
		if (this.imported == "true")
			mapping.set("Import", _t("Imported"))
		else
			mapping.set("Import", this.getImportLink())
		return mapping
	}
	
	getViewCommentBlock() {
		if (this.comment == null)
			return ""
		var id = this.divId + "_" + this.base64
		
		if (expandedCertificateComments.get(id)) {
			var linkText = _t("Hide Comment")
			var link = "<a href='#' onclick='window.hideCertificateComment(\"" + this.divId + "\",\"" + this.base64 + "\");return false;'>" + linkText + "</a>"
			var html = "<div id='certificate-comment-link-" + id + "'>" + link + "</div>"
			html += "<div id='certificate-comment-" + id + "'>"
			html += "<pre class='comment'>" + this.comment + "</pre>"
			html += "</div>"
			return html
		} else {
			var linkText = _t("Show Comment")
			var link = "<a href='#' onclick='window.showCertificateComment(\"" + this.divId + "\",\"" + this.base64 + "\");return false;'>" + linkText + "</a>"
			var linkBlock = "<div id='certificate-comment-link-" + id + "'>" + link + "</div>" +
				"<div id='certificate-comment-" + id + "'></div>"
			return linkBlock
		} 
	}
	
	getImportLink() {
		var linkText = _t("Import")
		var link = "<a href='#' onclick='window.importCertificate(\"" + this.base64 + "\"); return false;'>" + linkText + "</a>"
		return link
	}
}

class CertificateResponse {
	constructor(xmlNode, divId) {
		this.status = xmlNode.getElementsByTagName("Status")[0].childNodes[0].nodeValue
		this.total = xmlNode.getElementsByTagName("Total")[0].childNodes[0].nodeValue
		this.divId = divId
		
		var certNodes = xmlNode.getElementsByTagName("Certificates")[0].getElementsByTagName("Certificate")
		var i
		this.certificates = []
		this.certificatesBy64 = new Map()
		for (i = 0; i < certNodes.length; i++) {
			var certificate = new Certificate(certNodes[i], this.divId)
			this.certificates.push(certificate)
			this.certificatesBy64.set(certificate.base64, certificate)
		}
	}
	
	renderTable() {
		var html = _t("Status") + "  " + this.status
		if (this.certificates.length == 0)
			return html
		
		var params = sortParams.get(this.divId)
		if (params == null) {
			params = new SortParams(null, null)
			sortParams.set(this.divId, params)
		}
		
		var newOrder
		if (params.order == "descending")
			newOrder = "ascending"
		else if (params.order == "ascending")
			newOrder = "descending"
		var table = new Table(["Issuer", "Name", "Timestamp", "Import"], "sort", params.key, newOrder, this.divId)
		table.setCSSClass("certificates")
		
		var i
		for (i = 0; i < this.certificates.length; i++) {
			table.addRow(this.certificates[i].getMapping())
		}
		
		html += "  "
		html += _t("Certificates") + "  " + this.certificates.length + "/" + this.total
		html += "<br/>"
		html += table.render()
		
		return html
	}
}

class CertificateFetch {
	constructor(senderB64, fileInfoHash) {
		this.senderB64 = senderB64
		this.fileInfoHash = fileInfoHash
		this.divId = senderB64 + "_" + fileInfoHash
		this.lastResponse = null
	}
	
	updateTable() {
		var fetch = this
		var block = document.getElementById("certificates-" + this.divId)
		
		if (block == null) {
			// can happen if the user clicks away without hiding first
			certificateFetches.delete(this.divId)
			return
		}
		
		var xmlhttp = new XMLHttpRequest()
		xmlhttp.onreadystatechange = function() {
			if (this.readyState == 4 && this.status == 200) {
				fetch.lastResponse = new CertificateResponse(this.responseXML, fetch.divId)
				block.innerHTML = fetch.lastResponse.renderTable()
			}			
		}
		var queryParams = ""
		var params = sortParams.get(this.divId)
		if (params != null) {
			queryParams = "&key=" + params.key + "&order=" + params.order
		}
		xmlhttp.open("GET", "/MuWire/Certificate?user=" + this.senderB64 + "&infoHash=" + this.fileInfoHash + queryParams, true)
		xmlhttp.send()
	}
}

function sort(key, order, id) {
	var params = sortParams.get(id)
	params.key = key
	params.order = order
	refreshCertificates()
}

function importCertificate(b64) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshCertificates()
		}
	}
	xmlhttp.open("POST", "/MuWire/Certificate", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=import&base64=" + b64)
}

function showCertificateComment(divId, base64) {
	var certificateResponse = certificateFetches.get(divId).lastResponse
	var certificate = certificateResponse.certificatesBy64.get(base64)
	expandedCertificateComments.set(divId + "_" + base64, true)

	var linkDiv = document.getElementById("certificate-comment-link-" + divId + "_" + base64)
	var linkText = _t("Hide Comment")
	var link = "<a href='#', onclick='window.hideCertificateComment(\"" + divId + "\",\"" + base64 + "\");return false;'>" + linkText + "</a>"
	linkDiv.innerHTML = link
	
	var commentDiv = document.getElementById("certificate-comment-" + divId + "_" + base64)
	var commentHtml = "<pre class='comment'>" + certificate.comment + "</pre>"
	commentDiv.innerHTML = commentHtml
	
}

function hideCertificateComment(divId, base64) {
	var certificateResponse = certificateFetches.get(divId).lastResponse
	var certificate = certificateResponse.certificatesBy64.get(base64)
	expandedCertificateComments.delete(divId + "_" + base64)
	
	var linkDiv = document.getElementById("certificate-comment-link-" + divId + "_" + base64)
	var linkText = _t("Show Comment")
	var link = "<a href='#' onclick='window.showCertificateComment(\"" + divId + "\",\"" + base64 + "\");return false;'>" + linkText + "</a>"
	linkDiv.innerHTML = link
	
	var commentDiv = document.getElementById("certificate-comment-" + divId + "_" + base64)
	commentDiv.textContent = ""
}

function refreshCertificates() {
	for (var [ignored, fetch] of certificateFetches) {
		fetch.updateTable()
	}
}

function initCertificates() {
	setInterval(refreshCertificates, 3000)
	setTimeout(refreshCertificates, 1)
}
