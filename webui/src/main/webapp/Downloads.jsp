<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@ page import="com.muwire.core.search.*" %>
<%@ page import="net.i2p.data.*" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@include file="initcode.jsi"%>

<% String pagetitle="Downloads"; %>

<html>
    <head>
<%@include file="css.jsi"%>
    </head>
    <body onload="initConnectionsCount()">
<%@include file="header.jsi"%>    	
        <p>Downloads:</p>

		<div id="table-wrapper">
				<div id="downloads"></div>
			</div>
		</div>		
		</table>
		<hr/>
		<p>Download Details</p>
		<div id="downloadDetails"><p>Click on a download to view details</p></div>
		
		<script>
		
			class Downloader {
				constructor(xmlNode) {
					this.name = xmlNode.getElementsByTagName("Name")[0].childNodes[0].nodeValue;
					this.state = xmlNode.getElementsByTagName("State")[0].childNodes[0].nodeValue;
					this.speed = xmlNode.getElementsByTagName("Speed")[0].childNodes[0].nodeValue;
					this.ETA = xmlNode.getElementsByTagName("ETA")[0].childNodes[0].nodeValue;
					this.progress = xmlNode.getElementsByTagName("Progress")[0].childNodes[0].nodeValue;
					this.infoHash = xmlNode.getElementsByTagName("InfoHash")[0].childNodes[0].nodeValue;
				}
			}
			
			var downloader = null;
			var downloaders = new Map()
			
			function updateDownloader(infoHash) {
				var selected = downloaders.get(infoHash);
				
				var downloadDetailsDiv = document.getElementById("downloadDetails");
				downloadDetailsDiv.innerHTML = "<p>Details for "+selected.name+"</p>"
			}
			
			function refreshDownloader() {
				var xmlhttp = new XMLHttpRequest();
				xmlhttp.onreadystatechange = function() {
					if (this.readyState == 4 && this.status == 200) {
						var xmlDoc = this.responseXML;
						downloaders.clear();
						var i;
						var x = xmlDoc.getElementsByTagName("Download");
						for (i = 0; i < x.length; i ++) {
							var download = new Downloader(x[i]);
							downloaders.set(download.infoHash, download);
						}
						
						var table = "<table><thead><tr><th>Name</th><th>State</th><th>Speed</th><th>ETA</th><th>Progress</th><th>Cancel</th></tr></thead></tbody>";
						var downloadsDiv = document.getElementById("downloads");
						for (var [infoHash, download] of downloaders) {
							table += "<tr><td><a href='#' onclick='updateDownloader(\""+infoHash+"\");return false;'>";
							table += download.name;
							table += "</a></td>";
							table += "<td>"+download.state+"</td>";
							table += "<td>"+download.speed+"</td>";
							table += "<td>"+download.ETA+"</td>";
							table += "<td>"+download.progress+"</td>";
							
							table += "<td><form action='/MuWire/Download' method='post'><input type='hidden' name='infoHash' value='" +
									infoHash + "'><input type='hidden' name='action' value='cancel'><input type='submit' value='Cancel'></form></td>";
							
							table += "</tr>";
						}
						table += "</tbody></table>";
						if (downloaders.size > 0)
							downloadsDiv.innerHTML = table;
						if (downloader != null)
							updateDownloader(downloader);
					}
				}
				xmlhttp.open("GET", "/MuWire/Download", true);
				xmlhttp.send();
			}
			
			setInterval(refreshDownloader, 3000)
			setTimeout(refreshDownloader,1);
		</script>
    </body>
</html>
