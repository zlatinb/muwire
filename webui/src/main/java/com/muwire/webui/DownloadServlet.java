package com.muwire.webui;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.download.Downloader;
import com.muwire.core.download.UIDownloadCancelledEvent;
import com.muwire.core.download.UIDownloadEvent;
import com.muwire.core.search.UIResultEvent;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class DownloadServlet extends HttpServlet {

    private DownloadManager downloadManager;
    private SearchManager searchManager;
    private Core core;
    
    public void init(ServletConfig config) throws ServletException {
        downloadManager = (DownloadManager) config.getServletContext().getAttribute("downloadManager");
        searchManager = (SearchManager) config.getServletContext().getAttribute("searchManager");
        core = (Core) config.getServletContext().getAttribute("core");
    }
    
    
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (downloadManager == null) {
            resp.sendError(403, "Not initialized");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        sb.append("<Downloads>");
        downloadManager.getDownloaders().forEach(d -> {
            sb.append("<Download>");
            sb.append("<InfoHash>").append(Base64.encode(d.getInfoHash().getRoot())).append("</InfoHash>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(d.getFile().getName())).append("</Name>");
            sb.append("<State>").append(d.getCurrentState().toString()).append("</State>");
            int speed = d.speed();
            sb.append("<Speed>").append(DataHelper.formatSize2Decimal(speed)).append("B/sec").append("</Speed>");
            
            String ETA;
            if (speed == 0)
                ETA = "Unknown";
            else {
                long remaining = (d.getNPieces() - d.donePieces()) * d.getPieceSize() / speed;
                ETA = DataHelper.formatDuration(remaining * 1000);
            }
            sb.append("<ETA>").append(ETA).append("</ETA>");
            
            int percent = -1;
            if (d.getNPieces() != 0)
                percent = (int)(d.donePieces() * 100 / d.getNPieces());
            String totalSize = DataHelper.formatSize2Decimal(d.getLength(), false) + "B";
            String progress = String.format("%2d", percent) + "% of "+totalSize;
            sb.append("<Progress>").append(progress).append("</Progress>");
            
            // TODO: more details for the downloader details view
            sb.append("</Download>");
        });
        sb.append("</Downloads>");
        resp.setContentType("text/xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-store, max-age=0, no-cache, must-revalidate");
        byte[] out = sb.toString().getBytes("UTF-8");
        resp.setContentLength(out.length);
        resp.getOutputStream().write(out);
    }



    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String infoHashB64 = req.getParameter("infoHash");
        InfoHash infoHash = new InfoHash(Base64.decode(infoHashB64));
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(403, "Bad action param");
            return;
        }
        if (action.equals("start")) {
            if (core == null) {
                resp.sendError(403, "Not initialized");
                return;
            }
            UUID uuid = UUID.fromString(req.getParameter("uuid"));
            Set<UIResultEvent> results = searchManager.getResults().get(uuid).getByInfoHash(infoHash);
            
            UIDownloadEvent event = new UIDownloadEvent();
            UIResultEvent[] resultsArray = results.toArray(new UIResultEvent[0]);
            event.setResult(resultsArray);
            // TODO: sequential
            event.setSources(searchManager.getResults().get(uuid).getPossibleSources(infoHash));
            event.setTarget(new File(core.getMuOptions().getDownloadLocation(), resultsArray[0].getName()));
            core.getEventBus().publish(event);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        } else if (action.equals("cancel")) {
            if (downloadManager == null) {
                resp.sendError(403, "Not initialized");
                return;
            }
            downloadManager.cancel(infoHash);
        }
    }
}
