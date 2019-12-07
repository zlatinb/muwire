

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
			if (newRevision > tableRevision) {
				tableRevision = newRevision
				// TODO: let the user know they can refresh the table
			}
		}
	}
	xmlhttp.open("GET", "/MuWire/Files?section=status", true)
	xmlhttp.send();
}

var tableRevision = -1

function initFiles() {
	setInterval(refreshStatus, 3000)
	setTimeout(refreshStatus, 1)
	
	nodesById.set("root",root)
}

