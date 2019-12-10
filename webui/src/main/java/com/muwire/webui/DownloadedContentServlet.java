package com.muwire.webui;

import java.io.File;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

public class DownloadedContentServlet extends BasicServlet {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        loadMimeMap("com/muwire/webui/mime");
    }
    
    /**
     *  Find the file for the hash.
     *
     *  @param pathInContext should always start with /
     *  @return file or null
     */
    @Override
    public File getResource(String pathInContext)
    {
        File r = null;
        // TODO
        return r;
    }
}
