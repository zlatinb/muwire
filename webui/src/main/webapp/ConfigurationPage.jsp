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
String helptext = Util._t("Use this page to change MuWire options.");

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
<script nonce="<%=cspNonce%>" type="text/javascript">
  openAccordion = 2;
</script>
    </head>
    <body>
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
						<td>
							<div class="tooltip"><%=Util._t("Allow browsing")%>
								<span class="tooltiptext"><%=Util._t("Allow other users to browse your shared files")%></span>
							</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getBrowseFiles()) out.write("checked"); %> name="browseFiles" value="true"></p></td>
					</tr>
					<tr>
						<td>
							<div class="tooltip"><%=Util._t("Allow tracking")%>
								<span class="tooltiptext"><%=Util._t("Allow trackers to track your shared files")%></span>
							</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getAllowTracking()) out.write("checked"); %> name="allowTracking" value="true"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Downloads")%></h3>
				<table>
					<tr>
						<td><div class="tooltip"><%=Util._t("Download retry frequency (seconds)")%>
							<span class="tooltiptext"><%=Util._t("MuWire retries failed downloads automatically.  This value controls how often to retry.")%></span>
							</div>
						</td>
						<td><p align="right"><input type="text" size="1" name="downloadRetryInterval" class="right" value="<%= core.getMuOptions().getDownloadRetryInterval()%>"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Give up on sources after this many failures (-1 means never)")%>
							<span class="tooltiptext"><%=Util._t("After how many download attempts MuWire should give up on the download source.")%></span>
							</div>
						</td>
						<td><p align="right"><input type="text" size="1" name="downloadMaxFailures" class="right" value="<%= core.getMuOptions().getDownloadMaxFailures()%>"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Directory for downloaded files")%>
							<span class="tooltiptext"><%=Util._t("Where to save downloaded files. MuWire must be able to write to this location.")%></span>
							</div>
						</td>
						<td><p align="right"><input type="text" size="30" name="downloadLocation" value="<%= core.getMuOptions().getDownloadLocation().getAbsoluteFile()%>"></p></td>
					</tr>
					<tr> 
						<td><div class="tooltip"><%=Util._t("Directory for incomplete files")%>
							<span class="tooltiptext"><%=Util._t("Where to store partial data of files which are currently being downloaded.")%></span>
							</div>
						</td>
						<td><p align="right"><input type="text" size="30" name="incompleteLocation" value="<%= core.getMuOptions().getIncompleteLocation().getAbsoluteFile()%>"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Upload")%></h3>
				<table>
					<tr>
						<td><div class="tooltip"><%=Util._t("Total upload slots (-1 means unlimited)")%>
							<span class="tooltiptext"><%=Util._t("Maximum files to upload at once")%></span>
							</div>
						</td>
						<td><p align="right"><input type="text" size="1" name="totalUploadSlots" class="right" value="<%= core.getMuOptions().getTotalUploadSlots() %>"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Upload slots per user (-1 means unlimited)")%>
							<span class="tooltiptext"><%=Util._t("Maximum files to upload to a single user at once")%></span>
							</div>
						</td>
						<td><p align="right"><input type="text" size="1" name="uploadSlotsPerUser" class="right" value="<%= core.getMuOptions().getUploadSlotsPerUser() %>"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Sharing")%></h3>
				<table>
					<tr>
						<td><div class="tooltip"><%=Util._t("Share downloaded files")%>
							<span class="tooltiptext"><%=Util._t("Automatically share files you have downloaded with MuWire")%></span>
							</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getShareDownloadedFiles()) out.write("checked"); %> name="shareDownloadedFiles" value="true"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Share hidden files")%>
							<span class="tooltiptext"><%=Util._t("Share files marked as hidden by the operating system")%></span>
							</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getShareHiddenFiles()) out.write("checked"); %> name="shareHiddenFiles" value="true"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Publishing")%></h3>
				<table>
					<tr>
						<td><div class="tooltip"><%=Util._t("Enable my feed")%>
							<span class="tooltiptext"><%=Util._t("Enable your personal file feed")%></span></div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getFileFeed()) out.write("checked"); %> name="fileFeed" value="true"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Advertise my feed in search results")%>
							<span class="tooltiptext"><%=Util._t("Allow other users to find your personal file feed")%></span>
						</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getAdvertiseFeed()) out.write("checked"); %> name="advertiseFeed" value="true"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Publish shared files automatically")%>
							<span class="tooltiptext"><%=Util._t("All files you share in the future will be published to your feed automatically.")%></span>
						</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getAutoPublishSharedFiles()) out.write("checked"); %> name="autoPublishSharedFiles" value="true"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Default settings for new feeds")%></h3>
				<table>
					<tr>
						<td><div class="tooltip"><%=Util._t("Download published files automatically")%>
							<span class="tooltiptext"><%=Util._t("Download every file published to the given feed")%></span>
						</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getDefaultFeedAutoDownload()) out.write("checked"); %> name="defaultFeedAutoDownload" value="true"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Download each file sequentially")%>
							<span class="tooltiptext"><%=Util._t("Download files from this feed sequentially. This helps with previewing media files, but may reduce availability of the file for others.")%></span>
						</div>
						</td>
						
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getDefaultFeedSequential()) out.write("checked"); %> name="defaultFeedAutoDownload" value="true"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Feed update frequency (minutes)")%>
							<span class="tooltiptext"><%=Util._t("How often to check for updates to this feed")%></span>
						</div>
						</td>
						<td><p align="right"><input type="text" size="1" name="defaultFeedUpdateInterval" class="right" value="<%= core.getMuOptions().getDefaultFeedUpdateInterval() / 60000 %>"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Number of items to keep on disk (-1 means unlimited)")%>
							<span class="tooltiptext"><%=Util._t("Only remember this many published items across restarts, unless you set the value to -1")%></span>
						</div>
						</td>
						<td><p align="right"><input type="text" size="1" name="defaultFeedItemsToKeep" class="right" value="<%= core.getMuOptions().getDefaultFeedItemsToKeep() %>"></p></td>
					</tr>
				</table>
			</div>
			<div class="configuration-section">
				<h3><%=Util._t("Trust")%></h3>
				<table>
					<tr>
						<td><div class="tooltip"><%=Util._t("Allow only trusted connections")%>
							<span class="tooltiptext"><%=Util._t("Only connect to users you have marked as Trusted")%></span>
						</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (!core.getMuOptions().getAllowUntrusted()) out.write("checked"); %> name="allowUntrusted" value="true"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Search extra hop")%>
							<span class="tooltiptext"><%=Util._t("If only trusted connections are allowed, search only users that are directly connected to you. Use this setting to enable searches of additional users. It has no effect if untrusted connections are allowed.")%></span>
						</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getSearchExtraHop()) out.write("checked"); %> name="searchExtraHop" value="true"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Allow others to view my trust list")%>
							<span class="tooltiptext"><%=Util._t("Allow other MuWire users to see who you have marked as Trusted and Distrusted")%></span>
						</div>
						</td>
						<td><p align="right"><input type="checkbox" <% if (core.getMuOptions().getAllowTrustLists()) out.write("checked"); %> name="allowTrustLists" value="true"></p></td>
					</tr>
					<tr>
						<td><div class="tooltip"><%=Util._t("Trust list update frequency (hours)")%>
							<span class="tooltiptext"><%=Util._t("How often to check for updates to the trust lists you are subscribed to.")%></span>
						</div>
						</td>
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
