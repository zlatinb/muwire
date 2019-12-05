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
	</head>
	<script src="js/search.js" type="text/javascript"></script>
<%@include file="header.jsi"%>
<% if (groupBy.equals("sender")) { %>
		<body onload="initConnectionsCount();initGroupBySender();">
		<center><a href="/MuWire/Home.jsp?groupBy=file">Group By File</a></center>
<% } else { %>
		<body onload="initConnectionsCount();initGroupByFile();">
		<center><a href="/MuWire/Home.jsp?groupBy=sender">Group By Sender</a></center>
<% } %>
		<table width="100%">
			<tr>
				<td width="20%">
						<table width="100%">
							<tr>
								<th>
									Active Searches
								</th>
							</tr>
							<tr>
								<td>
									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="activeSearches"></div>
										</div>
									</div>
								</td>
							</tr>
							<tr>
								<td>
									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="unused"></div>
										</div>
									</div>
								</td>
							</tr>
						</table>
				</td>
				<td width="80%">
						<table width="100%">
							<tr>
								<th>
									<span id="currentSearch">Results</span>
								</th>
							</tr>
							<tr>
								<td>
									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="topTable"></div>
										</div>
									</div>
								</td>
							</tr>
							<tr>
								<td>
									<table width="100%">
										<thead>
											<tr>
												<th>
													<span id="resultsFrom"></span>
												</th>
											</tr>
										</thead>
										<tbody>
											<tr>
												<td>
													<div id="table-wrapper">
														<div id="table-scroll">
															<div id="bottomTable">
														</div>
													</div>
												</td>
											</tr>
										</tbody>
									</table>
								</td>
							</tr>
						</table>
				</td>
			</tr>
		</table>
	</body>
</html>
