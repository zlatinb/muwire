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

<% String pagetitle="Downloads"; %>

<html>
    <head>
<%@include file="css.jsi"%>
    </head>
    <script src="js/download.js" type="text/javascript"></script>
    <body onload="initConnectionsCount(); initDownloads();">
<%@include file="header.jsi"%>    	
        <p>Downloads:</p>

		<div id="table-wrapper">
				<div id="downloads"></div>
			</div>
		</div>		
		</table>
		<hr/>
		<p>Download Details</p>
		<div id="downloadDetails"><p>Click on a download to view details</p></div>
    </body>
</html>
