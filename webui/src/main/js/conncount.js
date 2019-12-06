function refreshConnectionsCount() {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var connections = this.responseXML.getElementsByTagName("Connections");
			var count = connections[0].childNodes[0].nodeValue
			var connectionCountSpan = document.getElementById("connectionsCount");
			var countString = ""+count;
			connectionCountSpan.innerHTML = countString;
		} else {
			connectionCountSpan.innerHTML = "down";
		}
	}
	xmlhttp.open("GET", "/MuWire/Search?section=connectionsCount", true);
	xmlhttp.send();
}

function initConnectionsCount() {
	setInterval(refreshConnectionsCount, 3000);
	setTimeout(refreshConnectionsCount, 1);
}
