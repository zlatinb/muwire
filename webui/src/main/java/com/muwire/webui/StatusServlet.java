package com.muwire.webui;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;

public class StatusServlet extends HttpServlet {
    
    private Core core;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        
        int incoming = (int)core.getConnectionManager().getConnections().stream().filter(c -> c.isIncoming()).count();
        int outgoing = (int)core.getConnectionManager().getConnections().stream().filter(c -> !c.isIncoming()).count();
        int knownHosts = core.getHostCache().getHosts().size();
        int failingHosts = core.getHostCache().countFailingHosts();
        int hopelessHosts = core.getHostCache().countHopelessHosts();
        int timesBrowsed = core.getConnectionAcceptor().getBrowsed();
        

        sb.append("<Status>");
        sb.append("<IncomingConnections>").append(incoming).append("</IncomingConnections>");
        sb.append("<OutgoingConnections>").append(outgoing).append("</OutgoingConnections>");
        sb.append("<KnownHosts>").append(knownHosts).append("</KnownHosts>");
        sb.append("<FailingHosts>").append(failingHosts).append("</FailingHosts>");
        sb.append("<HopelessHosts>").append(hopelessHosts).append("</HopelessHosts>");
        sb.append("<TimesBrowsed>").append(timesBrowsed).append("</TimesBrowsed>");
        sb.append("</Status>");
        
        resp.setContentType("text/xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-store, max-age=0, no-cache, must-revalidate");
        byte[] out = sb.toString().getBytes("UTF-8");
        resp.setContentLength(out.length);
        resp.getOutputStream().write(out);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        core = (Core) config.getServletContext().getAttribute("core");
    }
}
