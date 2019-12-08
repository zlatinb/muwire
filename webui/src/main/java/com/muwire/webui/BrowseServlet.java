package com.muwire.webui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.Persona;
import com.muwire.core.download.UIDownloadEvent;
import com.muwire.core.search.UIResultEvent;
import com.muwire.webui.BrowseManager.Browse;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class BrowseServlet extends HttpServlet {
    
    private BrowseManager browseManager;
    private DownloadManager downloadManager;
    private Core core;

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
                sb.append("<Downloading>").append(downloadManager.isDownloading(result.getInfohash())).append("</Downloading>");
                sb.append("<Size>").append(DataHelper.formatSize2Decimal(result.getSize(), false)).append("B").append("</Size>");
                sb.append("<InfoHash>").append(Base64.encode(result.getInfohash().getRoot())).append("</InfoHash>");
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
        } else if (action.equals("download")) {
            if (core == null) {
                resp.sendError(403, "Not initialized");
                return;
            }
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
            String infoHashB64 = req.getParameter("infoHash");
            if (infoHashB64 == null) {
                resp.sendError(403, "Bad param");
                return;
            }
            final InfoHash infoHash;
            try {
                infoHash = new InfoHash(Base64.decode(infoHashB64));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            Browse browse = browseManager.getBrowses().get(host);
            if (browse == null)
                return;
            
            Set<UIResultEvent> results = browse.getResults().stream().filter(e -> e.getInfohash().equals(infoHash)).
                    collect(Collectors.toSet());
            
            if (results.isEmpty())
                return;
            
            UIDownloadEvent event = new UIDownloadEvent();
            UIResultEvent[] resultsArray = results.toArray(new UIResultEvent[0]);
            event.setResult(resultsArray);
            // TODO: sequential
            // TODO: possible sources
            event.setSources(Collections.emptySet());
            event.setTarget(new File(core.getMuOptions().getDownloadLocation(), resultsArray[0].getName()));
            core.getEventBus().publish(event);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignore) {}
        }
        // TODO: implement canceling of browse
    }

    @Override
    public void init(ServletConfig cfg) throws ServletException {
        browseManager = (BrowseManager) cfg.getServletContext().getAttribute("browseManager");
        downloadManager = (DownloadManager) cfg.getServletContext().getAttribute("downloadManager");
        core = (Core) cfg.getServletContext().getAttribute("core");
    }

}
