class SharedFile {
	constructor(name, size, comment) {
		this.name = name
		this.size = size
		this.comment = comment
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
				hashingSpan.innerHTML = "Hashing "+hashing[0].childNodes[0].nodeValue
			} else
				hashingSpan.innerHTML = "";
				
			var newRevision = xmlDoc.getElementsByTagName("Revision")[0].childNodes[0].nodeValue
			var refreshDiv = document.getElementById("refresh-link")
			if (newRevision > tableRevision) {
				tableRevision = newRevision
				refreshDiv.innerHTML = "<a href='#' onclick='refreshTable();return false;'>Refresh</a>"
			} else
				refreshDiv.innerHTML = ""
		}
	}
	xmlhttp.open("GET", "/MuWire/Files?section=status", true)
	xmlhttp.send();
}

var tableRevision = -1
var filesByPath = new Map()

function refreshTable() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML
			
			var tableHtml = "<table><thead><tr><th>File</th><th>Size</th><th>Comment</th></tr></thead><tbody>"
			
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
				
				var nodeId = Base64.encode(fileName)
				var newSharedFile = new SharedFile(fileName, size, comment)
				filesByPath.set(nodeId, newSharedFile)
			}
			
			for (var [path, file] of filesByPath) {
				
				var unshareLink = "<a href='#' onclick='window.unshare(\"" + path + "\");return false;'>Unshare</a>"
				
				var commentLink = "<span id='comment-link-"+ path +"'>" + generateCommentLink(path) + "</span>"
				
				tableHtml += "<tr>"
				tableHtml += "<td>"+file.name+"<br/>" + unshareLink + commentLink + "<div id='comment-" + path + "'></div></td>"
				tableHtml += "<td>"+file.size+"</td>"
				tableHtml += "<td>"+(file.comment != null)+"</td>"
				tableHtml += "</tr>"
			}
			
			tableHtml += "</tbody></table>"
			
			var tableDiv = document.getElementById("filesTable")
			tableDiv.innerHTML = tableHtml
		}
	}
	xmlhttp.open("GET", "/MuWire/Files?section=fileTable", true)
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
	var saveCommentLink = "<a href='#' onclick='saveComment(\"" + nodeId + "\");return false;'>Save</a>"
	var cancelCommentLink = "<a href='#' onclick='cancelComment(\"" + nodeId + "\");return false;'>Cancel</a>"
	
	var html = textArea + "<br/>" + saveCommentLink + "  " + cancelCommentLink
	
	commentDiv.innerHTML = html
}

function cancelComment(nodeId) {
	var commentDiv = document.getElementById("comment-"+nodeId)
	commentDiv.innerHTML = ""
	var linkSpan = document.getElementById("comment-link-"+nodeId)
	linkSpan.innerHTML = generateCommentLink(nodeId)
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

function generateCommentLink(nodeId) {
	return "<a href='#' onclick='showCommentForm(\"" + nodeId + "\");return false;'>Comment</a>"
}
