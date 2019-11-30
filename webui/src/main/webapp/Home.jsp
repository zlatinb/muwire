<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.File" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>

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
		Active Searches:<br/>
		<%
			SearchManager searchManager = (SearchManager) client.getServletContext().getAttribute("searchManager");
			for (String search : searchManager.getSearches()) {
				out.print(search);
				out.print("<br/>");
			}
		%>
		
      	
    </body>
</html>
