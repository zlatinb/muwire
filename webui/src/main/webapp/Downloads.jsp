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

<% String pagetitle=Util._t("Downloads"); %>

<html>
    <head>
<%@include file="css.jsi"%>
	<script src="js/tables.js?<%=version%>" type="text/javascript"></script>
    <script src="js/download.js?<%=version%>" type="text/javascript"></script>
    </head>
    <body onload="initTranslate(jsTranslations); initConnectionsCount(); initDownloads();">
<%@include file="header.jsi"%>    	
	<aside>
<%@include file="searchbox.jsi"%>    	
            <div class="menubox">
                <h2><%=Util._t("Downloads")%></h2>
            </div>
<%@include file="sidebar.jsi"%>    	
	</aside>
	<section class="main foldermain">
		<div id="table-wrapper">
				<div id="downloads"></div>
		</div>		
		<hr/>
		<p><%=Util._t("Download Details")%></p>
		<div id="downloadDetails"><p><%=Util._t("Click on a download to view details")%></p></div>
	</section>
    </body>
</html>
