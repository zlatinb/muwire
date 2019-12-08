class Persona {
	constructor(xmlNode) {
		this.user = xmlNode.getElementsByTagName("User")[0].childNodes[0].nodeValue
		this.userB64 = xmlNode.getElementsByTagName("UserB64")[0].childNodes[0].nodeValue
		this.reason = xmlNode.getElementsByTagName("Reason")[0].childNodes[0].nodeValue
	}
}

var trusted = new Map()
var distrusted = new Map()
var revision = -1

function updateTable(map, divId) {
	var divElement = document.getElementById(divId)
	var tableHtml = "<table><thead><tr><th>User</th><th>Reason</th><th>Actions</th></tr></thead><tbody>"
	
	var isTrusted = (map == trusted)
	for (var [ignored, user] of map) {
		tableHtml += "<tr>"
		tableHtml += "<td>" + user.user + "</td>"
		tableHtml += "<td>" + user.reason + "</td>"
		
		if (isTrusted)
			tableHtml += "<td>Mark Neutral Mark Distrusted</td>
		else
			tableHtml += "<td>Mark Neutral Mark Trusted</td>"
		
		tableHtml += "</tr>"
	}
	tableHtml += "</tbody></table>"
	divElement.innerHTML = tableHtml
}

function refreshUsers() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			trusted.clear()
			distrusted.clear()
			var trustedElement = this.responseXML.getElementsByTagName("Trusted")
			var trustedUsers = trustedElement.getElementsByTagName("Persona")
			var i
			for (i = 0; i < trustedUsers.length; i++) {
				var persona = new Persona(trustedUsers[i])
				trusted.set(persona.user, persona)
			}
			
			var distrustedElement = this.responseXML.getElementsByTagName("Distrusted")
			var distrustedUsers = distrustedElement.getElementsByTagName("Persona")
			for (i = 0; i < distrustedUsers.length; i++) {
				var persona = new Persona(distrustedUsers[i])
				distrusted.set(persona.user, persona)
			}

			updateTable(trusted, "trustedUsers")
			updateTable(distrusted, "distrustedUsers")				
		
		}
	}
	xmlhttp.open("GET", "/MuWire/Trust?section=users", true)
	xmlhttp.send()
}

function fetchRevision() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var xmlDoc = this.responseXML
			var newRevision = xmlDoc.childNodes[0].nodeValue
			if (newRevision > revision) {
				revision = newRevision
				refreshUsers()
			}
		}
	}
	xmlhttp.open("GET", "/MuWire/Trust?section=revision", true)
	xmlhttp.send()
}

function initTrustUsers() {
	setTimeout(fetchRevision, 1)
	setInterval(fetchRevision, 3000)
}