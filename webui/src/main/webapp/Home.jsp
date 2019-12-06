<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%><%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@ page import="com.muwire.core.search.*" %>
<%@ page import="net.i2p.data.*" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@include file="initcode.jsi"%>
<%
        String pagetitle="Home";
	session.setAttribute("persona", persona);
	session.setAttribute("version", version);
	
	String groupBy = request.getParameter("groupBy");
	if (groupBy == null)
		groupBy = "sender";
%>
<html>
	<head>
<%@include file="css.jsi"%>
	<script src="js/search.js?<%=version%>" type="text/javascript"></script>
	</head>
<% if (groupBy.equals("sender")) { %>
		<body onload="initConnectionsCount();initGroupBySender();">
<% } else { %>
		<body onload="initConnectionsCount();initGroupByFile();">
<% } %>
<%@include file="header.jsi"%>
		<aside>
<% if (groupBy.equals("sender")) { %>
			<h3>Active Searches By Sender</h3>
			<a href="Home.jsp?groupBy=file">Group By File</a>
<% } else { %>
			<h3>Active Searches By File</h3>
			<a href="Home.jsp?groupBy=sender">Group By Sender</a>
<% } %>

									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="activeSearches"></div>
										</div>
									</div>
		</aside>
		<section class="main foldermain">
			<h3><span id="currentSearch">Results</span></h3>
									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="topTable"></div>
										</div>
									</div>
			<h3><span id="resultsFrom"></span></h3>
													<div id="table-wrapper">
														<div id="table-scroll">
															<div id="bottomTable">
															</div>
														</div>
													</div>
		</section>
	</body>
</html>
