package com.muwire.webui;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Constants;
import com.muwire.core.util.DataUtil;

public class InitServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String nickname = req.getParameter("nickname");
            if (nickname == null || nickname.trim().length() == 0)
                throw new Exception("Nickname cannot be blank");
            
            if (!DataUtil.isValidName(nickname))
                throw new Exception("Nickname cannot contain any of " + Constants.INVALID_NICKNAME_CHARS +
                        " and must be no longer than " + Constants.MAX_NICKNAME_LENGTH + " characters.");
            
            String downloadLocation = req.getParameter("download_location");
            if (downloadLocation == null)
                throw new Exception("Download location cannot be blank");
            File downloadLocationFile = new File(downloadLocation);
            if (!downloadLocationFile.exists()) {
                if (!downloadLocationFile.mkdirs())
                    throw new Exception("Couldn't create download location");
            } else if (downloadLocationFile.isFile())
                throw new Exception("Download location must point to a directory");
            else if (!downloadLocationFile.canWrite())
                throw new Exception("Download location not writeable");
            
            String incompleteLocation = req.getParameter("incomplete_location");
            if (incompleteLocation == null)
                throw new Exception("Incomplete files location cannot be blank");
            File incompleteLocationFile = new File(incompleteLocation);
            if (!incompleteLocationFile.exists()) {
                if (!incompleteLocationFile.mkdirs())
                    throw new Exception("Couldn't create incomplete files location");
            } else if (incompleteLocationFile.isFile())
                throw new Exception("Incomplete files location must point to a directory");
            else if (!incompleteLocationFile.canWrite())
                throw new Exception("Incomplete files location not writeable");
            
            String dropBoxLocation = req.getParameter("dropbox_location");
            if (dropBoxLocation == null)
                throw new Exception("DropBox location cannot be blank");
            File dropBoxLocationFile = new File(dropBoxLocation);
            if (!dropBoxLocationFile.exists()) {
                if (!dropBoxLocationFile.mkdirs())
                    throw new Exception("Couldn't create DropBox location");
            } else if (dropBoxLocationFile.isFile())
                throw new Exception("DropBox location must point to a directory");
            else if (!dropBoxLocationFile.canWrite())
                throw new Exception("DropBox location not writeable");
            
            MuWireClient client = (MuWireClient) req.getServletContext().getAttribute("mwClient");
            client.initMWProps(nickname, downloadLocationFile, incompleteLocationFile, dropBoxLocationFile);
            client.start();
            resp.sendRedirect("/MuWire/index");
        } catch (Throwable e) {
            req.getServletContext().setAttribute("MWInitError", e);
            resp.sendRedirect("/MuWire/MuWire");
        }
                
    }

}
