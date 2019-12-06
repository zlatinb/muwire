<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@include file="initcode.jsi"%>

<% String pagetitle="Files"; %>

<html>
	<head>
<%@ include file="css.jsi"%>
	<script src="js/files.js"?<%=version%>" type="text/javascript"></script>
	</head>
	<body onload="initConnectionsCount(); initFiles();">
<%@ include file="header.jsi"%>
		<p>Shared Files</p>
			<div id="sharedTree"></div>
		<hr/>
		<form action="/MuWire/Files" method="post">
			<input type="text" name="file">
			<input type="hidden" name="action" value="share">
			<input type="submit" value="Share">
		</form>
		<form action="/MuWire/Files" method="post">
			<input type="text" name="file">
			<input type="hidden" name="action" value="unshare">
			<input type="submit" value="Unshare">
		</form>
	</body>
</html>