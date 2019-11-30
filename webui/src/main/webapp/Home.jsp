<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@ page import="com.muwire.core.search.*" %>
<%@ page import="net.i2p.data.Base64" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	MuWireClient client = (MuWireClient) session.getAttribute("mwClient");
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
    	
        <p>Welcome to MuWire ${persona}</p>
        <form action="/MuWire/Search" method="post">
        	<input type="text", name="search" />
        	<input type="submit", value="Search" />
      	</form>

		<hr/>
		<%
			SearchManager searchManager = (SearchManager) client.getServletContext().getAttribute("searchManager");
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
				Persona sender = new Persona(new ByteArrayInputStream(Base64.decode(senderB64)));
				
				Set<UIResultEvent> results = searchResults.getBySender().get(sender);
				
				StringBuilder sb = new StringBuilder();
				results.forEach(result -> {
					sb.append(result.getName()).append("  size:  ").append(result.getSize()).append("</br>");
				});
				out.print(sb.toString());
				
			}
		%>
		
      	
    </body>
</html>
