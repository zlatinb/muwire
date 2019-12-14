<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@ page import="com.muwire.core.search.*" %>
<%@ page import="net.i2p.data.*" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@include file="initcode.jsi"%>

<% 
String pagetitle=Util._t("Configuration");
Core core = (Core) application.getAttribute("core");
%>

<html>
    <head>
<%@include file="css.jsi"%>
    </head>
    <body>
<%@include file="header.jsi"%>    	
	<aside>
<%@include file="searchbox.jsi"%>    	
<%@include file="sidebar.jsi"%>    	
	</aside>
	<section class="main foldermain">
		<form action="/MuWire/Configuration" method="post">
			<div class="configuration-section">
				<table>
					<tr>
						<td>Search in comments</td>
						<td><right><input type="checkbox" <% if (core.getMuOptions().getSearchComments()) out.write("checked"); %> name="searchComments" value="true"></right></td>
					</tr>
					<tr>
						<td>Allow browsing</td>
						<td><right><input type="checkbox" <% if (core.getMuOptions().getBrowseFiles()) out.write("checked"); %> name="browseFiles" value="true"></right></td>
					</tr>
				</table>
			</div>
			<input type="submit" value="<%=Util._t("Save")%>">
		</form>
	</section>
    </body>
</html>
