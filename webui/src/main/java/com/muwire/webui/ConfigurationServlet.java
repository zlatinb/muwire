package com.muwire.webui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;

public class ConfigurationServlet extends HttpServlet {
    
    private Core core;
    private ServletContext context;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            clearAllBooleans();

            Enumeration<String> patameterNames = req.getParameterNames();
            while(patameterNames.hasMoreElements()) {
                String name = patameterNames.nextElement();
                String value = req.getParameter(name);
                update(name, value);
            }
            core.saveMuSettings();
            core.saveI2PSettings();
            context.setAttribute("MWConfigError", null);
        } catch (Exception e) {
            context.setAttribute("MWConfigError", e);
        }
        
        resp.sendRedirect("/MuWire/ConfigurationPage");
    }
    
    private void clearAllBooleans() {
        core.getMuOptions().setAllowUntrusted(true);
        core.getMuOptions().setSearchExtraHop(false);
        core.getMuOptions().setAllowTrustLists(false);
        core.getMuOptions().setShareDownloadedFiles(false);
        core.getMuOptions().setShareHiddenFiles(false);
        core.getMuOptions().setSearchComments(false);
        core.getMuOptions().setBrowseFiles(false);
    }
    
    private void update(String name, String value) throws Exception {
        switch(name) {
        case "allowUntrusted" : core.getMuOptions().setAllowUntrusted(false); break;
        case "searchExtraHop" : core.getMuOptions().setSearchExtraHop(true); break;
        case "allowTrustLists": core.getMuOptions().setAllowTrustLists(true); break;
        case "trustListInterval" : core.getMuOptions().setTrustListInterval(getPositiveInteger(value)); break;
        case "downloadRetryInterval" : core.getMuOptions().setDownloadRetryInterval(getPositiveInteger(value)); break;
        case "totalUploadSlots" : core.getMuOptions().setTotalUploadSlots(Integer.parseInt(value)); break;
        case "uploadSlotsPerUser" : core.getMuOptions().setUploadSlotsPerUser(Integer.parseInt(value)); break;
        case "downloadLocation" : core.getMuOptions().setDownloadLocation(getDirectory(value)); break;
        case "incompleteLocation" : core.getMuOptions().setIncompleteLocation(getDirectory(value)); break;
        case "shareDownloadedFiles" : core.getMuOptions().setShareDownloadedFiles(true); break;
        case "shareHiddenFiles" : core.getMuOptions().setShareHiddenFiles(true); break;
        case "searchComments" : core.getMuOptions().setSearchComments(true); break;
        case "browseFiles" : core.getMuOptions().setBrowseFiles(true); break;
        case "speedSmoothSeconds" : core.getMuOptions().setSpeedSmoothSeconds(getPositiveInteger(value)); break;
        case "inBw" : core.getMuOptions().setInBw(getPositiveInteger(value)); break;
        case "outBw" : core.getMuOptions().setOutBw(getPositiveInteger(value)); break;
        case "inbound.length" : core.getI2pOptions().setProperty(name, value); break;
        case "inbound.quantity" : core.getI2pOptions().setProperty(name, value); break;
        case "outbound.length" : core.getI2pOptions().setProperty(name, value); break;
        case "outbound.quantity" : core.getI2pOptions().setProperty(name, value); break;
        // TODO: ui settings
        }
    }
    
    private static int getPositiveInteger(String s) throws Exception {
        int rv = Integer.parseInt(s);
        if (rv <= 0)
            throw new Exception(s + " is negative");
        return rv;
    }
    
    private static File getDirectory(String s) throws Exception {
        File f = new File(s);
        if (!f.exists())
            throw new Exception(s + " does not exist");
        if (!f.isDirectory())
            throw new Exception(s + " is not a directory");
        if (!f.canWrite())
            throw new Exception(s + " cannot be written to");
        return f;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        context = config.getServletContext();
        core = (Core) config.getServletContext().getAttribute("core");
    }

}
