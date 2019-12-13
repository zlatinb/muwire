package com.muwire.webui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.Persona;
import com.muwire.core.filecert.Certificate;
import com.muwire.webui.CertificateManager.CertificateRequest;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class CertificateServlet extends HttpServlet {
    
    private CertificateManager certificateManager;
    private Core core;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userB64 = req.getParameter("user");
        Persona user;
        try {
            user = new Persona(new ByteArrayInputStream(Base64.decode(userB64)));
        } catch (Exception bad) {
            resp.sendError(403, "Bad param");
            return;
        }
        
        String infoHashB64 = req.getParameter("infoHash");
        InfoHash infoHash;
        try {
            infoHash = new InfoHash(Base64.decode(infoHashB64));
        } catch (Exception bad) {
            resp.sendError(403, "Bad param");
            return;
        }
        
        CertificateRequest request = certificateManager.get(user, infoHash);
        if (request == null) {
            resp.sendError(404,"Not found");
            return;
        }
        
        List<CertificateEntry> entries = new ArrayList<>();
        for(Certificate certificate : request.getCertificates()) {
            String comment = certificate.getComment() != null ? certificate.getComment().getName() : null;
            CertificateEntry entry = new CertificateEntry(
                    certificate.getIssuer(),
                    certificate.toBase64(),
                    certificate.getName().getName(),
                    certificate.getTimestamp(),
                    comment,
                    core.getCertificateManager().isImported(certificate));
            entries.add(entry);
        }
        
        COMPARATORS.sort(entries, req);
        
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        sb.append("<CertificateRequest>");
        sb.append("<Status>").append(request.getStatus().toString()).append("</Status>");
        sb.append("<Total>").append(request.totalCertificates()).append("</Total>");
        sb.append("<Certificates>");
        entries.forEach(entry -> entry.toXML(sb));
        sb.append("</Certificates>");
        sb.append("</CertificateRequest>");
        
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
            resp.sendError(403, "Bad param");
            return;
        }
        
        if (action.equals("fetch")) {
            String userB64 = req.getParameter("user");
            Persona user;
            try {
                user = new Persona(new ByteArrayInputStream(Base64.decode(userB64)));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            String infoHashB64 = req.getParameter("infoHash");
            InfoHash infoHash;
            try {
                infoHash = new InfoHash(Base64.decode(infoHashB64));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            
            certificateManager.request(user, infoHash);
        } else if (action.equals("import")) {
            String certB64 = req.getParameter("base64");
            Certificate certificate;
            try {
                certificate = new Certificate(new ByteArrayInputStream(Base64.decode(certB64)));
            } catch (Exception bad) {
                resp.sendError(403, "Bad param");
                return;
            }
            certificateManager.importCertificate(certificate);
        } else if (action.equals("certify")) {
            String path = req.getParameter("file");
            if (path == null) {
                resp.sendError(403,"Bad param");
                return;
            }
            File file = Util.getFromPathElements(path);
            certificateManager.certify(file);
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        certificateManager = (CertificateManager) config.getServletContext().getAttribute("certificateManager");
        core = (Core) config.getServletContext().getAttribute("core");
    }
    
    private static class CertificateEntry {
        private final Persona persona;
        private final String b64;
        private final String name;
        private final long timestamp;
        private final String comment;
        private final boolean imported;
        
        CertificateEntry(Persona persona, String b64, String name, long timestamp, String comment, boolean imported) {
            this.persona = persona;
            this.b64 = b64;
            this.name = name;
            this.timestamp = timestamp;
            this.comment = comment;
            this.imported = imported;
        }
        
        void toXML(StringBuilder sb) {
            sb.append("<Certificate>");
            sb.append("<Issuer>").append(Util.escapeHTMLinXML(persona.getHumanReadableName())).append("</Issuer>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(name)).append("</Name>");
            if (comment != null)
                sb.append("<Comment>").append(Util.escapeHTMLinXML(comment)).append("</Comment>");
            sb.append("<Base64>").append(b64).append("</Base64>");
            sb.append("<Timestamp>").append(DataHelper.formatTime(timestamp)).append("</Timestamp>");
            sb.append("<Imported>").append(imported).append("</Imported>");
            sb.append("</Certificate>");
        }
    }

    private static final Comparator<CertificateEntry> BY_ISSUER = (l, r) -> {
        return l.persona.getHumanReadableName().compareTo(r.persona.getHumanReadableName());
    };
    
    private static final Comparator<CertificateEntry> BY_NAME = (l, r) -> {
        return l.name.compareTo(r.name);
    };
    
    private static final Comparator<CertificateEntry> BY_TIMESTAMP = (l, r) -> {
        return Long.compare(l.timestamp, r.timestamp);
    };
    
    private static final Comparator<CertificateEntry> BY_IMPORTED = (l, r) -> {
        return Boolean.compare(l.imported, r.imported);
    };
    
    private static final ColumnComparators<CertificateEntry> COMPARATORS = new ColumnComparators<>();
    static {
        COMPARATORS.add("Issuer", BY_ISSUER);
        COMPARATORS.add("Name", BY_NAME);
        COMPARATORS.add("Timestamp", BY_TIMESTAMP);
        COMPARATORS.add("Import", BY_IMPORTED);
    }
}
