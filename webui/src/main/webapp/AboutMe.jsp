<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@include file="initcode.jsi"%>

<% 
String pagetitle=Util._t("About Me");
String helptext = Util._t("This page shows information about your MuWire identity.");

Core core = (Core) application.getAttribute("core");

%>

<html>
    <head>
<%@include file="css.jsi"%>
<script nonce="<%=cspNonce%>" src="js/util.js?<%=version%>" type="text/javascript"></script>
<script nonce="<%=cspNonce%>" src="js/sign.js?<%=version%>" type ="text/javascript"></script>
<script nonce="<%=cspNonce%>" type="text/javascript">
function copyFullId() {
	copyToClipboard("full-id")
	alert("Full ID copied to clipboard")
}
openAccordion = 3;
</script>
    </head>
    <body>
<%@include file="header.jsi"%>    	
	<aside>
<%@include file="searchbox.jsi"%>    	
<%@include file="sidebar.jsi"%>    	
	</aside>
	<section class="main foldermain">
		<h3><%=Util._t("MuWire ID")%></h3>
		<p><%=Util._t("Your short MuWire ID: {0}", core.getMe().getHumanReadableName())%></p>
		<p><%=Util._t("Your full MuWire ID:")%></p>
		<p><textarea class="fullId" id="full-id" readonly><%=core.getMe().toBase64()%></textarea></p>
		<p><a href='#' onclick="window.copyFullId();return false;"><%=Util._t("Copy to clipboard")%></a></p>
		
		<hr/>
		<h3><%=Util._t("Sign Tool")%></h3>
		<p><%=Util._t("Enter text to sign with your MuWire ID")%></p>
		<p><textarea class="sign" id="sign"></textarea></p>
		<p><a href='#' onclick="window.sign();return false;"><%=Util._t("Sign")%></a></p>
		<div id="signed"></div>
	</section>
    </body>
</html>
