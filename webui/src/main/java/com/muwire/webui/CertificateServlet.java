package com.muwire.webui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

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
        
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        sb.append("<CertificateRequest>");
        sb.append("<Status>").append(request.getStatus().toString()).append("</Status>");
        sb.append("<Total>").append(request.totalCertificates()).append("</Total>");
        sb.append("<Certificates>");
        for (Certificate certificate : request.getCertificates()) {
            sb.append("<Certificate>");
            sb.append("<Issuer>").append(certificate.getIssuer().getHumanReadableName()).append("</Issuer>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(certificate.getName().getName())).append("</Name>");
            if (certificate.getComment() != null)
                sb.append("<Comment>").append(Util.escapeHTMLinXML(certificate.getComment().getName())).append("</Comment>");
            sb.append("<Timestamp>").append(DataHelper.formatTime(certificate.getTimestamp())).append("</Timestamp>");
            sb.append("<Base64>").append(certificate.toBase64()).append("</Base64>");
            sb.append("<Imported>").append(core.getCertificateManager().isImported(certificate)).append("</Imported>");
            sb.append("</Certificate>");
        }
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
    
    

}
