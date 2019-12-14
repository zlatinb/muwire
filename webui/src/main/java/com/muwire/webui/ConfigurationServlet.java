package com.muwire.webui;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;

public class ConfigurationServlet extends HttpServlet {
    
    private Core core;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        
        clearAllBooleans();
        
        Enumeration<String> patameterNames = req.getParameterNames();
        while(patameterNames.hasMoreElements()) {
            String name = patameterNames.nextElement();
            String value = req.getParameter(name);
            update(name, value);
        }
        core.saveMuSettings();
        core.saveI2PSettings();
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
    
    private void update(String name, String value) {
        switch(name) {
        case "allowUntrusted" : core.getMuOptions().setAllowUntrusted(false); break;
        case "searchExtraHop" : core.getMuOptions().setSearchExtraHop(true); break;
        case "allowTrustLists": core.getMuOptions().setAllowTrustLists(true); break;
        case "trustListInterval" : core.getMuOptions().setTrustListInterval(Integer.parseInt(value)); break;
        case "downloadRetryInterval" : core.getMuOptions().setDownloadRetryInterval(Integer.parseInt(value)); break;
        case "totalUploadSlots" : core.getMuOptions().setTotalUploadSlots(Integer.parseInt(value)); break;
        case "uploadSlotsPerUser" : core.getMuOptions().setUploadSlotsPerUser(Integer.parseInt(value)); break;
        case "downloadLocation" : core.getMuOptions().setDownloadLocation(new File(value)); break;
        case "incompleteLocation" : core.getMuOptions().setIncompleteLocation(new File(value)); break;
        case "shareDownloadedFiles" : core.getMuOptions().setShareDownloadedFiles(true); break;
        case "shareHiddenFiles" : core.getMuOptions().setShareHiddenFiles(true); break;
        case "searchComments" : core.getMuOptions().setSearchComments(true); break;
        case "browseFiles" : core.getMuOptions().setBrowseFiles(true); break;
        case "speedSmoothSeconds" : core.getMuOptions().setSpeedSmoothSeconds(Integer.parseInt(value)); break;
        case "inBw" : core.getMuOptions().setInBw(Integer.parseInt(value)); break;
        case "outBw" : core.getMuOptions().setOutBw(Integer.parseInt(value)); break;
        case "inbound.length" : core.getI2pOptions().setProperty(name, name); break;
        case "inbound.quantity" : core.getI2pOptions().setProperty(name, value); break;
        case "outbound.length" : core.getI2pOptions().setProperty(name, name); break;
        case "outbound.quantity" : core.getI2pOptions().setProperty(name, value); break;
        // TODO: ui settings
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        core = (Core) config.getServletContext().getAttribute("core");
    }

}
