<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@ page import="com.muwire.core.search.*" %>
<%@ page import="net.i2p.data.*" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@include file="initcode.jsi"%>

<% String pagetitle=Util._t("Uploads"); %>

<html>
    <head>
<%@include file="css.jsi"%>
	<script src="js/tables.js?<%=version%>" type="text/javascript"></script>
    <script src="js/upload.js?<%=version%>" type="text/javascript"></script>
    </head>
    <body onload="initTranslate(jsTranslations); initConnectionsCount(); initUploads();">
<%@include file="header.jsi"%>    	
	<aside>
<%@include file="searchbox.jsi"%>    	
<%@include file="sidebar.jsi"%>    	
	</aside>
	<section class="main foldermain">
		<div id="table-wrapper">
				<div id="uploads"></div>
				<center><span id="clearFinished"></span></center>
		</div>		
	</section>
    </body>
</html>
