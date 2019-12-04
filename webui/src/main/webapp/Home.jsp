<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%><%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@ page import="com.muwire.core.search.*" %>
<%@ page import="net.i2p.data.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
	MuWireClient client = (MuWireClient) application.getAttribute("mwClient");
	String persona = client.getCore().getMe().getHumanReadableName();
	String version = client.getCore().getVersion();
	session.setAttribute("persona", persona);
	session.setAttribute("version", version);
%>
<html>
	<head>
		<title>MuWire ${version}</title>
	</head>
	<body>
    	<table width="100%">
    	<tr>
    	<td>
        	Welcome to MuWire ${persona}
        </td>
        <td>
        	<span id="connectionsCount">Connections : 0</span>
        </td>
        </tr>
        </table>
        
        <table width="100%">
        <tr>
        <td>
        <form action="/MuWire/Search" method="post">
        	<input type="text", name="search" />
        	<input type="submit", value="Search" />
      	</form>
		</td>
		<td>
			<a href="/MuWire/Downloads.jsp">Downloads</a>
		</td>
		</tr>
		</table>
		<hr/>
		<style>
				#table-wrapper {
				  position:relative;
				}
				#table-scroll {
				  height:150px;
				  overflow:auto;  
				  margin-top:20px;
				}
				#table-wrapper table {
				  width:100%;
				
				}
				#table-wrapper table * {
				  background:yellow;
				  color:black;
				}
				#table-wrapper table thead th .text {
				  position:absolute;   
				  top:-20px;
				  z-index:2;
				  height:20px;
				  width:35%;
				  border:1px solid red;
				}
		</style>
		<table width="100%">
			<tr>
				<td width="20%">
						<table width="100%">
							<tr>
								<th>
									Active Searches
								</th>
							</tr>
							<tr>
								<td>
									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="activeSearches"></div>
										</div>
									</div>
								</td>
							</tr>
							<tr>
								<td>
									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="unused"></div>
										</div>
									</div>
								</td>
							</tr>
						</table>
				</td>
				<td width="80%">
						<table width="100%">
							<tr>
								<th>
									<span id="currentSearch">Results</span>
								</th>
							</tr>
							<tr>
								<td>
									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="senders"></div>
										</div>
									</div>
								</td>
							</tr>
							<tr>
								<td>
									<table width="100%">
										<thead>
											<tr>
												<th>
													<span id="resultsFrom"></span>
												</th>
											</tr>
										</thead>
										<tbody>
											<tr>
												<td>
													<div id="table-wrapper">
														<div id="table-scroll">
															<div id="results">
														</div>
													</div>
												</td>
											</tr>
										</tbody>
									</table>
								</td>
							</tr>
						</table>
				</td>
			</tr>
		</table>
		
		<script>
		
			class Search {
				constructor(xmlNode) {
					this.uuid = xmlNode.getElementsByTagName("uuid")[0].childNodes[0].nodeValue;
					this.query = xmlNode.getElementsByTagName("Query")[0].childNodes[0].nodeValue;
					this.resultBatches = new Map();
					
					var resultsBySender = xmlNode.getElementsByTagName("ResultsBySender")[0];
					var resultsFromSenders = resultsBySender.getElementsByTagName("ResultsFromSender");
					var i;
					for (i = 0; i < resultsFromSenders.length; i++) {
						var results = new Results(resultsFromSenders[i]);
						this.resultBatches.set(results.sender, results);
					}
				}
			}
			
			class Results {
				constructor(xmlNode) {
					this.sender = xmlNode.getElementsByTagName("Sender")[0].childNodes[0].nodeValue;
					this.results = [];
					var resultNodes = xmlNode.getElementsByTagName("Result");
					var i;
					for (i = 0 ; i < resultNodes.length; i ++) {
						var result = new Result(resultNodes[i]);
						this.results.push(result);
					}
				}
			}
			
			class Result {
				constructor(xmlNode) {
					this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue;
					this.size = xmlNode.getElementsByTagName("Size")[0].childNodes[0].nodeValue;
					this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue;
				}
			}
		
			var searches = new Map();
		
			var uuid = null;
			var sender = null;
			var lastXML = null;
			
			function refreshConnectionsCount() {
				var xmlhttp = new XMLHttpRequest();
				xmlhttp.onreadystatechange = function() {
					if (this.readyState == 4 && this.status == 200) {
						var connections = this.responseXML.getElementsByTagName("Connections");
						var count = connections[0].childNodes[0].nodeValue
						var connectionCountSpan = document.getElementById("connectionsCount");
						var countString = "Connections: "+count;
						connectionCountSpan.innerHTML = countString;
					}
				}
				xmlhttp.open("GET", "/MuWire/Search?section=connectionsCount", true);
				xmlhttp.send();
			}
			
			function updateSender(senderName) {
				sender = senderName;
				
				var resultsFromSpan = document.getElementById("resultsFrom");
				resultsFromSpan.innerHTML = "Results From "+sender;
				
				var resultsDiv = document.getElementById("results");
				var table = "<table><thead><tr><th>Name</th><th>Size</th></tr></thead><tbody>"
				var x = searches.get(uuid)
				x = x.resultBatches.get(sender).results;
				var i;
				for (i = 0; i < x.length; i++) {
					table += "<tr>";
					table += "<td>";
					table += x[i].name;
					table += "</td>";
					table += "<td>";
					table += x[i].size;
					table += "</td>";
					table += "</tr>";
				}
				table += "</tbody></table>";
				if (x.length > 0)
					resultsDiv.innerHTML = table
			}
			
			function updateUUID(resultUUID) {
				uuid = resultUUID;
				
				var currentSearchSpan = document.getElementById("currentSearch");
				currentSearchSpan.innerHTML = searches.get(uuid).query + " Results";
				
				var sendersDiv = document.getElementById("senders");
				var table = "<table><thead><tr><th>Sender</th></thead></tr><tbody>";
				var x = searches.get(uuid).resultBatches;
				for (var [senderName, ignored] of x) {
					table += "<tr><td><a href='#' onclick='updateSender(\""+senderName+"\");return false;'>"
					table += senderName;
					table += "</a></td></tr>";
				}
				table += "</tbody></table>";
				if (x.size > 0)
					sendersDiv.innerHTML = table;
				if (sender != null)
					updateSender(sender);
			}
			
			function refreshActiveSearches() {
				var xmlhttp = new XMLHttpRequest();
				xmlhttp.onreadystatechange = function () {
					if (this.readyState == 4 && this.status == 200) {
						var xmlDoc = this.responseXML;
						lastXML = xmlDoc;
						searches.clear();
						var i;
						var x = xmlDoc.getElementsByTagName("Search");
						for (i = 0; i < x.length; i++) {
							var search = new Search(x[i]);
							searches.set(search.uuid, search);
						}
						
						var table = "<table><thead><tr><th>Search</th><th>Senders</th><th>Results</th></tr></thead><tbody>";
						var activeSearchesDiv = document.getElementById("activeSearches");
						for (var [resultsUUID, search] of searches) {
							table += "<tr><td><a href='#' onclick='updateUUID(\"" + resultsUUID+ "\");return false;'>"
							table += search.query;
							table += "</a></td>";
							table += "<td>"
							table += search.resultBatches.size;
							table += "</td>";
							var totalResults = 0;
							search.resultBatches.forEach(resultBatch => totalResults+=resultBatch.results.length);
							table += "<td>";
							table += totalResults;
							table += "</td>"
							table += "</tr>"
						}
						table += "</tbody></table>"
						if (x.length > 0) 
							activeSearchesDiv.innerHTML = table;
						if (uuid != null)
							updateUUID(uuid);
						
					}
				}
				xmlhttp.open("GET", "/MuWire/Search?section=activeSearches", true);
				xmlhttp.send();
			}
			
			setInterval(refreshActiveSearches, 3000);
			setTimeout(refreshActiveSearches, 1);
			setInterval(refreshConnectionsCount, 3000);
			setTimeout(refreshConnectionsCount, 1);
		</script>
	</body>
</html>