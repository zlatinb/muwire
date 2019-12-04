<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.File" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<html>
    <head>
<%@include file="css.jsi"%>
    </head>
    <body>
    	<%
    		String defaultDownloadLocation = System.getProperty("user.home")+File.separator+"Downloads";
    		String defaultIncompletesLocation = System.getProperty("user.home") + File.separator+"MuWire Incompletes";
    		session.setAttribute("defaultDownloadLocation",defaultDownloadLocation);
    		session.setAttribute("defaultIncompletesLocation",defaultIncompletesLocation);
    	%>
    	
        <p>Welcome to MuWire!  Please select a nickname and download locations</p>
        <form action="/MuWire/init.jsp" method="post">
        Nickname:
        <input type="text" name="nickname"><br>
        Directory for saving downloaded files:
        <input type='text' name='download_location' value="${defaultDownloadLocation}"><br/>
        Directory for storing incomplete files:
        <input type='text' name='incomplete_location' value="${defaultIncompletesLocation}"><br/>
        <input type="submit" value="Submit">
    </body>
</html>
