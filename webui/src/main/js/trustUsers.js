class Persona {
	constructor(xmlNode) {
		this.user = xmlNode.getElementsByTagName("User")[0].childNodes[0].nodeValue
		this.userB64 = xmlNode.getElementsByTagName("UserB64")[0].childNodes[0].nodeValue
		this.reason = xmlNode.getElementsByTagName("Reason")[0].childNodes[0].nodeValue
	}
	
	getTrustedLink() {
		return "<a herf='#' onclick='window.markTrusted(\"" + this.userB64 + "\"); return false;'>Mark Trusted</a>"
	}
	
	getNeutralLink() {
		return "<a herf='#' onclick='window.markNeutral(\"" + this.userB64 + "\"); return false;'>Mark Neutral</a>"
	}
	
	getDistrustedLink() {
		return "<a herf='#' onclick='window.markDistrusted(\"" + this.userB64 + "\"); return false;'>Mark Distrusted</a>"
	}
} 

var trusted = new Map()
var distrusted = new Map()
var revision = -1

function markTrusted(host) {
	var linkSpan = document.getElementById("trusted-link-"+host)
	linkSpan.innerHTML = ""
	
	var textAreaSpan = document.getElementById("trusted-"+host)
	
	var textbox = "<textarea id='trust-reason-" + host + "'></textarea>"
	var submitLink = "<a href='#' onclick='window.submitTrust(\"" + host + "\");return false;'>Submit</a>"
	var cancelLink = "<a href='#' onclick='window.cancelTrust(\"" + host + "\");return false;'>Cancel</a>"
	
	var html = "<br/>Enter Reason (Optional)<br/>" + textbox + "<br/>" + submitLink + " " + cancelLink + "<br/>"
	
	textAreaSpan.innerHTML = html
}

function markNeutral(host) {
	publishTrust(host, "", "neutral")
}

function markDistrusted(host) {
	var linkSpan = document.getElementById("distrusted-link-"+host)
	linkSpan.innerHTML = ""
	
	var textAreaSpan = document.getElementById("distrusted-"+host)
	
	var textbox = "<textarea id='distrust-reason-" + host + "'></textarea>"
	var submitLink = "<a href='#' onclick='window.submitDistrust(\"" + host + "\");return false;'>Submit</a>"
	var cancelLink = "<a href='#' onclick='window.cancelDistrust(\"" + host + "\");return false;'>Cancel</a>"
	
	var html = "<br/>Enter Reason (Optional)<br/>" + textbox + "<br/>" + submitLink + " " + cancelLink + "<br/>"
	
	textAreaSpan.innerHTML = html
}

function submitTrust(host) {
	var reason = document.getElementById("trust-reason-"+host).value
	publishTrust(host, reason, "trust")
}

function submitDistrust(host) {
	var reason = document.getElementById("distrust-reason-"+host).value
	publishTrust(host, reason, "distrust")
}


function publishTrust(host, reason, trust) {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			refreshUsers()
		}
	}
	xmlhttp.open("POST","/MuWire/Trust", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send("action=" + trust + "&reason=" + reason + "&persona=" + host)
}

function cancelTrust(host) {
	var textAreaSpan = document.getElementById("trusted-" + host)
	textAreaSpan.innerHTML = ""
	
	var linkSpan = document.getElementById("trusted-link-"+host)
	var html = "<a href='#' onclick='markTrusted(\"" + host + "\"); return false;'>Mark Trusted</a>"
	linkSpan.innerHTML = html
}

function cancelDistrust(host) {
	var textAreaSpan = document.getElementById("distrusted-" + host)
	textAreaSpan.innerHTML = ""
	
	var linkSpan = document.getElementById("distrusted-link-"+host)
	var html = "<a href='#' onclick='markDistrusted(\"" + host + "\"); return false;'>Mark Distrusted</a>"
	linkSpan.innerHTML = html
}

function updateTable(map, divId) {
	var divElement = document.getElementById(divId)
	var tableHtml = "<table><thead><tr><th>User</th><th>Reason</th><th>Actions</th></tr></thead><tbody>"
	
	var isTrusted = (map == trusted)
	for (var [ignored, user] of map) {
		tableHtml += "<tr>"
		tableHtml += "<td>" + user.user + "</td>"
		tableHtml += "<td>" + user.reason + "</td>"
		
		tableHtml += "<td>"
		if (isTrusted) {
			tableHtml += user.getNeutralLink() + " <span id='distrusted-link-" + user.userB64 + "'>" + user.getDistrustedLink() + "</span>" +
						"<span id='distrusted-" + user.userB64 + "'></span>"
		} else {
			tableHtml += user.getNeutralLink() + " <span id='trusted-link-" + user.userB64 + "'>" + user.getTrustedLink() + "</span>" +
						"<span id='trusted-" + user.userB64 + "'></span>"
		}
		tableHtml += "</td>"
		
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
			var trustedElement = this.responseXML.getElementsByTagName("Trusted")[0]
			var trustedUsers = trustedElement.getElementsByTagName("Persona")
			var i
			for (i = 0; i < trustedUsers.length; i++) {
				var persona = new Persona(trustedUsers[i])
				trusted.set(persona.user, persona)
			}
			
			var distrustedElement = this.responseXML.getElementsByTagName("Distrusted")[0]
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
			var newRevision = xmlDoc.childNodes[0].childNodes[0].nodeValue
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