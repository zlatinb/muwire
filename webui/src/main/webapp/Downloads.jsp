<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.muwire.webui.*" %>
<%@ page import="com.muwire.core.*" %>
<%@ page import="com.muwire.core.search.*" %>
<%@ page import="net.i2p.data.*" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
	MuWireClient client = (MuWireClient) session.getAttribute("mwClient");
%>
<html>
    <head>
        <title>MuWire ${version}</title>
    </head>
    <body>
    	
        <p>Welcome to MuWire ${persona}</p>
        <p><a href="/MuWire/Home.jsp">Back to search</a></p>

		<hr/>
		<p>Downloads:</p>
		<%
			DownloadManager downloadManager = (DownloadManager) client.getServletContext().getAttribute("downloadManager");
			StringBuilder sb = new StringBuilder();
			sb.append("<table width='100%'>");
			downloadManager.getDownloaders().forEach(downloader -> {
			
				String name = downloader.getFile().getName();
				String state = downloader.getCurrentState().toString();
				int rawSpeed = downloader.speed();
				String speed = DataHelper.formatSize2Decimal(rawSpeed,false) + "B/sec";
				String ETA;
				if (rawSpeed == 0) 
					ETA = "Unknown";
				else {
					long remaining = (downloader.getNPieces() - downloader.donePieces()) * downloader.getPieceSize() / rawSpeed;
					ETA = DataHelper.formatDuration(remaining * 1000);
				}
				
				int percentDone = -1;
				if (downloader.getNPieces() != 0)
					percentDone = (int)(downloader.donePieces() * 100 / downloader.getNPieces());
				String totalSize = DataHelper.formatSize2Decimal(downloader.getLength(), false) + "B";
				String progress = String.format("%2d", percentDone) + "% of "+totalSize;
				

				sb.append("<tr>");
				sb.append("<td>").append(name).append("</td>");
				sb.append("<td>").append(state).append("</td>");
				sb.append("<td>").append(speed).append("</td>");
				sb.append("<td>").append(progress).append("</td>");
				sb.append("<td>").append(ETA).append("</td>");
				
				
				String form = "<form action='/MuWire/Download' method='post'><input type='hidden' name='infoHash' value='" + 
								net.i2p.data.Base64.encode(downloader.getInfoHash().getRoot()) +
								"'><input type='hidden' name='action' value='cancel'><input type='submit' value='Cancel'></form>";
				
				sb.append("<td>").append(form).append("</td>");
				sb.append("</tr>");				
			
			});
			sb.append("</table>");
			out.print(sb.toString());
		%>
		
      	
    </body>
</html>
