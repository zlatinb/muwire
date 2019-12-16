<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@include file="initcode.jsi"%>

<% 

String pagetitle=Util._t("Trust Lists"); 

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
				<div id="table-scroll">
					<div id="trustLists"></div>
				</div>
			</div>
			<hr/>
				<center><div id="currentList"></div></center>
				<table>
				<tbody>
					<tr>
						<td><div id="table-wrapper"><div id="table-scroll"><div id="trusted"></div></div></div></td>
						<td><div id="table-wrapper"><div id="table-scroll"><div id="distrusted"></div></div></div></td>
					</tr>
				</tbody>
				</table>
		<hr/>
	    </section>
	</body>
</html>
