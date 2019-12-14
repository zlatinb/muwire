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
        Enumeration<String> patameterNames = req.getParameterNames();
        while(patameterNames.hasMoreElements()) {
            String name = patameterNames.nextElement();
            String value = req.getParameter(name);
            update(name, value);
        }
        core.saveMuSettings();
        core.saveI2PSettings();
    }
    
    private void update(String name, String value) {
        switch(name) {
        case "allowUntrusted" : core.getMuOptions().setAllowUntrusted(Boolean.parseBoolean(value)); break;
        case "searchExtraHop" : core.getMuOptions().setSearchExtraHop(Boolean.parseBoolean(value)); break;
        case "allowTrustLists": core.getMuOptions().setAllowTrustLists(Boolean.parseBoolean(value)); break;
        case "trustListInterval" : core.getMuOptions().setTrustListInterval(Integer.parseInt(value)); break;
        case "downloadRetryInterval" : core.getMuOptions().setDownloadRetryInterval(Integer.parseInt(value)); break;
        case "totalUploadSlots" : core.getMuOptions().setTotalUploadSlots(Integer.parseInt(value)); break;
        case "uploadSlotsPerUser" : core.getMuOptions().setUploadSlotsPerUser(Integer.parseInt(value)); break;
        case "downloadLocation" : core.getMuOptions().setDownloadLocation(new File(value)); break;
        case "incompleteLocation" : core.getMuOptions().setIncompleteLocation(new File(value)); break;
        case "shareDownloadedFiles" : core.getMuOptions().setShareDownloadedFiles(Boolean.parseBoolean(value)); break;
        case "shareHiddenFiles" : core.getMuOptions().setShareHiddenFiles(Boolean.parseBoolean(value)); break;
        case "searchComments" : core.getMuOptions().setSearchComments(Boolean.parseBoolean(value)); break;
        case "browseFiles" : core.getMuOptions().setBrowseFiles(Boolean.parseBoolean(value)); break;
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
