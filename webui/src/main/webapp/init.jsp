<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core" %>
<%@ page import="java.io.File" %>
<%@ page import="com.muwire.webui.MuWireClient" %>

<html>
	<body>
    	<%
    		String nickname = request.getParameter("nickname");
    		String downloadLocation = request.getParameter("download_location");
    		String incompleteLocation = request.getParameter("incomplete_location");

			MuWireClient client = (MuWireClient) session.getAttribute("mwClient");
			client.initMWProps(nickname, new File(downloadLocation), new File(incompleteLocation));
			client.start();
    	%>
    	<c:redirect url="/index.jsp"/>
	</body>
</html>
