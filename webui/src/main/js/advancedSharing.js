class Directory {
	constructor(xmlNode) {
		this.directory = xmlNode.getElementsByTagName("Directory")[0].childNodes[0].nodeValue
		this.path = Base64.encode(this.directory)
		this.autoWatch = xmlNode.getElementsByTagName("AutoWatch")[0].childNodes[0].nodeValue
		this.syncInterval = xmlNode.getElementsByTagName("SyncInterval")[0].childNodes[0].nodeValue
		this.lastSync = xmlNode.getElementsByTagName("LastSync")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		
		var configLink = new Link(_t("Configure"), "configure", [this.path])
		var divRight = "<span class='right'><div class='dropdown'><a class='droplink'>" + _t("Actions") + "</a><div class='dropdown-content'>" +
			configLink.render() + "</div></div></span>"
			
		mapping.set("Directory", this.directory + divRight)
		mapping.set("Auto Watch", this.autoWatch)
		mapping.set("Last Sync", this.lastSync)
		mapping.set("Sync Interval", this.syncInterval)
		
		return mapping
	}
}

function initAdvancedSharing() {
	setInterval(fetchRevision, 3000)
	setTimeout(fetchRevision, 1)
}

function fetchRevision() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var newRevision = parseInt(this.responseXML.getElementsByTagName("Revision")[0].childNodes[0].nodeValue)
			if (newRevision > revision) {
				revision = newRevision
				refreshDirs()
			}
		}
	}
	xmlhttp.open("GET", "/MuWire/AdvancedShare?section=revision", true)
	xmlhttp.send()
}

function refreshDirs() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			pathToDir.clear()
			var listOfDirs = []
			var dirNodes = this.responseXML.getElementsByTagName("WatchedDir")
			var i
			for (i = 0; i < dirNodes.length; i ++) {
				var dir = new Directory(dirNodes[i])
				listOfDirs.push(dir)
				pathToDir.set(dir.path, dir)
			}
			
			var newOrder
			if (sortOrder == "descending")
				newOrder = "ascending"
			else if (sortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Directory", "Auto Watch", "Sync Interval", "Last Sync"], "sortDirs", sortKey, newOrder, null)
			
			for (i = 0; i < listOfDirs.length; i++) {
				table.addRow(listOfDirs[i].getMapping())
			}
			
			var dirsDiv = document.getElementById("dirsTable")
			if (listOfDirs.length > 0)
				dirsDiv.innerHTML = table.render()
			else
				dirsDiv.innerHTML = ""
		}
	}
	xmlhttp.open("GET", "/MuWire/AdvancedShare?section=dirs", true)
	xmlhttp.send()
}

function sortDirs(key, order) {
	sortKey = key
	sortOrder = order
	refreshDirs()
}

function configure(path) {
	var dir = pathToDir.get(path)
	
	var html = "<form action='/MuWire/AdvancedShare' method='post'>"
	html += "<h3>" + _t("Directory configuration for {0}", dir.directory) + "</h3>"
	
	html += "<table>"
	
	html += "<tr>"
	html += "<td>" + _t("Watch directory for changes using operating system") + "</td>"
	html += "<td><p align='right'><input type='checkbox' name='autoWatch' value='true'"
	if (dir.autoWatch == "true")
		html += " checked "
	html += "></p></td>"
	html += "</tr>"
	
	html += "<tr>"
	html += "<td>" + _t("Directory sync frequency (seconds, 0 means never)") + "</td>"
	html += "<td><p align='right'><input type='text' size='3' name='syncInterval' value='" + dir.syncInterval + "'></p></td>"
	html += "</tr>"
	
	html += "</table>"
	
	html += "<input type='hidden' name='path' value='" + path + "'>"
	html += "<input type='hidden' name='action' value='configure'>"
	
	html += "<input type='submit' value='" + _t("Save") + "'>"
	html += "<a href='#' onclick='window.cancelConfig();return false;'>" + _t("Cancel") + "</a>"
	html += "</form>"
	
	var tableDiv = document.getElementById("dirConfig")
	tableDiv.innerHTML = html
}

function cancelConfig() {
	var tableDiv = document.getElementById("dirConfig")
	tableDiv.innerHTML = ""
}

var revision = -1
var pathToDir = new Map()

var sortKey = "Directory"
var sortOrder = "descending"