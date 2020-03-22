class SharedFile {
	constructor(name, infoHash, path, size, comment, certified, published) {
		this.name = name
		this.infoHash = infoHash
		this.path = path
		this.size = size
		this.comment = comment
		this.certified = certified
		this.published = published
	}
	
	getMapping() {
		var mapping = new Map()
		
		var unshareLink = new Link(_t("Unshare"), "unshare", [this.path])
		var certifyLink = new Link(_t("Certify"), "certify", [this.path])
		var certified
		if (this.certified == "true")
			certified = " " + _t("Certified")
		else
			certified = ""
		var publishLink
		var published
		if (this.published == "true") {
			publishLink = new Link(_t("Unpublish"), "unpublish", [this.path])
			published = _t("Published")
		} else {
		    publishLink = new Link(_t("Publish"), "publish", [this.path])
			published = ""
		}

		var infoHashTextArea = "<textarea class='copypaste' readOnly='true' id='" + this.infoHash + "'>" + this.infoHash + "</textarea>"
		var copyInfoHashLink = new Link(_t("Copy hash to clipboard"), "copyAndAlert", [this.infoHash, _t("Hash copied to clipboard")])

		var showCommentHtml = ""
		var showCommentLink = new Link(_t("Comment"), "showCommentForm", [this.path])
		showCommentHtml = "<span id='comment-link-" + this.path + "'>" + showCommentLink.render() + "</span>"
		var commentDiv = "<div class='centercomment' id='comment-" + this.path + "'></div>"
		var nameLink = "<a href='/MuWire/DownloadedContent/" + this.infoHash + "'>" + this.name + "</a>"
		var detailsLink = "<a href='/MuWire/FileDetails?path=" + encodeURI(this.path) + "'>" + _t("Show Details") + "</a>"
		
		var html = nameLink + infoHashTextArea + "<div class=\"right\">" + certified + "  " + published + "  "
		html += "<div class='dropdown'><a class='droplink'>" + _t("Actions") + "</a><div class='dropdown-content'>"
		html += copyInfoHashLink.render() + unshareLink.render() + showCommentHtml + certifyLink.render() + publishLink.render() + detailsLink
		html += "</div></div></div>"
		html += "<br/>" + commentDiv
		
		mapping.set("File", html)
		mapping.set("Size", this.size)
		
		return mapping
	}
}

function refreshStatus() {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML;
			
			var count = xmlDoc.getElementsByTagName("Count")[0].childNodes[0].nodeValue
			var countSpan = document.getElementById("count")
			countSpan.innerHTML = count
			
			var hashingSpan = document.getElementById("hashing")
			var hashing = xmlDoc.getElementsByTagName("Hashing")
			if (hashing != null && hashing.length == 1) {
				hashingSpan.innerHTML = _t("Hashing") + " " +hashing[0].childNodes[0].nodeValue
			} else
				hashingSpan.innerHTML = "";
				
			var newRevision = parseInt(xmlDoc.getElementsByTagName("Revision")[0].childNodes[0].nodeValue)
			if (newRevision > tableRevision) {
				tableRevision = newRevision // TODO: auto-refresh
				refreshTable()
			} 
		}
	}
	xmlhttp.open("GET", "/MuWire/Files?section=status", true)
	xmlhttp.send();
}

var tableRevision = -1
var filesByPath = new Map()
var sortKey = "File"
var sortOrder = "descending"

function sort(key, order) {
	sortKey = key
	sortOrder = order
	refreshTable()
}

function refreshTable() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML
			
			var filesList = []			
			var files = xmlDoc.getElementsByTagName("File")
			var i
			for(i = 0; i < files.length; i++) {
				var fileName = files[i].getElementsByTagName("Path")[0].childNodes[0].nodeValue
				var infoHash = files[i].getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue
				var size = files[i].getElementsByTagName("Size")[0].childNodes[0].nodeValue
				var comment = files[i].getElementsByTagName("Comment")
				if (comment != null && comment.length == 1)
					comment = comment[0].childNodes[0].nodeValue
				else
					comment = null
				var certified = files[i].getElementsByTagName("Certified")[0].childNodes[0].nodeValue
				var published = files[i].getElementsByTagName("Published")[0].childNodes[0].nodeValue
				
				var path = Base64.encode(fileName)
				var newSharedFile = new SharedFile(fileName, infoHash, path, size, comment, certified, published)
				filesByPath.set(path, newSharedFile)
				filesList.push(newSharedFile)
			}
			
			var newOrder
			if (sortOrder == "descending")
				newOrder = "ascending"
			else if (sortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["File","Size"], "sort", sortKey, newOrder, null)
			
			for (i = 0; i < filesList.length; i++) {
				table.addRow(filesList[i].getMapping())
			}
			
			var tableDiv = document.getElementById("filesTable")
			if (filesList.length > 0 )
				tableDiv.innerHTML = table.render()
			else
				tableDiv.innerHTML = ""
		}
	}
	var sortParam = "&key=" + sortKey + "&order=" + sortOrder
	xmlhttp.open("GET", "/MuWire/Files?section=fileTable" + sortParam, true)
	xmlhttp.send()
}

function initFiles() {
	setInterval(refreshStatus, 3000)
	setTimeout(refreshStatus, 1)

	setTimeout(refreshTable, 1)	
}

function unshare(fileId) {
	var file = filesByPath.get(fileId)
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			filesByPath.delete(fileId)
			refreshTable()
		}
	}
	xmlhttp.open("POST", "/MuWire/Files", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=unshare&path="+fileId)
}

function showCommentForm(nodeId) {
	var linkSpan = document.getElementById("comment-link-"+nodeId)
	linkSpan.innerHTML=""
	var commentDiv = document.getElementById("comment-"+nodeId)
	
	var node = filesByPath.get(nodeId)
	var existingComment = node.comment == null ? "" : node.comment
	
	var textArea = "<textarea id='comment-text-" + nodeId + "'>"+existingComment+"</textarea>" 
	var saveCommentLink = "<a href='#' onclick='saveComment(\"" + nodeId + "\");return false;'>" + _t("Save") + "</a>"
	var cancelCommentLink = "<a href='#' onclick='cancelComment(\"" + nodeId + "\");return false;'>" + _t("Cancel") + "</a>"
	
	var html = textArea + "<br/>" + saveCommentLink + "  " + cancelCommentLink
	
	commentDiv.innerHTML = html
}

function cancelComment(nodeId) {
	var commentDiv = document.getElementById("comment-"+nodeId)
	commentDiv.innerHTML = ""
	
	var commentLink = new Link(_t("Comment"), "showCommentForm", [nodeId])
	
	var linkSpan = document.getElementById("comment-link-"+nodeId)
	linkSpan.innerHTML = commentLink.render()
}	

function saveComment(fileId) {
	var comment = document.getElementById("comment-text-"+fileId).value
	var file = filesByPath.get(fileId)
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			cancelComment(fileId)
			refreshTable()
		}
	}
	xmlhttp.open("POST", "/MuWire/Files", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=comment&path="+fileId+ "&comment="+comment))
}

function certify(path) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshTable()
		}
	}
	xmlhttp.open("POST", "/MuWire/Certificate", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=certify&file=" + path)
}

function publish(path) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshTable()
		}
	}
	xmlhttp.open("POST", "/MuWire/Feed", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=publish&file=" + path)
}

function unpublish(path) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshTable()
		}
	}
	xmlhttp.open("POST", "/MuWire/Feed", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=unpublish&file=" + path)
}
