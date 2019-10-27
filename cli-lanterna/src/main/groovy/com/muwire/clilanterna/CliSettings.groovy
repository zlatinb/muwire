package com.muwire.clilanterna

class CliSettings {
    
    boolean clearCancelledDownloads
    boolean clearFinishedDownloads
    boolean clearUploads
    
    CliSettings(Properties props) {
        clearCancelledDownloads = Boolean.parseBoolean(props.getProperty("clearCancelledDownloads","true"))
        clearFinishedDownloads = Boolean.parseBoolean(props.getProperty("clearFinishedDownloads", "false"))
        clearUploads = Boolean.parseBoolean(props.getProperty("clearUploads", "false"))
    }
    
    void write(OutputStream os) {
        Properties props = new Properties()
        props.with { 
            setProperty("clearCancelledDownloads", String.valueOf(clearCancelledDownloads))
            setProperty("clearFinishedDownloads", String.valueOf(clearFinishedDownloads))
            setProperty("clearUploads", String.valueOf(clearUploads))
            
            store(os, "CLI Properties")
        }
    }
}
