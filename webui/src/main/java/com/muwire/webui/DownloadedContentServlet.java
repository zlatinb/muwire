package com.muwire.webui;

import java.io.File;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.muwire.core.Core;
import com.muwire.core.InfoHash;
import com.muwire.core.SharedFile;

import net.i2p.data.Base64;

public class DownloadedContentServlet extends BasicServlet {

    private Core core;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        core = (Core) config.getServletContext().getAttribute("core");
        loadMimeMap("com/muwire/webui/mime");
    }
    
    /**
     *  Find the file for the hash.
     *
     *  @param pathInContext should always start with /
     *  @return file or null
     */
    @Override
    public File getResource(String pathInContext) {
        String infoHashB64 = pathInContext.substring("/DownloadedContent/".length());
        InfoHash infoHash = new InfoHash(Base64.decode(infoHashB64));
        SharedFile[] sharedFiles = core.getFileManager().getRootToFiles().get(infoHash);
        if (sharedFiles == null || sharedFiles.length == 0)
            return null;
        return sharedFiles[0].getFile();
    }
}
