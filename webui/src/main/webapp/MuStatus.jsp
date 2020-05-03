<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@include file="initcode.jsi"%>

<% 

String pagetitle=Util._t("MuWire Status");
String helptext = Util._t("This page shows internal MuWire metrics.");
 
Core core = (Core) application.getAttribute("core");
String buildNumber = (String)application.getAttribute("buildNumber");

%>

<html>
	<head>
<%@ include file="css.jsi"%>
<script src="js/status.js?<%=version%>" type="text/javascript"></script>
<script>
    openAccordion = 3;
</script>
	</head>
	<body onload="initConnectionsCount(); initStatus();">
<%@ include file="header.jsi"%>
	    <aside>
		<div class="menubox-divider"></div>
<%@include file="sidebar.jsi"%>    	
	    </aside>
	    <section class="main foldermain">
	    <h3>MuWire <%=core.getVersion() + "-b" + buildNumber%></h3>
	    <table>
	    	<tr>
	    		<td><%=Util._t("Incoming Connections")%></td>
	    		<td><span id="incoming-connections"></span></td>
	    	</tr>
	    	<tr>
	    		<td><%=Util._t("Outgoing Connections")%></td>
	    		<td><span id="outgoing-connections"></span></td>
	    	</tr>
	    	<tr>
	    		<td><%=Util._t("Known Hosts")%></td>
	    		<td><span id="known-hosts"></span></td>
	    	</tr>
	    	<tr>
	    		<td><%=Util._t("Failing Hosts")%></td>
	    		<td><span id="failing-hosts"></span></td>
	    	</tr>
	    	<tr>
	    		<td><%=Util._t("Hopeless Hosts")%></td>
	    		<td><span id="hopeless-hosts"></span></td>
	    	</tr>
	    	<tr>
	    		<td><%=Util._t("Times Browsed")%></td>
	    		<td><span id="times-browsed"></span></td>
	    	</tr>
	    </table>
	    </section>
	</body>
</html>
