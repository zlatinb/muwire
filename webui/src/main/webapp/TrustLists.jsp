<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@include file="initcode.jsi"%>

<% 

String pagetitle=Util._t("Subscriptions"); 
String helptext = Util._t("This page shows the trust lists of users whom you have subscribed to.");
%>

<html>
	<head>
<%@ include file="css.jsi"%>
<script src="js/util.js?<%=version%>" type="text/javascript"></script>
<script src="js/tables.js?<%=version%>" type="text/javascript"></script>
<script src="js/trustLists.js?<%=version%>" type="text/javascript"></script>

	</head>
	<body onload="initTranslate(jsTranslations); initConnectionsCount(); initTrustLists();">
<%@ include file="header.jsi"%>
	    <aside>
<%@include file="sidebar.jsi"%>    	
	    </aside>
	    <section class="main foldermain">
		    <div id="table-wrapper">
				<div id="table-scroll" class="paddedTable">
					<div id="trustLists"></div>
				</div>
			</div>
			<hr/>
				<center><div id="currentList"></div></center>
						<div id="table-wrapper"><div id="table-scroll" class="paddedTable"><div id="trusted"></div></div></div>
						<div id="table-wrapper"><div id="table-scroll" class="paddedTable"><div id="distrusted"></div></div></div>
	    </section>
	</body>
</html>
