<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@include file="initcode.jsi"%>

<% String pagetitle="Shared Files"; %>

<html>
	<head>
<%@ include file="css.jsi"%>
	<script src="js/files.js"?<%=version%>" type="text/javascript"></script>
	</head>
	<body onload="initConnectionsCount(); initFiles();">
<%@ include file="header.jsi"%>
	    <aside>
		<div class="menubox-divider"></div>
		<div class="menubox">
			<h2>Share</h2>
			<form action="/MuWire/Files" method="post">
				<input type="text" name="file">
				<input type="hidden" name="action" value="share">
				<input type="submit" value="Share">
			</form>
		</div>
		<div class="menubox-divider"></div>
<%@include file="sidebar.jsi"%>    	
	    </aside>
	    <section class="main foldermain">
		<p>Shared Files <span id="count">0</span></p>
		<p><span id="hashing"></span></p>
		<hr/>
			<ul>
				<div id="root"></div>
			</ul>
		<hr/>
	    </section>
	</body>
</html>
