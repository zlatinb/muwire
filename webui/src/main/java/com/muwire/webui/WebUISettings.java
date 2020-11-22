package com.muwire.webui;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

public class WebUISettings extends Util {

    private File dropBoxLocation;
    
    
    public File getDropBoxLocation() {
        return dropBoxLocation;
    }

    public void setDropBoxLocation(File dropBoxLocation) {
        this.dropBoxLocation = dropBoxLocation;
    }

    WebUISettings() {
        this(new Properties());
    }
    
    WebUISettings(Properties props) {
        dropBoxLocation = new File(props.getProperty("dropBoxLocation", System.getProperty("user.home")));
    }
    
    void write(Writer out) throws IOException {
        Properties props = new Properties();
        
        props.setProperty("dropBoxLocation", dropBoxLocation.getAbsolutePath());
        
        props.store(out, "This file is UTF-8");
    }
}
