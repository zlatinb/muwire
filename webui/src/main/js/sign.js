function sign() {
	var signText = document.getElementById("sign").value
	
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var signed = this.responseXML.getElementsByTagName("Signed")[0].childNodes[0].nodeValue
			var signedDiv = document.getElementById("signed")
			signedDiv.innerHTML = "<pre>" + signed + "</pre>"
		}
	}
	xmlhttp.open("POST", "/MuWire/Sign", true)
	xmlhttp.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	xmlhttp.send(encodeURI("text=" + signText))
}
