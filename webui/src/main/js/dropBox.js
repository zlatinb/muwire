
function sendFiles(files) {
	const uri = "/MuWire/DropBox"
	const xhr = new XMLHttpRequest()
	const fd = new FormData()
	const dropBoxText = document.getElementById("dropBoxText")
	
	for (let i = 0; i < files.length; i ++) {
		fd.append("myFile", files[i])
	}
	
	xhr.open("POST", uri, true)
	xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status == 200) {
			dropBoxText.innerText = _t("Drag and drop files here to share them")
		}
	}
	dropBoxText.innerText = _t("Sending {0} files to your MuWire drop box, please wait.", files.length)
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
		
		
		const filesArray = e.dataTransfer.files;
		sendFiles(filesArray)
	}
}

document.addEventListener("DOMContentLoaded", function() {
   initDropBox();
}, true);
