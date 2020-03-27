class Directory {
	constructor(xmlNode) {
		this.directory = xmlNode.getElementsByTagName("Directory")[0].childNodes[0].nodeValue
		this.autoWatch = xmlNode.getElementsByTagName("AutoWatch")[0].childNodes[0].nodeValue
		this.syncInterval = xmlNode.getElementsByTagName("SyncInterval")[0].childNodes[0].nodeValue
		this.lastSync = xmlNode.getElementsByTagName("LastSync")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		var mapping = new Map()
		
		mapping.set("Directory", this.directory)
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
			var listOfDirs = []
			var dirNodes = this.responseXML.getElementsByTagName("WatchedDir")
			var i
			for (i = 0; i < dirNodes.length; i ++) {
				listOfDirs.push(new Directory(dirNodes[i]))
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

var revision = -1

var sortKey = "Directory"
var sortOrder = "descending"