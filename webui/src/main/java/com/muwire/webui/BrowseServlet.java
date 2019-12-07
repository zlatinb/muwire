package com.muwire.webui;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Persona;
import com.muwire.webui.BrowseManager.Browse;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class BrowseServlet extends HttpServlet {
    
    private BrowseManager browseManager;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String section = req.getParameter("section");
        if (section == null) {
            resp.sendError(403, "Bad section param");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        
        if (section.equals("status")) {
            sb.append("<Status>");
            browseManager.getBrowses().forEach( (persona, browse) -> {
                sb.append("<Browse>");
                sb.append("<Host>").append(Util.escapeHTMLinXML(persona.getHumanReadableName())).append("</Host>");
                sb.append("<HostB64>").append(persona.toBase64()).append("</HostB64>");
                sb.append("<BrowseStatus>").append(browse.getStatus()).append("</BrowseStatus>");
                sb.append("<TotalResults>").append(browse.getTotalResults()).append("</TotalResults>");
                sb.append("<ResultsCount>").append(browse.getResults().size()).append("</ResultsCount>");
                sb.append("</Browse>");
            });
            sb.append("</Status>");
        } else if (section.equals("results")) {
            String hostB64 = req.getParameter("host");
            if (hostB64 == null) {
                resp.sendError(403,"Bad param");
                return;
            }
            Persona host;
            try {
                host = new Persona(new ByteArrayInputStream(Base64.decode(hostB64)));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            Browse browse = browseManager.getBrowses().get(host);
            if (browse == null)
                return; // hmm
            
            sb.append("<Results>");
            browse.getResults().forEach(result -> {
                sb.append("<Result>");
                sb.append("<Name>").append(Util.escapeHTMLinXML(result.getName())).append("</Name>");
                sb.append("<Size>").append(DataHelper.formatSize2Decimal(result.getSize(), false)).append("B").append("</Size>");
                if (result.getComment() != null) {
                    sb.append("<Comment>").append(Util.escapeHTMLinXML(result.getComment())).append("</Comment>");
                }
                // TODO: add more fields
                sb.append("</Result>");
            });
            sb.append("</Results>");
        }
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(403,"Bad param");
            return;
        }
        if (action.equals("browse")) {
            String personaB64 = req.getParameter("host");
            if (personaB64 == null) {
                resp.sendError(403,"Bad param");
                return;
            }
            Persona host;
            try {
                host = new Persona(new ByteArrayInputStream(Base64.decode(personaB64)));
            } catch (Exception bad) {
                resp.sendError(403,"Bad param");
                return;
            }
            browseManager.browse(host);
            resp.sendRedirect("/MuWire/BrowseHost.jsp");
        } // TODO: implement canceling of browse
    }

    @Override
    public void init(ServletConfig cfg) throws ServletException {
        browseManager = (BrowseManager) cfg.getServletContext().getAttribute("browseManager");
    }

}
