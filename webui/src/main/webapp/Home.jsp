<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@ page import="com.muwire.core.search.*" %>
<%@ page import="net.i2p.data.*" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	MuWireClient client = (MuWireClient) application.getAttribute("mwClient");
	ConnectionCounter connectionCounter = (ConnectionCounter) client.getServletContext().getAttribute("connectionCounter");
	
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
        	Connections <%= connectionCounter.getConnections() %>
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
		<%
			SearchManager searchManager = (SearchManager) client.getServletContext().getAttribute("searchManager");
			DownloadManager downloadManager = (DownloadManager) client.getServletContext().getAttribute("downloadManager");
			if (request.getParameter("uuid") == null) {
				out.print("Active Searches<br/>");
				for (SearchResults results : searchManager.getResults().values()) {
					StringBuilder sb = new StringBuilder();
					sb.append(results.getSearch());
					sb.append("   senders:   ");
					Map<Persona, Set<UIResultEvent>> bySender = results.getBySender();
					sb.append(bySender.size());
					
					int total = 0;
					for (Set<UIResultEvent> s : bySender.values()) {
						total += s.size();
					}
					sb.append("  results:  ");
					sb.append(total);
					
					out.print("<a href='/MuWire/Home.jsp?uuid="+results.getUUID()+"'>"+sb.toString()+"</a><br/>");
				}
			} else if (request.getParameter("sender") == null) {
				UUID uuid = UUID.fromString(request.getParameter("uuid"));
				SearchResults results = searchManager.getResults().get(uuid);
				
				out.print("Results for "+results.getSearch()+"<br/>");
				
				Map<Persona, Set<UIResultEvent>> bySender = results.getBySender();
				for (Persona sender : bySender.keySet()) {
					StringBuilder sb = new StringBuilder();
					sb.append(sender.getHumanReadableName());
					sb.append("  count:   ");
					sb.append(bySender.get(sender).size());
					String link = "/MuWire/Home.jsp?uuid="+uuid.toString()+"&sender="+sender.toBase64();
					out.print("<a href='"+link+"'>"+sb.toString()+"</a><br/>");
				}
			} else {
				UUID uuid = UUID.fromString(request.getParameter("uuid"));
				SearchResults searchResults = searchManager.getResults().get(uuid);
				
				String senderB64 = request.getParameter("sender");
				Persona sender = new Persona(new ByteArrayInputStream(net.i2p.data.Base64.decode(senderB64)));
				
				Set<UIResultEvent> results = searchResults.getBySender().get(sender);
				
				StringBuilder sb = new StringBuilder();
				sb.append("<table width='100%'>");
				sb.append("<tr><td>Name</td><td>Size</td><td>Direct Sources</td><td>Possible Sources</td><td>Download</td></tr>");
				results.forEach(result -> {
					sb.append("<tr>");
					sb.append("<td>").append(result.getName()).append("</td>");
					sb.append("<td>").append(DataHelper.formatSize2Decimal(result.getSize(),false)).append("B").append("</td>");
					sb.append("<td>").append(searchResults.getByInfoHash(result.getInfohash()).size()).append("</td>");
					sb.append("<td>").append(searchResults.getPossibleSources(result.getInfohash()).size()).append("</td>");
					
					
					if (downloadManager.isDownloading(result.getInfohash())) {
						sb.append("<td>Downloading</td>");
					} else {
						sb.append("<td><form action='/MuWire/Download' method='post'><input type='hidden' name='infoHash' value='");
						sb.append(net.i2p.data.Base64.encode(result.getInfohash().getRoot()));
						sb.append("'/><input type='hidden' name='uuid' value='");
						sb.append(uuid.toString());
						sb.append("'/><input type='hidden' name='action' value='start'><input type='submit' value='Download'></form></td>");
					}
					sb.append("</tr>");
				});
				sb.append("</table>");
				out.print(sb.toString());
				
			}
		%>
		
      	
    </body>
</html>
