<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="com.muwire.webui.*" %>
<%@include file="initcode.jsi"%>

<% 

String pagetitle=Util._t("Advanced Sharing"); 
String helptext = Util._t("Use this page to configure advanced settings for each shared directory.");

%>

<html>
	<head>
<%@ include file="css.jsi"%>
<script nonce="<%=cspNonce%>" src="js/util.js?<%=version%>" type="text/javascript"></script>
<script nonce="<%=cspNonce%>" src="js/tables.js?<%=version%> type="text/javascript"></script>
<script nonce="<%=cspNonce%>" src="js/advancedSharing.js?<%=version%>" type="text/javascript"></script>
<script type="text/javascript">
  openAccordion = 2;
</script>
	</head>
	<body>
<%@ include file="header.jsi"%>
	    <aside>
		<div class="menubox-divider"></div>
<%@include file="sidebar.jsi"%>    	
	    </aside>
	    <section class="main foldermain">
	    	<p><%=Util._t("Shared directories can be watched automatically or periodically.  Automatic watching is recommended, but may not work on some NAS devices.")%></p>
		    <div id="table-wrapper">
				<div class="paddedTable" id="table-scroll">
					<div id="dirsTable"></div>
				</div>
			</div>
			<div id="dirConfig"></div>
	    </section>
	</body>
</html>
