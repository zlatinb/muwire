package com.muwire.webui;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.Persona;

import net.i2p.data.DataHelper;

public class UploadServlet extends HttpServlet {
    
    private UploadManager uploadManager;
    private BrowseManager browseManager;
    private Core core;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        List<UploadEntry> entries = new ArrayList<>();
        synchronized(uploadManager.getUploads()) {
            for(UploadManager.UploaderWrapper wrapper : uploadManager.getUploads()) {
                Persona downloader = wrapper.getUploader().getDownloaderPersona();
                UploadEntry entry = new UploadEntry(
                        wrapper.getUploader().getName(),
                        wrapper.getUploader().getProgress(),
                        wrapper.getUploader().getDownloader(),
                        wrapper.getUploader().getDownloaderPersona().toBase64(),
                        wrapper.getUploader().getDonePieces(),
                        wrapper.getUploader().getTotalPieces(),
                        wrapper.getUploader().speed(),
                        wrapper.getUploader().isBrowseEnabled(),
                        wrapper.getUploader().isFeedEnabled(),
                        browseManager.isBrowsing(downloader),
                        core.getFeedManager().getFeed(downloader) != null
                        );
                entries.add(entry);
            }
        }
        COMPARATORS.sort(entries, req);
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        sb.append("<Uploads>");
        entries.forEach(e -> e.toXML(sb));
        sb.append("</Uploads>");
        
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
        uploadManager = (UploadManager) config.getServletContext().getAttribute("uploadManager");
        browseManager = (BrowseManager) config.getServletContext().getAttribute("browseManager");
        core = (Core) config.getServletContext().getAttribute("core");
    }
    
    
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action.equals("clear")) {
            uploadManager.clearFinished();
        }
    }



    private static class UploadEntry {
        private final String name;
        private final int progress;
        private final String downloader, b64;
        private final int remotePieces;
        private final int totalPieces;
        private final int speed;
        private final boolean browse, feed;
        private final boolean browsing, subscribed;
        
        UploadEntry(String name, int progress, String downloader, String b64, int remotePieces, int totalPieces, int speed,
                boolean browse, boolean feed, boolean browsing, boolean subscribed) {
            this.name = name;
            this.progress = progress;
            this.downloader = downloader;
            this.b64 = b64;
            this.remotePieces = progress == 100 ? remotePieces + 1 : remotePieces;
            this.totalPieces = totalPieces;
            this.speed = speed;
            this.browse = browse;
            this.feed = feed;
            this.browsing = browsing;
            this.subscribed = subscribed;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Upload>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(name)).append("</Name>");
            sb.append("<Progress>").append(Util._t("{0}% of piece", String.valueOf(progress))).append("</Progress>");
            sb.append("<Downloader>").append(Util.escapeHTMLinXML(downloader)).append("</Downloader>");
            sb.append("<DownloaderB64>").append(b64).append("</DownloaderB64>");
            sb.append("<RemotePieces>").append(remotePieces).append("/").append(totalPieces).append("</RemotePieces>");
            sb.append("<Speed>").append(DataHelper.formatSize2Decimal(speed, false)).append("B/sec").append("</Speed>");
            sb.append("<Browse>").append(browse).append("</Browse>");
            sb.append("<Browsing>").append(browsing).append("</Browsing>");
            sb.append("<Feed>").append(feed).append("</Feed>");
            sb.append("<Subscribed>").append(subscribed).append("</Subscribed>");
            sb.append("</Upload>");
        }
    }
    
    private static final Comparator<UploadEntry> BY_NAME = (l, r) -> {
        return Collator.getInstance().compare(l.name, r.name);
    };
    
    private static final Comparator<UploadEntry> BY_PROGRESS = (l, r) -> {
        return Integer.compare(l.progress, r.progress);
    };
    
    private static final Comparator<UploadEntry> BY_DOWNLOADER = (l, r) -> {
        return Collator.getInstance().compare(l.downloader, r.downloader);
    };
    
    private static final Comparator<UploadEntry> BY_REMOTE_PIECES = (l, r) -> {
        float lrp = l.remotePieces * 1f / l.totalPieces;
        float rrp = r.remotePieces * 1f / r.totalPieces;
        return Float.compare(lrp, rrp);
    };
    
    private static final Comparator<UploadEntry> BY_SPEED = (l, r) -> {
        return Integer.compare(l.speed, r.speed);
    };
    
    private static final Comparator<UploadEntry> BY_BROWSE = (l, r) -> {
        return Boolean.compare(l.browse, r.browse);
    };
    
    private static final Comparator<UploadEntry> BY_FEED = (l, r) -> {
        return Boolean.compare(l.feed, r.feed);
    };
    
    private static final ColumnComparators<UploadEntry> COMPARATORS = new ColumnComparators<>();
    static {
        COMPARATORS.add("Name", BY_NAME);
        COMPARATORS.add("Progress", BY_PROGRESS);
        COMPARATORS.add("Downloader", BY_DOWNLOADER);
        COMPARATORS.add("Remote Pieces", BY_REMOTE_PIECES);
        COMPARATORS.add("Speed", BY_SPEED);
        COMPARATORS.add("Browse", BY_BROWSE);
        COMPARATORS.add("Feed", BY_FEED);
    }

}
