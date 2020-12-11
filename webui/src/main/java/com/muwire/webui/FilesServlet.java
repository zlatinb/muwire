package com.muwire.webui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.SharedFile;
import com.muwire.core.filecert.CertificateManager;
import com.muwire.core.util.DataUtil;
import com.muwire.core.files.FileListCallback;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class FilesServlet extends HttpServlet {
    
    private FileManager fileManager;
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
            sb.append("<Count>").append(fileManager.numSharedFiles()).append("</Count>");
            sb.append("<Revision>").append(fileManager.getRevision()).append("</Revision>");
            String hashingFile = fileManager.getHashingFile();
            if (hashingFile != null)
                sb.append("<Hashing>").append(Util.escapeHTMLinXML(hashingFile)).append("</Hashing>");
            sb.append("</Status>");
        } else if (section.equals("fileTree")) {
            sb.append("<Files>");
            sb.append("<Revision>").append(fileManager.getRevision()).append("</Revision>");
            
            ListCallback cb = new ListCallback();
            String encodedPath = req.getParameter("path");
            File current = null;
            if (encodedPath != null && encodedPath.length() > 0) {
                String[] split = DataHelper.split(encodedPath, ",");
                for (String element : split) {
                    element = Base64.decodeToString(element);
                    if (element == null) {
                        resp.sendError(403, "bad path");
                        return;
                    }
                    element = Util.unescapeHTMLinXML(element);
                    if (current == null) {
                        current = new File(element);
                        continue;
                    }
                    current = new File(current, element);
                }
            }
            fileManager.list(current, cb);
            
            Comparator<TreeEntry> comparator = (l, r) -> {
                return Collator.getInstance().compare(l.file.getName(), r.file.getName());
            };
            
            Collections.sort(cb.treeEntries, comparator);
            
            cb.treeEntries.forEach(e -> e.toXML(sb));
            sb.append("</Files>");
        } else if (section.equals("fileTable")) {
            
            List<FilesTableEntry> entries = new ArrayList<>();
            fileManager.getAllFiles().forEach(sf -> {
                String comment = null;
                if (sf.getComment() != null) 
                    comment = DataUtil.readi18nString(Base64.decode(sf.getComment()));
                InfoHash ih = new InfoHash(sf.getRoot());
                FilesTableEntry entry = new FilesTableEntry(sf.getFile().getName(),
                        ih,
                        sf.getCachedPath(),
                        sf.getCachedLength(),
                        comment,
                        core.getCertificateManager().hasLocalCertificate(ih),
                        sf.isPublished());
                entries.add(entry);
            });
            
            COMPARATORS.sort(entries, req);
            
            sb.append("<Files>");
            entries.forEach(entry -> entry.toXML(sb));
            sb.append("</Files>");
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
    public void init(ServletConfig cfg) throws ServletException {
        fileManager = (FileManager) cfg.getServletContext().getAttribute("fileManager");
        core = (Core) cfg.getServletContext().getAttribute("core");
    }

    private class ListCallback implements FileListCallback<SharedFile> {
        private final List<TreeEntry> treeEntries = new ArrayList<>();
        
        @Override
        public void onFile(File f, SharedFile value) {
            treeEntries.add(new FileTreeEntry(f, value));
        }
        @Override
        public void onDirectory(File f) {
            treeEntries.add(new DirectoryTreeEntry(f));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        if (action == null) {
            resp.sendError(403,"Bad param");
            return;
        }
        if (action.equals("share")) {
            String file = req.getParameter("file");
            if (file == null || file.trim().length() == 0) {
                resp.sendError(403, "Bad param");
                return;
            }
            fileManager.share(file);
            resp.sendRedirect("/MuWire/SharedFiles");
        } else if (action.equals("unshare")) {
            String pathElements = req.getParameter("path");
            if (pathElements == null) {
                resp.sendError(403,"Bad param");
                return;
            }
            File file = Util.getFromPathElements(pathElements);
            if (file == null) {
                resp.sendError(403, "Bad param");
                return;
            }
            fileManager.unshareFile(file);
        } else if (action.equals("comment")) {
            String pathElements = req.getParameter("path");
            if (pathElements == null) {
                resp.sendError(403,"Bad param");
                return;
            }
            File file = Util.getFromPathElements(pathElements);
            if (file == null) {
                resp.sendError(403, "Bad param");
                return;
            }
            String comment = req.getParameter("comment"); // null is ok
            if (comment.isEmpty())
                comment = null;
            fileManager.comment(file, comment);
        }
    }
    
    private static class FilesTableEntry {
        private final String name;
        private final InfoHash infoHash;
        private final String path;
        private final long size;
        private final String comment;
        private final boolean certified;
        private final boolean published;
        
        FilesTableEntry(String name, InfoHash infoHash, String path, long size, String comment, boolean certified, boolean published) {
            this.name = name;
            this.infoHash = infoHash;
            this.path = path;
            this.size = size;
            this.comment = comment;
            this.certified = certified;
            this.published = published;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<File>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(name)).append("</Name>");
            sb.append("<InfoHash>").append(Base64.encode(infoHash.getRoot())).append("</InfoHash>");
            sb.append("<Path>").append(Util.escapeHTMLinXML(path)).append("</Path>");
            sb.append("<Size>").append(Util.formatSize2Decimal(size)).append("B").append("</Size>");
            if (comment != null) {
                sb.append("<Comment>").append(Util.escapeHTMLinXML(comment)).append("</Comment>");
            }
            sb.append("<Certified>").append(certified).append("</Certified>");
            sb.append("<Published>").append(published).append("</Published>");
            sb.append("</File>");
        }
    }
    
    private static final Comparator<FilesTableEntry> BY_PATH = (l, r) -> {
        return Collator.getInstance().compare(l.path, r.path);
    };
    
    private static final Comparator<FilesTableEntry> BY_SIZE = (l, r) -> {
        return Long.compare(l.size, r.size);
    };
    
    private static final ColumnComparators<FilesTableEntry> COMPARATORS = new ColumnComparators<>();
    static {
        COMPARATORS.add("File", BY_PATH);
        COMPARATORS.add("Size", BY_SIZE);
    }
 
    private abstract class TreeEntry {
        protected final File file;
        TreeEntry(File file) {
            this.file = file;
        }
        
        abstract void toXML(StringBuilder sb);
    }
    
    private class DirectoryTreeEntry extends TreeEntry {
        DirectoryTreeEntry(File file) {
            super(file);
        }
        
        void toXML(StringBuilder sb) {
            String name = file.getName().isEmpty() ? file.toString() : file.getName();
            boolean shared = core.getWatchedDirectoryManager().isWatched(file);
            sb.append("<Directory>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(name)).append("</Name>");
            sb.append("<Shared>").append(shared).append("</Shared>");
            sb.append("</Directory>");
        }
    }
    
    private class FileTreeEntry extends TreeEntry {
        private SharedFile sf;
        FileTreeEntry(File file, SharedFile sf) {
            super(file);
            this.sf = sf;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<File>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(sf.getFile().getName())).append("</Name>");
            sb.append("<Path>").append(Util.escapeHTMLinXML(sf.getCachedPath())).append("</Path>");
            sb.append("<Size>").append(Util.formatSize2Decimal(sf.getCachedLength())).append("B").append("</Size>");
            sb.append("<InfoHash>").append(Base64.encode(sf.getRoot())).append("</InfoHash>");
            if (sf.getComment() != null) {
                String comment = DataUtil.readi18nString(Base64.decode(sf.getComment()));
                sb.append("<Comment>").append(Util.escapeHTMLinXML(comment)).append("</Comment>");
            }
            InfoHash ih = new InfoHash(sf.getRoot());
            sb.append("<Certified>").append(core.getCertificateManager().hasLocalCertificate(ih)).append("</Certified>");
            sb.append("<Published>").append(sf.isPublished()).append("</Published>");
            // TODO: other stuff
            sb.append("</File>");
        }
    }
}
