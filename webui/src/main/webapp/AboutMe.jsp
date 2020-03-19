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
Core core = (Core) application.getAttribute("core");

%>

<html>
    <head>
<%@include file="css.jsi"%>
    </head>
    <body onload="initConnectionsCount();">
<%@include file="header.jsi"%>    	
	<aside>
<%@include file="searchbox.jsi"%>    	
<%@include file="sidebar.jsi"%>    	
	</aside>
	<section class="main foldermain">
		<p><%=Util._t("Your short MuWire id is {0}", core.getMe().getHumanReadableName())%></p>
		<p><%=Util._t("Your full MuWire id is")%></p>
		<p><pre class="fullId"><%=core.getMe().toBase64()%></pre></p>
	</section>
    </body>
</html>
