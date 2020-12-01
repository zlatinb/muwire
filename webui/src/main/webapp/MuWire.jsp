<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.File" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%
	String pagetitle = Util._t("Initial Setup");
	String helptext = Util._t("Set up your MuWire nickname and download locations now.");
%>

<html>
    <head>

<%@include file="nonce.jsi"%>
<title>MuWire ${version}</title>
<link href="i2pbote.css?${version}" rel="stylesheet" type="text/css">
<link href="muwire.css?${version}" rel="stylesheet" type="text/css">
<link rel="icon" type="image/png" href="images/muwire_logo.png" />
<script src="js/translate.js?${version}" type="text/javascript"></script>
<script src="js/util.js?${version}" type="text/javascript"></script>
<script nonce="<%=cspNonce%>" type="text/javascript">
  var jsTranslations = '<%=Util.getJSTranslations()%>';
</script>

    </head>
<%@include file="header.jsi"%>
    <body>
    	<%
    		String defaultDownloadLocation = System.getProperty("user.home")+File.separator+"Downloads";
    		String defaultIncompletesLocation = System.getProperty("user.home") + File.separator+"MuWire Incompletes";
    		String defaultDropBoxLocation = System.getProperty("user.home") + File.separator + "MuWire DropBox";
    		session.setAttribute("defaultDownloadLocation",defaultDownloadLocation);
    		session.setAttribute("defaultIncompletesLocation",defaultIncompletesLocation);
    		session.setAttribute("defaultDropBoxLocation",defaultDropBoxLocation);
    		
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
        <p><%=Util._t("These folders will be created if they do not already exist")%></p>
        <form action="/MuWire/init" method="post">
        <%=Util._t("Nickname")%>:<br/>
        <input type="text" name="nickname"><br>
        <%=Util._t("Folder for saving downloaded files")%>:<br/>
        <input type='text' name='download_location' value="${defaultDownloadLocation}"><br/>
        <%=Util._t("Folder for storing incomplete files")%>:<br/>
        <input type='text' name='incomplete_location' value="${defaultIncompletesLocation}"><br/>
        <%=Util._t("Drop Box for files you share with MuWire")%>:<br/>
        <input type='text' name='dropbox_location' value="${defaultDropBoxLocation}"><br/>
        <input type="submit" value="<%=Util._t("Submit")%>">
    </body>
</html>
