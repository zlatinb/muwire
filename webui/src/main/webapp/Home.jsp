<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.File" %>
<%@ page import="com.muwire.webui.MuWireClient" %>

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
    </body>
</html>
