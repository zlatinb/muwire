package com.muwire.webui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import com.muwire.core.Core;
import com.muwire.core.files.FileSharedEvent;

public class DropBoxServlet extends HttpServlet {
    
    private Core core;
    private WebUISettings webSettings;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            for(Part part : req.getParts()) {
                String fileName = part.getSubmittedFileName();
                File target = new File(webSettings.getDropBoxLocation(), fileName);

                try (OutputStream os = new FileOutputStream(target)) {
                    InputStream is = part.getInputStream();
                    byte [] tmp = new byte[0x1 << 13];
                    int read;
                    while((read = is.read(tmp)) > 0) {
                        os.write(tmp, 0, read);
                    }
                }
                
                FileSharedEvent event = new FileSharedEvent();
                event.setFile(target);
                core.getEventBus().publish(event);
            }
        } catch (IOException iox) {
            resp.sendError(403, iox.getMessage());
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        core = (Core) config.getServletContext().getAttribute("core");
        webSettings = (WebUISettings) config.getServletContext().getAttribute("webSettings");
    }
}
