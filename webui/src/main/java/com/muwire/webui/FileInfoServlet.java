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
import com.muwire.core.InfoHash;
import com.muwire.core.Persona;
import com.muwire.core.SharedFile;
import com.muwire.core.filecert.Certificate;

import net.i2p.data.DataHelper;

public class FileInfoServlet extends HttpServlet {
    
    private Core core;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getParameter("path");
        if (path == null) {
            resp.sendError(403, "bad param");
            return;
        }
        File file = Util.getFromPathElements(path);
        SharedFile sf = core.getFileManager().getFileToSharedFile().get(file);
        if (sf == null) {
            resp.sendError(403, file + " is not shared");
            return;
        }
        
        String section = req.getParameter("section");
        if (section == null) {
            resp.sendError(403, "Bad param");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        
        if (section.equals("searchers")) {
            List<SearchEntry> searchEntries = sf.getSearches().stream().
                    map(e -> new SearchEntry(e)).
                    collect(Collectors.toList());
            SEARCH_ENTRY_COMPARATORS.sort(searchEntries, req);
            sb.append("<SearchEntries>");
            searchEntries.forEach(e -> e.toXML(sb));
            sb.append("</SearchEntries>");
        } else if (section.equals("downloaders")) {
            List<DownloadEntry> downloadEntries = sf.getDownloaders().stream().
                    map(d -> new DownloadEntry(d)).
                    collect(Collectors.toList());
            DOWNLOADER_COMPARATORS.sort(downloadEntries, req);
            sb.append("<Downloaders>");
            downloadEntries.forEach(e -> e.toXML(sb));
            sb.append("</Downloaders>");
        } else if (section.equals("certificates")) {
            List<CertificateEntry> certificateEntries = core.getCertificateManager().getByInfoHash(new InfoHash(sf.getRoot())).stream().
                map(c -> new CertificateEntry(c)).
                collect(Collectors.toList());
            CERT_COMPARATORS.sort(certificateEntries, req);
            sb.append("<Certificates>");
            certificateEntries.forEach(e -> e.toXML(sb));
            sb.append("</Certificates>");
        } else {
            resp.sendError(403, "Bad param");
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
    public void init(ServletConfig config) throws ServletException {
        core = (Core) config.getServletContext().getAttribute("core");
    }
    
    private static class SearchEntry {
        private final Persona persona;
        private final long timestamp;
        private final String query;
        
        SearchEntry(SharedFile.SearchEntry e) {
            this.persona = e.getSearcher();
            this.timestamp = e.getTimestamp();
            this.query = e.getQuery();
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<SearchEntry>");
            sb.append("<Persona>").append(Util.escapeHTMLinXML(persona.getHumanReadableName())).append("</Persona>");
            sb.append("<Timestamp>").append(DataHelper.formatTime(timestamp)).append("</Timestamp>");
            sb.append("<Query>").append(Util.escapeHTMLinXML(query)).append("</Query>");
            sb.append("</SearchEntry>");
        }
    }

    private static final Comparator<SearchEntry> BY_SEARCHER = (l, r) -> {
        return Collator.getInstance().compare(l.persona.getHumanReadableName(), r.persona.getHumanReadableName());
    };
    
    private static final Comparator<SearchEntry> BY_TIMESTAMP = (l, r) -> {
        return Long.compare(l.timestamp, r.timestamp);
    };
    
    private static final Comparator<SearchEntry> BY_QUERY = (l, r) -> {
        return Collator.getInstance().compare(l.query, r.query);
    };
    
    private static final ColumnComparators<SearchEntry> SEARCH_ENTRY_COMPARATORS = new ColumnComparators<>();
    static {
        SEARCH_ENTRY_COMPARATORS.add("Searcher", BY_SEARCHER);
        SEARCH_ENTRY_COMPARATORS.add("Timestamp", BY_TIMESTAMP);
        SEARCH_ENTRY_COMPARATORS.add("Query", BY_QUERY);
    }
    
    private static class DownloadEntry {
        private final String downloader;
        DownloadEntry(String downloader) {
            this.downloader = downloader;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Downloader>");
            sb.append("<Persona>").append(Util.escapeHTMLinXML(downloader)).append("</Persona>");
            sb.append("</Downloader>");
        }
    }
    
    private static final Comparator<DownloadEntry> BY_DOWNLOADER = (l, r) -> {
        return Collator.getInstance().compare(l.downloader, r.downloader);
    };
    
    private static final ColumnComparators<DownloadEntry> DOWNLOADER_COMPARATORS = new ColumnComparators<>();
    static {
        DOWNLOADER_COMPARATORS.add("Downloader", BY_DOWNLOADER);
    }
    
    private static class CertificateEntry {
        private final Certificate certificate;
        CertificateEntry(Certificate certificate) {
            this.certificate = certificate;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Certificate>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(certificate.getName().getName())).append("</Name>");
            if (certificate.getComment() != null)
                sb.append("<Comment>").append(Util.escapeHTMLinXML(certificate.getComment().getName())).append("</Comment>");
            sb.append("<Timestamp>").append(DataHelper.formatTime(certificate.getTimestamp())).append("</Timestamp>");
            sb.append("<TimestampLong>").append(certificate.getTimestamp()).append("</TimestampLong>");
            sb.append("<Issuer>").append(Util.escapeHTMLinXML(certificate.getIssuer().getHumanReadableName())).append("</Issuer>");
            sb.append("</Certificate>");
        }
    }
    
    private static final Comparator<CertificateEntry> CERT_BY_NAME = (l, r) -> {
        return Collator.getInstance().compare(l.certificate.getName().getName(), r.certificate.getName().getName());
    };
    
    private static final Comparator<CertificateEntry> CERT_BY_TIMESTAMP = (l, r) -> {
        return Long.compare(l.certificate.getTimestamp(), r.certificate.getTimestamp());
    };
    
    private static final Comparator<CertificateEntry> CERT_BY_ISSUER = (l, r) -> {
        return Collator.getInstance().compare(l.certificate.getIssuer().getHumanReadableName(), r.certificate.getIssuer().getHumanReadableName());
    };
    
    private static final ColumnComparators<CertificateEntry> CERT_COMPARATORS = new ColumnComparators<>();
    static {
        CERT_COMPARATORS.add("Name", CERT_BY_NAME);
        CERT_COMPARATORS.add("Timestamp", CERT_BY_TIMESTAMP);
        CERT_COMPARATORS.add("Issuer", CERT_BY_ISSUER);
    }
}
