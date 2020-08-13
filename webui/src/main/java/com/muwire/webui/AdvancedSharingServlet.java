package com.muwire.webui;

import java.io.File;
import java.io.IOException;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.files.directories.WatchedDirectory;

import net.i2p.data.DataHelper;

public class AdvancedSharingServlet extends HttpServlet {

    private Core core;
    private AdvancedSharingManager advancedSharingManager;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String section = req.getParameter("section");
        if (section == null) {
            resp.sendError(403, "Bad section param");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        
        if (section.equals("revision")) {
            sb.append("<Revision>").append(advancedSharingManager.getRevision()).append("</Revision>");
        } else if (section.equals("dirs")) {
            List<WrappedDir> dirs = core.getWatchedDirectoryManager().getWatchedDirsStream().
                    map(WrappedDir::new).
                    collect(Collectors.toList());
            DIR_COMPARATORS.sort(dirs, req);
            
            sb.append("<WatchedDirs>");
            dirs.forEach(d -> d.toXML(sb));
            sb.append("</WatchedDirs>");
        } else {
            resp.sendError(403, "Bad section param");
            return;
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
        String path = req.getParameter("path");
        if (path == null) {
            resp.sendError(403, "Bad param");
            return;
        }
        
        File dir = Util.getFromPathElements(path);
        
        if (action.equals("sync")) {
            advancedSharingManager.sync(dir);
            Util.pause();
        } else if (action.equals("configure")) {
            boolean autoWatch = Boolean.parseBoolean(req.getParameter("autoWatch"));
            int syncInterval = Integer.parseInt(req.getParameter("syncInterval"));
            advancedSharingManager.configure(dir, autoWatch, syncInterval);
            Util.pause();
            resp.sendRedirect("/MuWire/AdvancedSharing");
        }
    }
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        core = (Core) config.getServletContext().getAttribute("core");
        advancedSharingManager = (AdvancedSharingManager) config.getServletContext().getAttribute("advancedSharingManager");
    }
    
    private static class WrappedDir {
        private final String directory;
        private final boolean autoWatch;
        private final long lastSync;
        private final int syncInterval;
        
        WrappedDir(WatchedDirectory wd) {
            this.directory = wd.getDirectory().getAbsolutePath();
            this.autoWatch = wd.getAutoWatch();
            this.lastSync = wd.getLastSync();
            this.syncInterval = wd.getSyncInterval();
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<WatchedDir>");
            sb.append("<Directory>").append(Util.escapeHTMLinXML(directory)).append("</Directory>");
            sb.append("<AutoWatch>").append(autoWatch).append("</AutoWatch>");
            sb.append("<LastSync>").append(DataHelper.formatTime(lastSync)).append("</LastSync>");
            sb.append("<LastSyncTS>").append(lastSync).append("</LastSyncTS>");
            sb.append("<SyncInterval>").append(syncInterval).append("</SyncInterval>");
            sb.append("</WatchedDir>");
        }
    }
    
    private static final Comparator<WrappedDir> BY_DIRECTORY = (l, r) -> {
        return Collator.getInstance().compare(l.directory, r.directory);
    };
    
    private static final Comparator<WrappedDir> BY_AUTOWATCH = (l, r) -> {
        return Boolean.compare(l.autoWatch, r.autoWatch);
    };
    
    private static final Comparator<WrappedDir> BY_LAST_SYNC = (l, r) -> {
        return Long.compare(l.lastSync, r.lastSync);
    };
    
    private static final Comparator<WrappedDir> BY_SYNC_INTERVAL = (l, r) -> {
        return Integer.compare(l.syncInterval, r.syncInterval);
    };
    
    private static final ColumnComparators<WrappedDir> DIR_COMPARATORS = new ColumnComparators<>();
    static {
        DIR_COMPARATORS.add("Directory", BY_DIRECTORY);
        DIR_COMPARATORS.add("Auto Watch", BY_AUTOWATCH);
        DIR_COMPARATORS.add("Last Sync", BY_LAST_SYNC);
        DIR_COMPARATORS.add("Sync Interval", BY_SYNC_INTERVAL);
    }
}
