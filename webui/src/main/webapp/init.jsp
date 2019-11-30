<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.File" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html
	<body>
		<jsp:useBean id="mwBean" class="com.muwire.webui.MuWireBean"/>
    	<c:set var="mwClient" scope="application" value="${mwBean.getMwClient()}"/>
    	<%
    		String nickname = request.getParameter("nickname");
    		String downloadLocation = request.getParameter("download_location");
    		String incompleteLocation = request.getParameter("incomplete_location");

			session.setAttribute("downloadLocation", new File(downloadLocation));
			session.setAttribute("incompleteLocation", new File(incompleteLocation));    		
			session.setAttribute("nickname",nickname);
    	%>
    	<c:set var="initResult" scope="session" value="${mwClient.initMWProps(nickname,downloadLocation,incompleteLocation)}"/>
    	<c:redirect url="/index.jsp"/>
	</body>
</html>
