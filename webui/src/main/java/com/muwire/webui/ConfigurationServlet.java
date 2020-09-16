package com.muwire.webui;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.muwire.core.Core;

public class ConfigurationServlet extends HttpServlet {
    
    private static final Map<String, InputValidator> INPUT_VALIDATORS = new HashMap<>();
    static {
        INPUT_VALIDATORS.put("trustListInterval", new PositiveIntegerValidator("Trust list update frequency (hours)"));
        INPUT_VALIDATORS.put("downloadRetryInterval", new PositiveIntegerValidator("Download retry frequency (seconds)"));
        INPUT_VALIDATORS.put("totalUploadSlots", new IntegerValidator("Total upload slots (-1 means unlimited)"));
        INPUT_VALIDATORS.put("uploadSlotsPerUser", new IntegerValidator("Upload slots per user (-1 means unlimited)"));
        INPUT_VALIDATORS.put("downloadLocation", new DirectoryValidator());
        INPUT_VALIDATORS.put("incompleteLocation", new DirectoryValidator());
        INPUT_VALIDATORS.put("speedSmoothSeconds", new PositiveIntegerValidator("Download speed smoothing interval (seconds)"));
        INPUT_VALIDATORS.put("inbound.length", new PositiveIntegerValidator("Inbound tunnel length"));
        INPUT_VALIDATORS.put("inbound.quantity", new PositiveIntegerValidator("Inbound tunnel quantity"));
        INPUT_VALIDATORS.put("outbound.length", new PositiveIntegerValidator("Outbound tunnel length"));
        INPUT_VALIDATORS.put("outbound.quantity", new PositiveIntegerValidator("Outbound tunnel quantity"));
        INPUT_VALIDATORS.put("defaultFeedUpdateInterval", new PositiveIntegerValidator("Feed update frequency (minutes)"));
        INPUT_VALIDATORS.put("defaultFeedItemsToKeep", new IntegerValidator("Number of items to keep on disk (-1 means unlimited)"));
    }
    
    private Core core;
    private ServletContext context;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            
            Enumeration<String> parameterNames = req.getParameterNames();
            while(parameterNames.hasMoreElements()) {
                String name = parameterNames.nextElement();
                InputValidator iv = INPUT_VALIDATORS.get(name);
                if (iv != null)
                    iv.validate(req.getParameter(name));
            }
            
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
        core.getMuOptions().setFileFeed(false);
        core.getMuOptions().setAdvertiseFeed(false);
        core.getMuOptions().setAutoPublishSharedFiles(false);
        core.getMuOptions().setDefaultFeedAutoDownload(false);
        core.getMuOptions().setDefaultFeedSequential(false);
        core.getMuOptions().setAllowTracking(false);
    }
    
    private void update(String name, String value) throws Exception {
        switch(name) {
        case "allowUntrusted" : core.getMuOptions().setAllowUntrusted(false); break;
        case "searchExtraHop" : core.getMuOptions().setSearchExtraHop(true); break;
        case "allowTrustLists": core.getMuOptions().setAllowTrustLists(true); break;
        case "trustListInterval" : core.getMuOptions().setTrustListInterval(Integer.parseInt(value)); break;
        case "downloadRetryInterval" : core.getMuOptions().setDownloadRetryInterval(Integer.parseInt(value)); break;
        case "totalUploadSlots" : core.getMuOptions().setTotalUploadSlots(Integer.parseInt(value)); break;
        case "uploadSlotsPerUser" : core.getMuOptions().setUploadSlotsPerUser(Integer.parseInt(value)); break;
        case "downloadLocation" : core.getMuOptions().setDownloadLocation(getDirectory(value)); break;
        case "incompleteLocation" : core.getMuOptions().setIncompleteLocation(getDirectory(value)); break;
        case "shareDownloadedFiles" : core.getMuOptions().setShareDownloadedFiles(true); break;
        case "shareHiddenFiles" : core.getMuOptions().setShareHiddenFiles(true); break;
        case "searchComments" : core.getMuOptions().setSearchComments(true); break;
        case "browseFiles" : core.getMuOptions().setBrowseFiles(true); break;
        case "allowTracking" : core.getMuOptions().setAllowTracking(true); break;
        case "speedSmoothSeconds" : core.getMuOptions().setSpeedSmoothSeconds(Integer.parseInt(value)); break;
        case "inbound.length" : core.getI2pOptions().setProperty(name, value); break;
        case "inbound.quantity" : core.getI2pOptions().setProperty(name, value); break;
        case "outbound.length" : core.getI2pOptions().setProperty(name, value); break;
        case "outbound.quantity" : core.getI2pOptions().setProperty(name, value); break;
        case "fileFeed" : core.getMuOptions().setFileFeed(true); break;
        case "advertiseFeed" : core.getMuOptions().setAdvertiseFeed(true); break;
        case "autoPublishSharedFiles" : core.getMuOptions().setAutoPublishSharedFiles(true); break;
        case "defaultFeedAutoDownload" : core.getMuOptions().setDefaultFeedAutoDownload(true); break;
        case "defaultFeedSequential" : core.getMuOptions().setDefaultFeedSequential(true); break;
        case "defaultFeedUpdateInterval" : core.getMuOptions().setDefaultFeedUpdateInterval(60000 * Long.parseLong(value)); break;
        case "defaultFeedItemsToKeep" : core.getMuOptions().setDefaultFeedItemsToKeep(Integer.parseInt(value)); break;
        
        // TODO: ui settings
        }
    }
    
    
    private interface InputValidator {
        void validate(String input) throws Exception;
    }
    
    private static class IntegerValidator implements InputValidator {
        private final String fieldName;
        IntegerValidator(String fieldName) {
            this.fieldName = fieldName;
        }
        public void validate(String input) throws Exception {
            try {
                Integer.parseInt(input);
            } catch (NumberFormatException e) {
                throw new Exception(Util._t("Bad input")+ ":  \"" + input + "\"  " + Util._t(fieldName));    
            }
        }
    }
    
    private static class PositiveIntegerValidator implements InputValidator {
        private final String fieldName;
        PositiveIntegerValidator(String fieldName) {
            this.fieldName = fieldName;
        }
        
        public void validate(String input) throws Exception {
            try {
                int value = Integer.parseInt(input);
                if (value <= 0) 
                    throw new Exception(Util._t("Bad input") + " : \"" + Util._t(fieldName) + "\" " + Util._t("must be greater than zero"));
            } catch (NumberFormatException e) {
                throw new Exception(Util._t("Bad input")+ ":  \"" + input + "\"  " + Util._t(fieldName));    
            }
        }
    }
    
    private static class DirectoryValidator implements InputValidator {
        public void validate(String input) throws Exception {
            File f = new File(input);
            f = f.getCanonicalFile();
            if (!f.exists())
                throw new Exception(Util._t("Bad input") + " : " + Util._t("{0} does not exist",input));
            if (!f.isDirectory())
                throw new Exception(Util._t("Bad input") + " : " + Util._t("{0} is not a directory",input));
            if (!f.canWrite())
                throw new Exception(Util._t("Bad input") + " : " + Util._t("{0} not writeable",input));
        }
    }
    
    private static File getDirectory(String s) throws Exception {
        File f = new File(s);
        return f.getCanonicalFile();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        context = config.getServletContext();
        core = (Core) config.getServletContext().getAttribute("core");
    }

}
