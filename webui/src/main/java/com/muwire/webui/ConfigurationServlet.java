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
        core.getMuOptions().setFileFeed(true);
        core.getMuOptions().setAdvertiseFeed(true);
        core.getMuOptions().setAutoPublishSharedFiles(false);
        core.getMuOptions().setDefaultFeedAutoDownload(false);
        core.getMuOptions().setDefaultFeedSequential(false);
    }
    
    private void update(String name, String value) throws Exception {
        switch(name) {
        case "allowUntrusted" : core.getMuOptions().setAllowUntrusted(false); break;
        case "searchExtraHop" : core.getMuOptions().setSearchExtraHop(true); break;
        case "allowTrustLists": core.getMuOptions().setAllowTrustLists(true); break;
        case "trustListInterval" : core.getMuOptions().setTrustListInterval(getPositiveInteger(value,"Trust list update frequency (hours)")); break;
        case "downloadRetryInterval" : core.getMuOptions().setDownloadRetryInterval(getPositiveInteger(value,"Download retry frequency (seconds)")); break;
        case "totalUploadSlots" : core.getMuOptions().setTotalUploadSlots(getInteger(value,"Total upload slots (-1 means unlimited)")); break;
        case "uploadSlotsPerUser" : core.getMuOptions().setUploadSlotsPerUser(getInteger(value,"Upload slots per user (-1 means unlimited)")); break;
        case "downloadLocation" : core.getMuOptions().setDownloadLocation(getDirectory(value)); break;
        case "incompleteLocation" : core.getMuOptions().setIncompleteLocation(getDirectory(value)); break;
        case "shareDownloadedFiles" : core.getMuOptions().setShareDownloadedFiles(true); break;
        case "shareHiddenFiles" : core.getMuOptions().setShareHiddenFiles(true); break;
        case "searchComments" : core.getMuOptions().setSearchComments(true); break;
        case "browseFiles" : core.getMuOptions().setBrowseFiles(true); break;
        case "speedSmoothSeconds" : core.getMuOptions().setSpeedSmoothSeconds(getPositiveInteger(value,"Download speed smoothing interval (second)")); break;
        case "inbound.length" : core.getI2pOptions().setProperty(name, String.valueOf(getPositiveInteger(value,"Inbound tunnel length"))); break;
        case "inbound.quantity" : core.getI2pOptions().setProperty(name, String.valueOf(getPositiveInteger(value,"Inbound tunnel quantity"))); break;
        case "outbound.length" : core.getI2pOptions().setProperty(name, String.valueOf(getPositiveInteger(value,"Outbound tunnel length"))); break;
        case "outbound.quantity" : core.getI2pOptions().setProperty(name, String.valueOf(getPositiveInteger(value,"Outbound tunnel quantity"))); break;
        case "fileFeed" : core.getMuOptions().setFileFeed(true); break;
        case "advertiseFeed" : core.getMuOptions().setAdvertiseFeed(true); break;
        case "autoPublishSharedFiles" : core.getMuOptions().setAutoPublishSharedFiles(true); break;
        case "defaultFeedAutoDownload" : core.getMuOptions().setDefaultFeedAutoDownload(true); break;
        case "defaultFeedSequential" : core.getMuOptions().setDefaultFeedSequential(true); break;
        case "defaultFeedUpdateInterval" : core.getMuOptions().setDefaultFeedUpdateInterval(60000 * getPositiveInteger(value,"Feed update frequency (minutes")); break;
        case "defaultFeedItemsToKeep" : core.getMuOptions().setDefaultFeedItemsToKeep(getInteger(value, "Number of items to keep on disk (-1 means unlimited)")); break;
        
        // TODO: ui settings
        }
    }
    
    private static int getInteger(String s, String fieldName) throws Exception {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new Exception(Util._t("Bad input")+ ":  \"" + s + "\"  " + Util._t(fieldName));
        }
    }
    
    private static int getPositiveInteger(String s, String fieldName) throws Exception {
        int rv = getInteger(s, fieldName);
        if (rv <= 0)
            throw new Exception(Util._t("Bad input")+ ":  \"" + s + "\"  " + Util._t(fieldName));
        return rv;
    }
    
    private static File getDirectory(String s) throws Exception {
        File f = new File(s);
        f = f.getCanonicalFile();
        if (!f.exists())
            throw new Exception(Util._t("Bad input") + " : " + Util._t("{0} does not exist",s));
        if (!f.isDirectory())
            throw new Exception(Util._t("Bad input") + " : " + Util._t("{0} is not a directory",s));
        if (!f.canWrite())
            throw new Exception(Util._t("Bad input") + " : " + Util._t("{0} not writeable",s));
        return f;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        context = config.getServletContext();
        core = (Core) config.getServletContext().getAttribute("core");
    }

}
