package com.muwire.gui

class UISettings {
    
    String lnf
    boolean showMonitor
    String font
    
    UISettings(Properties props) {
        lnf = props.getProperty("lnf", "system")
        showMonitor = Boolean.parseBoolean(props.getProperty("showMonitor", "true"))
        font = props.getProperty("font",null)
    }
    
    void write(OutputStream out) throws IOException {
        Properties props = new Properties()
        props.setProperty("lnf", lnf)
        props.setProperty("showMonitor", showMonitor)
        if (font != null)
            props.setProperty("font", font)
        props.store(out, "UI Properties")
    }
}
