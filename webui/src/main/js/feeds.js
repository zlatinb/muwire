class Feed {
	constructor(xmlNode) {
		this.publisher = xmlNode.getElementsByTagName("Publisher")[0].childNodes[0].nodeValue
		this.publisherB64 = xmlNode.getElementsByTagName("PublisherB64")[0].childNodes[0].nodeValue
		this.files = xmlNode.getElementsByTagName("Files")[0].childNodes[0].nodeValue
		this.revision = xmlNode.getElementsByTagName("Revision")[0].childNodes[0].nodeValue
		this.status = xmlNode.getElementsByTagName("Status")[0].childNodes[0].nodeValue
		this.active = xmlNode.getElementsByTagName("Active")[0].childNodes[0].nodeValue
		this.lastUpdated = xmlNode.getElementsByTagName("LastUpdated")[0].childNodes[0].nodeValue
	}
	
	getMapping() {
		// TODO: implement
	}
}


class Item {
	constructor(xmlNode) {
		this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue
		this.resultStatus = xmlNode.getElementsByTagName("ResultStatus")[0].childNodes[0].nodeValue
		this.size = xmlNode.getElementsByTagName("Size")[0].childNodes[0].nodeValue
		this.timestamp = xmlNode.getElementsByTagName("Timestamp")[0].childNodes[0].nodeValue
		this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue
		this.certificates = xmlNode.getElementsByTagName("Certificates")[0].childNodes[0].nodeValue
		try {
			this.comment = xmlNode.getElementsByTagName("Comment")[0].childNodes[0].nodeValue
		} catch (ignore) {
			this.comment = ""
		}
	}
	
	getMapping() {
		// TODO: implement
	}
}


function initFeeds() {
	setTimeout(refreshFeeds, 1)
	setInterval(refreshFeeds, 3000)
}


function refreshActive() {
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			feeds.clear()
			var listOfFeeds = []
			var feedNodes = this.responseXML.getElementsByTagName("Feed")
			var i
			for (i = 0; i < feedNodes.length; i++) {
				var feed = new Feed(feedNodes[i])
				feeds.set(feed.publisher, feed)
				listOfFeeds.push(feed)
			}
			
			var newOrder
			if (feedsSortOrder == "descending")
				newOrder = "ascending"
			else if (feedsSortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Publisher", "Files", "Last Updated", "Status"], "sortFeeds", feedsSortKey, newOrder, null)
			
			for (i = 0; i < listOfFeeds.length; i++) {
				table.addRow(listOfFeeds[i].getMapping())
			}
			
			var feedsDiv = document.getElementById("feedsTable")
			if (listOfFeeds.length > 0)
				feedsDiv.innerHTML = table.render()
			else
				feedsDiv.innerHTML = ""
				
			if (currentFeed != null)
				displayFeed(currentFeed)
			else
				document.getElementById("itemsTable").innerHTML = ""
		}
	}
	var sortParam = "&key=" + feedsSortKey + "&order=" + feedsSortOrder
	xmlhttp.open("GET", encodeURI("/MuWire/Feed?section=feeds" + sortParam), true)
	xmlhttp.send()
}

function displayFeed(feed) {
	currentFeed = feed
	var b64 = feeds.get(currentFeed).publisherB64
	var xmlhttp = new XMLHttpRequest()
	xmlhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var items = []
			var itemNodes = this.responseXML.getElementsByTagName("Item")
			var i
			for (i = 0; i < itemNodes.length; i++)
				items.push(new Item(itemNodes[i]))
				
			var newOrder
			if (itemsSortOrder == "descending")
				newOrder = "ascending"
			else if (itemsSortOrder == "ascending")
				newOrder = "descending"
			var table = new Table(["Name", "Size", "Status", "Published"], "sortItems", itemsSortKey, newOrder, null)
			for (i = 0; i < items.length; i++) {
				table.addRow(items[i].getMapping())
			}
			
			var itemsDiv = document.getElementById("itemsTable")
			if (items.length > 0)
				itemsDiv.innerHTML = table.render()
			else
				itemsDiv.innerHTML = ""
		}
	}
	var sortParam = "&key=" + itemsSortKey + "&order=" + itemsSortOrder
	xmlhttp.open("GET", encodeURI("/MuWire/Feed?section=items&publisher=" + b64 + sortParam), true)
	xmlhttp.send()
}

function sortFeeds(key, order) {
	feedsSortKey = key
	feedsSortOrder = order
	refreshActive()
}

function sortItems(key, order) {
	itemsSortKey = key
	itemsSortOrder = order
	displayFeed(currentFeed)
}

var feeds = new Map()
var currentFeed = null

var feedsSortKey = "Publisher"
var feedsSortOrder = "descending"
var itemsSortKey = "Name"
var itemsSortOrder = "descending"