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
        String pagetitle=Util._t("Home");
        
        String helptext = Util._t("On this page you can search for files shared by other MuWire users");
        helptext += "<br/>" + Util._t("You can group the results by sender or by file");
        
	session.setAttribute("persona", persona);
	session.setAttribute("version", version);
	
	String groupBy = request.getParameter("groupBy");
	if (groupBy == null)
		groupBy = "sender";
	
	String topTableId, bottomTableId;
	if (groupBy.equals("sender")) {
		topTableId = "topTableSender";
		bottomTableId = "bottomTableSender";
	} else {
		topTableId = "topTableFile";
		bottomTableId = "bottomTableFile";
	}
%>
<html>
	<head>
<%@include file="css.jsi"%>
	<script src="js/tables.js?<%=version%>" type="text/javascript"></script>
	<script src="js/certificates.js?<%=version%>" type="text/javascript"></script>
	<script src="js/search.js?<%=version%>" type="text/javascript"></script>
<% if (request.getParameter("uuid") != null) {%>
	<script>
		uuid="<%=request.getParameter("uuid")%>"
	</script>
<% } %>
	</head>
<% if (groupBy.equals("sender")) { %>
		<body onload="initTranslate(jsTranslations); initConnectionsCount(); initGroupBySender(); initCertificates();">
<% } else { %>
		<body onload="initTranslate(jsTranslations); initConnectionsCount(); initGroupByFile(); initCertificates();">
<% } %>
<%@include file="header.jsi"%>
		<aside>
<%@include file="searchbox.jsi"%>    	
                    <div class="menubox">
<% if (groupBy.equals("sender")) { %>
			<h2><%=Util._t("Active Searches By Sender")%></h2>
			<a class="menuitem" href="Home?groupBy=file"><%=Util._t("Group By File")%></a>
<% } else { %>
			<h2><%=Util._t("Active Searches By File")%></h2>
			<a class="menuitem" href="Home?groupBy=sender"><%=Util._t("Group By Sender")%></a>
<% } %>
                    </div>

									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="activeSearches"></div>
										</div>
									</div>
<%@include file="sidebar.jsi"%>    	
		</aside>
		<section class="main foldermain">
			<h3><span id="currentSearch"><%=Util._t("Results")%></span></h3>
									<div id="table-wrapper">
										<div id="table-scroll">
											<div id="<%=topTableId%>"></div>
										</div>
									</div>
			<h3><span id="resultsFrom"></span></h3>
													<div id="table-wrapper">
														<div id="table-scroll">
															<div id="<%=bottomTableId%>">
															</div>
														</div>
													</div>
		</section>
	</body>
</html>
