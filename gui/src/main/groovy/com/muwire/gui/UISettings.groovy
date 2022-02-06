package com.muwire.gui

import com.muwire.core.util.DataUtil

import java.awt.Font

class UISettings {

    String lnf
    String locale
    boolean showMonitor
    String font
    boolean autoFontSize
    int fontSize, fontStyle
    int mainFrameX, mainFrameY
    boolean showUnsharedPaths
    boolean clearCancelledDownloads
    boolean clearFinishedDownloads
    boolean excludeLocalResult
    boolean showSearchHashes
    boolean closeWarning
    boolean collectionWarning
    boolean certificateWarning
    boolean exitOnClose
    boolean clearUploads
    boolean storeSearchHistory
    boolean groupByFile
    int maxChatLines
    Set<String> searchHistory
    Set<String> openTabs
    boolean messageNotifications
    
    UISettings(Properties props) {
        lnf = props.getProperty("lnf", "system")
        locale = props.getProperty("locale","us")
        showMonitor = Boolean.parseBoolean(props.getProperty("showMonitor", "false"))
        font = props.getProperty("font",null)
        clearCancelledDownloads = Boolean.parseBoolean(props.getProperty("clearCancelledDownloads","true"))
        clearFinishedDownloads = Boolean.parseBoolean(props.getProperty("clearFinishedDownloads","false"))
        excludeLocalResult = Boolean.parseBoolean(props.getProperty("excludeLocalResult","true"))
        showSearchHashes = Boolean.parseBoolean(props.getProperty("showSearchHashes","true"))
        autoFontSize = Boolean.parseBoolean(props.getProperty("autoFontSize","false"))
        fontSize = Integer.parseInt(props.getProperty("fontSize","12"))
        fontStyle = Integer.parseInt(props.getProperty("fontStyle", String.valueOf(Font.PLAIN)))
        closeWarning = Boolean.parseBoolean(props.getProperty("closeWarning","true"))
        collectionWarning = Boolean.parseBoolean(props.getProperty("collectionWarning", "true"))
        certificateWarning = Boolean.parseBoolean(props.getProperty("certificateWarning","true"))
        exitOnClose = Boolean.parseBoolean(props.getProperty("exitOnClose","false"))
        clearUploads = Boolean.parseBoolean(props.getProperty("clearUploads","false"))
        storeSearchHistory = Boolean.parseBoolean(props.getProperty("storeSearchHistory","true"))
        groupByFile = Boolean.parseBoolean(props.getProperty("groupByFile","false"))
        maxChatLines = Integer.parseInt(props.getProperty("maxChatLines","-1"))
        
        mainFrameX = Integer.parseInt(props.getProperty("mainFrameX","-1"))
        mainFrameY = Integer.parseInt(props.getProperty("mainFrameY","-1"))
        
        searchHistory = DataUtil.readEncodedSet(props, "searchHistory")
        openTabs = DataUtil.readEncodedSet(props, "openTabs")
        
        messageNotifications = Boolean.parseBoolean(props.getProperty("messageNotifications","true"))
        
        showUnsharedPaths = Boolean.parseBoolean(props.getProperty("showUnsharedPaths","false"))
    }

    void write(OutputStream out) throws IOException {
        Properties props = new Properties()
        props.setProperty("lnf", lnf)
        props.setProperty("locale", locale);
        props.setProperty("showMonitor", String.valueOf(showMonitor))
        props.setProperty("clearCancelledDownloads", String.valueOf(clearCancelledDownloads))
        props.setProperty("clearFinishedDownloads", String.valueOf(clearFinishedDownloads))
        props.setProperty("excludeLocalResult", String.valueOf(excludeLocalResult))
        props.setProperty("showSearchHashes", String.valueOf(showSearchHashes))
        props.setProperty("autoFontSize", String.valueOf(autoFontSize))
        props.setProperty("fontSize", String.valueOf(fontSize))
        props.setProperty("closeWarning", String.valueOf(closeWarning))
        props.setProperty("collectionWarning", String.valueOf(collectionWarning))
        props.setProperty("certificateWarning", String.valueOf(certificateWarning))
        props.setProperty("exitOnClose", String.valueOf(exitOnClose))
        props.setProperty("clearUploads", String.valueOf(clearUploads))
        props.setProperty("storeSearchHistory", String.valueOf(storeSearchHistory))
        props.setProperty("groupByFile", String.valueOf(groupByFile))
        props.setProperty("maxChatLines", String.valueOf(maxChatLines))
        props.setProperty("fontStyle", String.valueOf(fontStyle))
        if (font != null)
            props.setProperty("font", font)
            
        props.setProperty("mainFrameX", String.valueOf(mainFrameX))
        props.setProperty("mainFrameY", String.valueOf(mainFrameY))

        DataUtil.writeEncodedSet(searchHistory, "searchHistory", props)
        DataUtil.writeEncodedSet(openTabs, "openTabs", props)
        
        props.setProperty("messageNotifications", String.valueOf(messageNotifications))
        
        props.setProperty("showUnsharedPaths", String.valueOf(showUnsharedPaths))

        props.store(out, "UI Properties")
    }
}
