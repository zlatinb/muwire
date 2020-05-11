<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="com.muwire.webui.*" %>
<%@include file="initcode.jsi"%>

<% 

String pagetitle=Util._t("Feeds"); 
String helptext = Util._t("Every MuWire user can have a file feed to publish shared files of their choosing. " +
                          "You can subscribe to the feeds of other users. This is similar to following someone on a social network.") +
        "<br/>" + Util._t("On this page you can view the file feeds of users you are subscribed to. You can configure each feed " +
			  "separately with various options, and you can download the published files.");

%>

<html>
	<head>
<%@ include file="css.jsi"%>
<script src="js/certificates.js?<%=version%>" type="text/javascript"></script>
<script src="js/feeds.js?<%=version%>" type="text/javascript"></script>

	</head>
	<body>
<%@ include file="header.jsi"%>
	    <aside>
		<div class="menubox-divider"></div>
		<div class="menubox">
			<h2><%=Util._t("Enter a full MuWire id")%></h2>
			<form action="/MuWire/Feed" method="post">
				<input type="text" name="host">
				<input type="hidden" name="action" value="subscribe">
				<div class="menuitem feeds">
				  <div class="menu-icon"></div>
				  <input type="submit" value=<%=Util._t("Subscribe")%>>
				  <div class="tooltip"><%=Util._t("Help")%>
				  	<span class="tooltiptext">
				  		<%=Util._t("Enter the full ID of a MuWire user to see what files they have published to their feed")%>
				  	</span>
				  </div>
				</div>
			</form>
		</div>
		<div class="menubox-divider"></div>
<%@include file="sidebar.jsi"%>    	
	    </aside>
	    <section class="main foldermain">
		    <div id="table-wrapper">
				<div class="paddedTable" id="table-scroll">
					<div id="feedsTable"></div>
				</div>
			</div>
			<div id="feedConfig"></div>
			<hr/>
			<div id="table-wrapper">
				<div id="table-scroll">
					<div id="itemsTable"></div>
				</div>
			</div>
	    </section>
	</body>
</html>
