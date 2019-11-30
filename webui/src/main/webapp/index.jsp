<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.File" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
    <head>
        <title>MuWire</title>
    </head>
    <body>
    	<jsp:useBean id="mwBean" class="com.muwire.webui.MuWireBean"/>
    	<c:set var="mwClient" scope="application" value="${mwBean.getMwClient()}"/>
    	
    	<%
    		String defaultDownloadLocation = System.getProperty("user.home")+File.separator+"Downloads";
    		String defaultIncompletesLocation = System.getProperty("user.home") + File.separator+"MuWire Incompletes";
    		session.setAttribute("defaultDownloadLocation",defaultDownloadLocation);
    		session.setAttribute("defaultIncompletesLocation",defaultIncompletesLocation);
    	%>
    	
    	<c:if test = "${mwClient.needsMWInit()}">
	        <p>Welcome to MuWire!  Please select a nickname and download locations</p>
	        <form action="/MuWire/init.jsp" method="post">
	        Nickname:
	        <input type="text" name="nickname"><br>
	        Directory for saving downloaded files:
	        <input type='text' name='download_location' value="${defaultDownloadLocation}"><br/>
	        Directory for storing incomplete files:
	        <input type='text' name='incomplete_location' value="${defaultIncompletesLocation}"><br/>
	        <input type="submit" value="Submit">
	    </c:if>
	    <c:if test = "${!mwClient.needsMWInit()}">
	    	<p>MW doesn't need initing</p>
	  	</c:if>
    </body>
</html>
