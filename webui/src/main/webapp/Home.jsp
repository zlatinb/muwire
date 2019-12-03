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
		<table width="100%">
			<tr>
				<th>
					Active Searches
				</th>
				<th>
					Results
				</th>
			</tr>
			<tr>
				<td>
					<div id="activeSearches"></div>
				</td>
				<td>
					<div id="results"></div>
				</td>
			</tr>
		</table>
		<script>
			var uuid = null;
			
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
			
			function updateUUID(resultUUID) {
				uuid = resultUUID;
				// TODO: update results table
			}
			
			function refreshActiveSearches() {
				var xmlhttp = new XMLHttpRequest();
				xmlhttp.onreadystatechange = function () {
					if (this.readyState == 4 && this.status == 200) {
						var xmlDoc = this.responseXML;
						var i;
						var table = "<table><tr><th>Search</th><th>Senders</th><th>Results</th></tr>";
						var activeSearchesDiv = document.getElementById("activeSearches");
						var x = xmlDoc.getElementsByTagName("Search");
						for (i = 0; i < x.length; i ++) {
							var resultUUID = x[i].getElementsByTagName("uuid")[0].childNodes[0].nodeValue;
							table += "<tr><td><a href='#' onclick='updateUUID(resultUUID);return false;'>"
							table += x[i].getElementsByTagName("Query")[0].childNodes[0].nodeValue;
							table += "</a></td><td>";
							table += x[i].getElementsByTagName("Senders")[0].childNodes[0].nodeValue;
							table += "</td><td>";
							table += x[i].getElementsByTagName("Results")[0].childNodes[0].nodeValue;
							table += "</td></tr>"
						}
						table += "</table>"
						if (x.length > 0)
							activeSearchesDiv.innerHTML = table;
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