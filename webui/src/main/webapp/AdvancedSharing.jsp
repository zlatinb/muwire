<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="com.muwire.webui.*" %>
<%@include file="initcode.jsi"%>

<% 

String pagetitle=Util._t("Advanced Sharing"); 

%>

<html>
	<head>
<%@ include file="css.jsi"%>
<script src="js/util.js?<%=version%>" type="text/javascript"></script>
<script src="js/tables.js?<%=version%> type="text/javascript"></script>
<script src="js/advancedSharing.js?<%=version%>" type="text/javascript"></script>

	</head>
	<body onload="initTranslate(jsTranslations); initConnectionsCount(); initAdvancedSharing();">
<%@ include file="header.jsi"%>
	    <aside>
		<div class="menubox-divider"></div>
<%@include file="sidebar.jsi"%>    	
	    </aside>
	    <section class="main foldermain">
		    <div id="table-wrapper">
				<div class="paddedTable" id="table-scroll">
					<div id="dirsTable"></div>
				</div>
			</div>
			<div id="dirConfig"></div>
	    </section>
	</body>
</html>
