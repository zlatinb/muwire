class SharedFile {
	constructor(name, path, size, comment, certified) {
		this.name = name
		this.path = path
		this.size = size
		this.comment = comment
		this.certified = certified
	}
	
	getMapping() {
		var mapping = new Map()
		
		var unshareLink = new Link(_t("Unshare"), "unshare", [this.path])
		var certifyLink = new Link(_t("Certify"), "certify", [this.path])
		var certifyHtml = certifyLink.render()
		if (this.certified == "true")
			certifyHtml += " " + _t("Certified")
		var showCommentHtml = ""
		var showCommentLink = new Link(_t("Comment"), "showCommentForm", [this.path])
		showCommentHtml = "<span id='comment-link-" + this.path + "'>" + showCommentLink.render() + "</span>"
		showCommentHtml += "<div id='comment-" + this.path + "'></div>"
		mapping.set("File", this.name + " " + unshareLink.render() + " " + certifyHtml + " " + showCommentHtml)
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
				
			var newRevision = xmlDoc.getElementsByTagName("Revision")[0].childNodes[0].nodeValue
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
var sortKey
var sortOrder

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
				var size = files[i].getElementsByTagName("Size")[0].childNodes[0].nodeValue
				var comment = files[i].getElementsByTagName("Comment")
				if (comment != null && comment.length == 1)
					comment = comment[0].childNodes[0].nodeValue
				else
					comment = null
				var certified = files[i].getElementsByTagName("Certified")[0].childNodes[0].nodeValue
				
				var path = Base64.encode(fileName)
				var newSharedFile = new SharedFile(fileName, path, size, comment, certified)
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
			tableDiv.innerHTML = table.render()
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