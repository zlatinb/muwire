
function showLoader() {
	const textSpan = document.getElementById("dropBoxText")
	textSpan.style.display = "none"
	const loaderSpan = document.getElementById("dropBoxLoader")
	loaderSpan.style.display = "inline"
}

function hideLoader() {
	const textSpan = document.getElementById("dropBoxText")
	textSpan.style.display = "inline"
	const loaderSpan = document.getElementById("dropBoxLoader")
	loaderSpan.style.display = "none"
}

function sendFiles(files) {
	const uri = "/MuWire/DropBox"
	const xhr = new XMLHttpRequest()
	const fd = new FormData()
	
	for (let i = 0; i < files.length; i ++) {
		fd.append("myFile", files[i])
	}
	
	xhr.open("POST", uri, true)
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status == 200) {
			hideLoader()		
		}
	}
	xhr.send(fd)
}

function initDropBox() {
	const dropBox = document.getElementById("dropBox")
	dropBox.ondragover = dropBox.ondragenter = function(e) {
		e.stopPropagation()
		e.preventDefault()
	}
	
	dropBox.ondrop = function(e) {
		e.stopPropagation()
		e.preventDefault()
		
		showLoader()
		const filesArray = event.dataTransfer.files;
		sendFiles(filesArray)
	}
}

document.addEventListener("DOMContentLoaded", function() {
   initDropBox();
}, true);
