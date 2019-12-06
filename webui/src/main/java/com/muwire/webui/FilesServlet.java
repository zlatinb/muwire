package com.muwire.webui;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.SharedFile;
import com.muwire.core.files.FileListCallback;

import net.i2p.data.Base64;
import net.i2p.data.DataHelper;

public class FilesServlet extends HttpServlet {
    
    private FileManager fileManager;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='UTF-8'?>");
        sb.append("<Files>");
        ListCallback cb = new ListCallback(sb);
        String encodedPath = req.getParameter("path");
        File current = null;
        if (encodedPath != null) {
            String[] split = encodedPath.split(",");
            for (String element : split) {
                element = Base64.decodeToString(element);
                if (current == null) {
                    current = new File(element);
                    continue;
                }
                current = new File(current, element);
            }
        }
        fileManager.list(current, cb);
        sb.append("</Files>");
        resp.setContentType("text/xml");
        resp.setCharacterEncoding("UTF-8");
        resp.setDateHeader("Expires", 0);
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("Cache-Control", "no-store, max-age=0, no-cache, must-revalidate");
        resp.getWriter().write(sb.toString());
        resp.flushBuffer();
    }

    @Override
    public void init(ServletConfig cfg) throws ServletException {
        fileManager = (FileManager) cfg.getServletContext().getAttribute("fileManager");
    }

    private static class ListCallback implements FileListCallback<SharedFile> {
        private final StringBuilder sb;
        ListCallback(StringBuilder sb) {
            this.sb = sb;
        }
        @Override
        public void onFile(File f, SharedFile value) {
            sb.append("<File>");
            sb.append("<Name>").append(Util.escapeHTMLinXML(f.getName())).append("</Name>");
            sb.append("<Size>").append(DataHelper.formatSize2Decimal(value.getCachedLength())).append("B").append("</Size>");
            // TODO: other stuff
            sb.append("</File>");
        }
        @Override
        public void onDirectory(File f) {
            sb.append("<Directory>").append(Util.escapeHTMLinXML(f.getName())).append("</Directory>");
        }
    }
}
