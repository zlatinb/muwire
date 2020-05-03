<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@include file="initcode.jsi"%>

<% 

String pagetitle= Util._t("Shared Files");
String helptext = Util._t("Shared files are those files you decide to share with other MuWire users.") +
        "<br/>" + Util._t("To share a file, enter its path in the box to the left.") +
        "<br/>" + Util._t("You can view your shared files as a tree or as a table.") +
        "<br/>" + Util._t("The \"Actions\" menu lets you perform various actions on the shared file.");

String viewAs = request.getParameter("viewAs");
if (viewAs == null)
	viewAs = "tree";

%>

<html>
	<head>
<%@ include file="css.jsi"%>
<script src="js/util.js?<%=version%>" type="text/javascript"></script>
<script src="js/tables.js?<%=version%>" type="text/javascript"></script>
<% if (viewAs.equals("tree")) { %>
	<script src="js/files.js?<%=version%>" type="text/javascript"></script>
<% } else { %>
	<script src="js/filesTable.js?<%=version%>" type="text/javascript"></script>
<% } %>

	</head>
	<body onload="initTranslate(jsTranslations); initConnectionsCount(); initFiles();">
<%@ include file="header.jsi"%>
	    <aside>
		<div class="menubox-divider"></div>
		<div class="menubox">
			<h2><%=Util._t("Share")%></h2>
			<form action="/MuWire/Files" method="post">
				<input type="text" name="file">
				<input type="hidden" name="action" value="share">
<% if (viewAs.equals("table")) { %>
				<input type="hidden" name="viewAs" value="table">
<% } %>
				<div class="menuitem shared">
				  <div class="menu-icon"></div>
				  <input type="submit" value="<%=Util._t("Share")%>">
				  <div class="tooltip"><%=Util._t("Help")%>
				  	<span class="tooltiptext"><%=Util._t("Enter the full path for the file or folder to be shared.")%>
				  		<br/>
				  		<%=Util._t("When sharing files on network drives (like a NAS) please visit {0}Advanced Sharing{1}", "<a href=\"/MuWire/AdvancedSharing\">", "</a>")%>
				  	</span>
				  </div>
				</div>
			</form>
<% if (viewAs.equals("tree")) { %>
			<a class="menuitem" href="SharedFiles?viewAs=table"><%=Util._t("View As Table")%></a>
<% } else { %>
			<a class="menuitem" href="SharedFiles?viewAs=tree"><%=Util._t("View As Tree")%></a>
<% } %>
		</div>
		<div class="menubox-divider"></div>
<%@include file="sidebar.jsi"%>    	
	    </aside>
	    <section class="main foldermain">
		<p><%=Util._t("Shared Files")%> <span id="count">0</span></p>
		<p><span id="hashing"></span></p>
		<hr/>
<% if (viewAs.equals("tree")) { %>
			<ul>
				<div id="root"></div>
			</ul>
<% } else { %>
			<div id="table-wrapper">
				<div class="paddedTable" id="table-scroll">
					<div id="filesTable"></div>
				</div>
			</div>
<% } %>
	    </section>
	</body>
</html>
