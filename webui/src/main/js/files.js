
class Node {
	constructor(nodeId, parent, leaf, infoHash, path, size, comment, certified, published, shared, revision) {
		this.nodeId = nodeId
		this.parent = parent
		this.leaf = leaf
		this.infoHash = infoHash
		this.children = []
		this.path = path
		this.size = size
		this.comment = comment
		this.certified = certified
		this.published = published
		this.revision = revision
		this.shared = shared
	}
	
	updateDiv() {
		var div = document.getElementById(this.nodeId)
		var unshareLink = "<a href='#' onclick='window.unshare(\"" + this.nodeId +"\");return false;'>" + _t("Unshare") + "</a>"
		if (!this.shared)
			unshareLink = ""
		var commentLink = "<span id='comment-link-"+this.nodeId+"'><a href='#' onclick='window.showCommentForm(\"" + this.nodeId + "\");return false;'>" + _t("Comment") + "</a></span>";
		if (!this.shared)
			commentLink = ""
		var certifyLink = "<a href='#' onclick='window.certify(\"" + this.nodeId + "\");return false;'>" + _t("Certify") + "</a>"
		if (!this.shared)
			certifyLink = ""
		var publishLink = "<a href='#' onclick='window.publish(\"" + this.nodeId + "\");return false;'>" + _t("Publish") + "</a>"
		if (!this.shared)
			publishLink = ""
		var actionsLink = "<a class='droplink' href='#'>" + _t("Actions") + "</a>"
		if (!this.shared)
			actionsLink = ""
		if (this.leaf) {
			var certified = ""
			if (this.certified == "true") 
				certified = _t("Certified")
			var publish
			var published
			if (this.published == "true") {
				publish = new Link(_t("Unpublish"), "unpublish", [this.nodeId])
				published = _t("Published")
			} else {
				publish = new Link(_t("Publish"), "publish", [this.nodeId])
				published = ""
			}
			
			var nameLink = "<a href='/MuWire/DownloadedContent/" + this.infoHash + "'>" + this.path + "</a>"
			var html = "<li class='fileTree'>" + nameLink
			html += "<div class='right'>" + certified + "  " + published + "   <div class='dropdown'>" + actionsLink + "<div class='dropdown-content'>"
			html += unshareLink + commentLink + certifyLink + publish.render()
			html += "</div></div></div>"
			html += "<div class='centercomment' id='comment-" + this.nodeId + "'></div>"
			html += "</li>"
			
			div.innerHTML = html
		} else {
			if (this.children.length == 0) {
				
				var link = "<a class='caret' href='#' onclick='window.expand(\"" + this.nodeId + "\");return false'>" + this.path + "</a>"
				var commentDiv = "<div class='centercomment' id='comment-" + this.nodeId + "'></div>"
				var html = "<li>" + link + "<span class='right'><div class='dropdown'>" + actionsLink + "<div class='dropdown-content'>" 
				html += unshareLink + commentLink + certifyLink + "</div></div></span>" + commentDiv + "</li>"
				div.innerHTML = html				
			} else {
				var link = "<a class='caret caret-down' href='#' onclick='window.collapse(\"" + this.nodeId + "\");return false;'>"+this.path+"</a>"
				var commentDiv = "<div class='centercomment' id='comment-" + this.nodeId + "'></div>"
				var l = "<li>" + link + "<span class='right'><div class='dropdown'>" + actionsLink + "<div class='dropdown-content'>" 
				l += unshareLink + commentLink + certifyLink + "</div></div></span>" + commentDiv
				
				l += "<ul class='fileTree'>"
				var i
				for (i = 0; i < this.children.length; i++) {
					l += "<li class='fileTree'>"
					l += "<div id='" + this.children[i].nodeId+"'></div>"
					l += "</li>"
				}
				l += "</ul></li>"
				div.innerHTML = l
			}
		}
	}
}


function fetch(infoHash) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.open("GET", "/MuWire/DownloadedContent/" + infoHash)
	xmlhttp.send()
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
				
			var newRevision = parseInt(xmlDoc.getElementsByTagName("Revision")[0].childNodes[0].nodeValue)
			if (newRevision > treeRevision) {
				// TODO: update expanded nodes
				treeRevision = newRevision
			}
		}
	}
	xmlhttp.open("GET", "/MuWire/Files?section=status", true)
	xmlhttp.send();
}

var treeRevision = -1
var root
var nodesById = new Map()

function initFiles() {
	root = new Node("root",null,false, null, _t("Shared Files"), -1, null, false, false, -1)
	setInterval(refreshStatus, 3000)
	setTimeout(refreshStatus, 1)
	
	nodesById.set("root",root)
	root.updateDiv()
	expand(root.nodeId)
}

function encodedPathToRoot(node) {
	var pathElements = []
	var tmpNode = node
	while(tmpNode.parent != null) {
		pathElements.push(Base64.encode(tmpNode.path))
		tmpNode = tmpNode.parent
	}
	var reversedPath = []
	while(pathElements.length > 0)
		reversedPath.push(pathElements.pop())
	var encodedPath = reversedPath.join(",")
	return encodedPath	
}

function expand(nodeId) {
	var node = nodesById.get(nodeId)
	var encodedPath = encodedPathToRoot(node)	
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML
			var revision = parseInt(xmlDoc.getElementsByTagName("Revision")[0].childNodes[0].nodeValue)
			
			var i
			var elements = xmlDoc.getElementsByTagName("Files")[0]
			
			for (i = 0; i < elements.childNodes.length; i++) {
				var element = elements.childNodes[i]
				if (element.nodeName == "File") {
					var fileName = element.getElementsByTagName("Name")[0].childNodes[0].nodeValue
					var infoHash = element.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue
					var size = element.getElementsByTagName("Size")[0].childNodes[0].nodeValue
					var comment = element.getElementsByTagName("Comment")
					if (comment != null && comment.length == 1)
						comment = comment[0].childNodes[0].nodeValue
					else
						comment = null
					var certified = element.getElementsByTagName("Certified")[0].childNodes[0].nodeValue
					var published = element.getElementsByTagName("Published")[0].childNodes[0].nodeValue
					
					var nodeId = node.nodeId + "_"+ Base64.encode(fileName)
					var newFileNode = new Node(nodeId, node, true, infoHash, fileName, size, comment, certified, published, true, revision)
					nodesById.set(nodeId, newFileNode)
					node.children.push(newFileNode)
				} else if (element.nodeName == "Directory") {
					var dirName = element.getElementsByTagName("Name")[0].childNodes[0].nodeValue
					var shared = parseBoolean(element.getElementsByTagName("Shared")[0].childNodes[0].nodeValue)
					var nodeId = node.nodeId + "_"+ Base64.encode(dirName)
					var newDirNode = new Node(nodeId, node, false, null, dirName, -1, null, false, false, shared, revision)
					nodesById.set(nodeId, newDirNode)
					node.children.push(newDirNode)
				}
			}
			
			node.updateDiv()
		    for (i = 0; i < node.children.length; i++) {
				node.children[i].updateDiv()
			}
			if (node.children.length == 1 && !node.children[0].leaf)
				expand(node.children[0].nodeId)
		}
	}
	xmlhttp.open("GET", "/MuWire/Files?section=fileTree&path="+encodedPath, true)
	xmlhttp.send()
}


function collapse(nodeId) {
	var node = nodesById.get(nodeId)
	node.children = []
	node.updateDiv()
}

function unshare(nodeId) {
	var node = nodesById.get(nodeId)
	var encodedPath = encodedPathToRoot(node)
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var parent = node.parent
			if (parent == null)
				parent = root
			collapse(parent.nodeId)
			expand(parent.nodeId)
		}
	}
	xmlhttp.open("POST", "/MuWire/Files", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=unshare&path="+encodedPath)
}

function showCommentForm(nodeId) {
	var linkSpan = document.getElementById("comment-link-"+nodeId)
	linkSpan.innerHTML=""
	var commentDiv = document.getElementById("comment-"+nodeId)
	
	var node = nodesById.get(nodeId)
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
	
	var node = nodesById.get(nodeId)
	node.updateDiv()
}

function saveComment(nodeId) {
	var comment = document.getElementById("comment-text-"+nodeId).value
	var node = nodesById.get(nodeId)
	var encodedPath = encodedPathToRoot(node)
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			cancelComment(nodeId)
			collapse(node.parent.nodeId)
			expand(node.parent.nodeId) // this can probably be done smarter
		}
	} 
	xmlhttp.open("POST", "/MuWire/Files", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("action=comment&path="+encodedPath+ "&comment="+comment))
}

function certify(nodeId) {
	var node = nodesById.get(nodeId)
	var encodedPath = encodedPathToRoot(node)
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			collapse(node.parent.nodeId)
			expand(node.parent.nodeId)
		}
	}
	xmlhttp.open("POST", "/MuWire/Certificate", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=certify&file=" + encodedPath)
}

function publish(nodeId) {
	var node = nodesById.get(nodeId)
	var encodedPath = encodedPathToRoot(node)
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			collapse(node.parent.nodeId)
			expand(node.parent.nodeId)
		}
	}
	xmlhttp.open("POST", "/MuWire/Feed", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=publish&file=" + encodedPath)
}

function unpublish(nodeId) {
	var node = nodesById.get(nodeId)
	var encodedPath = encodedPathToRoot(node)
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			collapse(node.parent.nodeId)
			expand(node.parent.nodeId)
		}
	}
	xmlhttp.open("POST", "/MuWire/Feed", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=unpublish&file=" + encodedPath)
}