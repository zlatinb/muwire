function refreshConnectionsCount() {
	var xmlhttp = new XMLHttpRequest();
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4) {
		  var connectionCountSpan = document.getElementById("connectionsCount");
		  var connectionIcon = document.getElementById("connectionsIcon");
		  if (this.status == 200) {
			
			var networkStatus = this.responseXML.getElementsByTagName("NetworkStatus")[0];
			var image;
			var connections = networkStatus.getElementsByTagName("Connections");
			var count = connections[0].childNodes[0].nodeValue
			var countString = ""+count;
			connectionCountSpan.innerHTML = countString;
			if (count > 0)
			    image = "Connect.png";
			else
			    image = "NotStarted.png";
			connectionIcon.innerHTML = "<img src=\"images/" + image + "\" alt=\"\">";
			
			var downBW = networkStatus.getElementsByTagName("DownBW")[0].childNodes[0].nodeValue
			var downBWSpan = document.getElementById("downBw")
			downBWSpan.innerText = downBW
			
			var upBW = networkStatus.getElementsByTagName("UpBW")[0].childNodes[0].nodeValue
			var upBWSpan = document.getElementById("upBw")
			upBWSpan.innerText = upBW
		  } else {
			connectionCountSpan.textContent = _t("Down");
			connectionIcon.textContent = "";
		  }
		}
	}
	xmlhttp.open("GET", "/MuWire/Search?section=connectionsCount", true);
	xmlhttp.send();
}

function initConnectionsCount() {
	setInterval(refreshConnectionsCount, 3000);
	setTimeout(refreshConnectionsCount, 1);
}

document.addEventListener("DOMContentLoaded", function() {
   initConnectionsCount();
}, true);
