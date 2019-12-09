<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@include file="initcode.jsi"%>

<% 

String pagetitle="Trust Lists"; 

%>

<html>
	<head>
<%@ include file="css.jsi"%>
<script src="js/util.js?<%=version%>" type="text/javascript"></script>
<script src="js/trustLists.js?<%=version%>" type="text/javascript"></script>

	</head>
	<body onload="initConnectionsCount(); initTrustLists();">
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
			<div id="table-wrapper">
				<div id="table-scroll">
					<center><div id="currentList"></div></center>
					<thead><tr><th>Trusted</th><th>Distrusted></th></tr></thead>
					<tbody>
						<tr>
							<td><div id="trusted"></div></td>
							<td><div id="distrusted"></div></td>
						</tr>
					</tbody>
				</div>
			</div>
		<hr/>
	    </section>
	</body>
</html>
