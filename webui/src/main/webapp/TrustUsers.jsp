<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@include file="initcode.jsi"%>

<% 

String pagetitle=Util._t("Trust Users"); 

%>

<html>
	<head>
<%@ include file="css.jsi"%>
<script src="js/util.js?<%=version%>" type="text/javascript"></script>
<script src="js/trustUsers.js?<%=version%>" type="text/javascript"></script>

	</head>
	<body onload="initTranslate(jsTranslations); initConnectionsCount(); initTrustUsers();">
<%@ include file="header.jsi"%>
	    <aside>
<%@include file="sidebar.jsi"%>    	
	    </aside>
	    <section class="main foldermain">
		    <div id="table-wrapper">
				<div id="table-scroll">
					<div id="trustedUsers"></div>
				</div>
			</div>
			<hr/>
			<div id="refresh-link"></div>
			<div id="table-wrapper">
				<div id="table-scroll">
					<div id="distrustedUsers"></div>
				</div>
			</div>
		<hr/>
	    </section>
	</body>
</html>
