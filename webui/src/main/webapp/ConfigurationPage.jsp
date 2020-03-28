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

String inboundLength = core.getI2pOptions().getProperty("inbound.length");
String inboundQuantity = core.getI2pOptions().getProperty("inbound.quantity");
String outboundLength = core.getI2pOptions().getProperty("outbound.length");
String outboundQuantity = core.getI2pOptions().getProperty("outbound.quantity");

Exception error = (Exception) application.getAttribute("MWConfigError");
%>

<html>
    <head>
<%@include file="css.jsi"%>
    </head>
    <body onload="initConnectionsCount();">
<%@include file="header.jsi"%>    	
	<aside>
<%@include file="searchbox.jsi"%>    	
<%@include file="sidebar.jsi"%>    	
	</aside>
	<section class="main foldermain">
<% if (error != null) { %>
<div class="warning"><%=error.getMessage()%></div>
<% } %>

		<form action="/MuWire/Configuration" method="post">
			<div class="configuration-section">
				<h3><%=Util._t("Search")%></h3>
				<table>
					<tr>
						<td>
							<div class="tooltip"><%=Util._t("Search in comments")%>
								<span class="tooltiptext"><%=Util._t("When searching the network, should MuWire search only file names or in the comments too?")%></span>
							</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getSearchComments()) out.write("checked"); %> name="searchComments" value="true"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Allow browsing")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getBrowseFiles()) out.write("checked"); %> name="browseFiles" value="true"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Downloads")%></h3>
				<table>
					<tr>
						<td><%=Util._t("Download retry frequency (seconds)")%></td>
						<td><p align="right"><input type="text" size="1" name="downloadRetryInterval" class="right" value="<%= core.getMuOptions().getDownloadRetryInterval()%>"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Directory for downloaded files")%></td>
						<td><p align="right"><input type="text" size="30" name="downloadLocation" value="<%= core.getMuOptions().getDownloadLocation().getAbsoluteFile()%>"></p></td>
					</tr>
					<tr> 
						<td><%=Util._t("Directory for incomplete files")%></td>
						<td><p align="right"><input type="text" size="30" name="incompleteLocation" value="<%= core.getMuOptions().getIncompleteLocation().getAbsoluteFile()%>"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Upload")%></h3>
				<table>
					<tr>
						<td><%=Util._t("Total upload slots (-1 means unlimited)")%></td>
						<td><p align="right"><input type="text" size="1" name="totalUploadSlots" class="right" value="<%= core.getMuOptions().getTotalUploadSlots() %>"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Upload slots per user (-1 means unlimited)")%></td>
						<td><p align="right"><input type="text" size="1" name="uploadSlotsPerUser" class="right" value="<%= core.getMuOptions().getUploadSlotsPerUser() %>"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Sharing")%></h3>
				<table>
					<tr>
						<td><%=Util._t("Share downloaded files")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getShareDownloadedFiles()) out.write("checked"); %> name="shareDownloadedFiles" value="true"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Share hidden files")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getShareHiddenFiles()) out.write("checked"); %> name="shareHiddenFiles" value="true"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Publishing")%></h3>
				<table>
					<tr>
						<td><%=Util._t("Enable my feed")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getFileFeed()) out.write("checked"); %> name="fileFeed" value="true"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Advertise my feed in search results")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getAdvertiseFeed()) out.write("checked"); %> name="advertiseFeed" value="true"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Publish shared files automatically")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getAutoPublishSharedFiles()) out.write("checked"); %> name="autoPublishSharedFiles" value="true"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Default settings for new feeds")%></h3>
				<table>
					<tr>
						<td><%=Util._t("Download published files automatically")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getDefaultFeedAutoDownload()) out.write("checked"); %> name="defaultFeedAutoDownload" value="true"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Download each file sequentially")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getDefaultFeedSequential()) out.write("checked"); %> name="defaultFeedAutoDownload" value="true"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Feed update frequency (minutes)")%></td>
						<td><p align="right"><input type="text" size="1" name="defaultFeedUpdateInterval" class="right" value="<%= core.getMuOptions().getDefaultFeedUpdateInterval() / 60000 %>"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Number of items to keep on disk (-1 means unlimited)")%></td>
						<td><p align="right"><input type="text" size="1" name="defaultFeedItemsToKeep" class="right" value="<%= core.getMuOptions().getDefaultFeedItemsToKeep() %>"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Trust")%></h3>
				<table>
					<tr>
						<td><%=Util._t("Allow only trusted connections")%></td>
						<td><p align="right"><input type="checkbox" <% if (!core.getMuOptions().getAllowUntrusted()) out.write("checked"); %> name="allowUntrusted" value="true"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Search extra hop")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getSearchExtraHop()) out.write("checked"); %> name="searchExtraHop" value="true"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Allow others to view my trust list")%></td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getAllowTrustLists()) out.write("checked"); %> name="allowTrustLists" value="true"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Trust list update frequency (hours)")%></td>
						<td><p align="right"><input type="text" size="1" name="trustListInterval" class="right" value="<%= core.getMuOptions().getTrustListInterval() %>"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("I2P Tunnels - Changes Require Plugin Restart")%></h3>
				<table>
					<tr>
						<td><%=Util._t("Inbound tunnel length")%></td>
						<td><p align="right"><input type="text" size="1" name="inbound.length" class="right" value="<%=inboundLength%>"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Inbound tunnel quantity")%></td>
						<td><p align="right"><input type="text" size="1" name="inbound.quantity" class="right" value="<%=inboundQuantity%>"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Outbound tunnel length")%></td>
						<td><p align="right"><input type="text" size="1" name="outbound.length" class="right" value="<%=outboundLength%>"></p></td>
					</tr>
					<tr>
						<td><%=Util._t("Outbound tunnel quantity")%></td>
						<td><p align="right"><input type="text" size="1" name="outbound.quantity" class="right" value="<%=outboundQuantity%>"></p></td>
					</tr>
				</table>
			</div>
			<input type="submit" value="<%=Util._t("Save")%>">
		</form>
	</section>
    </body>
</html>
