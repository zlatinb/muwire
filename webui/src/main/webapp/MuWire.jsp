<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.File" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
	String pagetitle = Util._t("Initial Setup");
	String helptext = Util._t("On this page you can set up your MuWire nickname and download locations");
%>

<html>
    <head>
<%@include file="css.jsi"%>
    </head>
<%@include file="header.jsi"%>
    <body>
    	<%
    		String defaultDownloadLocation = System.getProperty("user.home")+File.separator+"Downloads";
    		String defaultIncompletesLocation = System.getProperty("user.home") + File.separator+"MuWire Incompletes";
    		session.setAttribute("defaultDownloadLocation",defaultDownloadLocation);
    		session.setAttribute("defaultIncompletesLocation",defaultIncompletesLocation);
    		
    		Throwable error = (Throwable) application.getAttribute("MWInitError");
    	%>
    	
        <noscript>
         <div class="warning">
          <center><b><%=Util._t("MuWire requires JavaScript. Please enable JavaScript in your browser.")%></b></center>
         </div>
        </noscript>
        
<% if (error != null) { %>
<div class="warning"><%=error.getMessage()%></div>
<% } %>
        
        <h3><%=Util._t("Welcome to MuWire!  Please select a nickname and download locations")%></h3>
        <p><%=Util._t("These directories will be created if they do not already exist")%></p>
        <form action="/MuWire/init" method="post">
        <%=Util._t("Nickname")%>:<br/>
        <input type="text" name="nickname"><br>
        <%=Util._t("Directory for saving downloaded files")%>:<br/>
        <input type='text' name='download_location' value="${defaultDownloadLocation}"><br/>
        <%=Util._t("Directory for storing incomplete files")%>:<br/>
        <input type='text' name='incomplete_location' value="${defaultIncompletesLocation}"><br/>
        <input type="submit" value="<%=Util._t("Submit")%>">
    </body>
</html>
