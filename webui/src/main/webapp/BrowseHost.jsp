<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@ page import="java.io.*" %>
<%@ page import="net.i2p.data.Base64" %>
<%@include file="initcode.jsi"%>

<% 

String pagetitle=Util._t("Browse Host");
String helptext = Util._t("Use this page to browse the files shared by other MuWire users.");

String currentBrowse = null;
if (request.getParameter("currentHost") != null) {
	Persona host = new Persona(new ByteArrayInputStream(Base64.decode(request.getParameter("currentHost"))));
	currentBrowse = host.getHumanReadableName();
} 

%>

<html>
	<head>
<%@ include file="css.jsi"%>
<script src="js/certificates.js?<%=version%>" type="text/javascript"></script>
<script src="js/browse.js?<%=version%>" type="text/javascript"></script>

<% if (currentBrowse != null) { %>
	<script nonce="<%=cspNonce%>" type="text/javascript">
		currentHost="<%=currentBrowse%>"
	</script>
<% } %>

	</head>
	<body>
<%@ include file="header.jsi"%>
	    <aside>
		<div class="menubox-divider"></div>
		<div class="menubox">
			<h2><%=Util._t("Enter a full MuWire ID")%></h2>
			<form action="/MuWire/Browse" method="post">
				<input type="text" name="host">
				<input type="hidden" name="action" value="browse">
				<div class="menuitem browse">
				  <div class="menu-icon"></div>
				  <input type="submit" value=<%=Util._t("Browse")%>>
				  <div class="tooltip"><%=Util._t("Help")%>
				  	<span class="tooltiptext">
				  		<%=Util._t("Enter the full ID of a MuWire user to see what files they are sharing")%>
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
				<div id="table-scroll">
					<div id="activeBrowses"></div>
				</div>
			</div>
			<hr/>
			<div id="table-wrapper">
				<div id="table-scroll">
					<div id="resultsTable"></div>
				</div>
			</div>
	    </section>
	</body>
</html>
