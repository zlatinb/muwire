package com.muwire.gui

class UISettings {

    String lnf
    boolean showMonitor
    String font
    boolean autoFontSize
    int fontSize
    boolean clearCancelledDownloads
    boolean clearFinishedDownloads
    boolean excludeLocalResult
    boolean showSearchHashes
    
    UISettings(Properties props) {
        lnf = props.getProperty("lnf", "system")
        showMonitor = Boolean.parseBoolean(props.getProperty("showMonitor", "false"))
        font = props.getProperty("font",null)
        clearCancelledDownloads = Boolean.parseBoolean(props.getProperty("clearCancelledDownloads","true"))
        clearFinishedDownloads = Boolean.parseBoolean(props.getProperty("clearFinishedDownloads","false"))
        excludeLocalResult = Boolean.parseBoolean(props.getProperty("excludeLocalResult","true"))
        showSearchHashes = Boolean.parseBoolean(props.getProperty("showSearchHashes","true"))
        autoFontSize = Boolean.parseBoolean(props.getProperty("autoFontSize","false"))
        fontSize = Integer.parseInt(props.getProperty("fontSize","12"))
    }

    void write(OutputStream out) throws IOException {
        Properties props = new Properties()
        props.setProperty("lnf", lnf)
        props.setProperty("showMonitor", String.valueOf(showMonitor))
        props.setProperty("clearCancelledDownloads", String.valueOf(clearCancelledDownloads))
        props.setProperty("clearFinishedDownloads", String.valueOf(clearFinishedDownloads))
        props.setProperty("excludeLocalResult", String.valueOf(excludeLocalResult))
        props.setProperty("showSearchHashes", String.valueOf(showSearchHashes))
        props.setProperty("autoFontSize", String.valueOf(autoFontSize))
        props.setProperty("fontSize", String.valueOf(fontSize))
        if (font != null)
            props.setProperty("font", font)


        props.store(out, "UI Properties")
    }
}
