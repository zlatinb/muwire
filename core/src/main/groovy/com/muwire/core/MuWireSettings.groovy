package com.muwire.core

import java.util.stream.Collectors

import com.muwire.core.hostcache.CrawlerResponse
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64
import net.i2p.util.ConcurrentHashSet

class MuWireSettings {

    final boolean isLeaf
    boolean allowUntrusted
    boolean searchExtraHop
    boolean allowTrustLists
    int trustListInterval
    Set<Persona> trustSubscriptions
    int downloadRetryInterval, downloadMaxFailures
    int totalUploadSlots
    int uploadSlotsPerUser
    int updateCheckInterval
    long lastUpdateCheck
    boolean autoDownloadUpdate
    String updateType
    String nickname
    File downloadLocation
    File incompleteLocation
    CrawlerResponse crawlerResponse
    boolean shareDownloadedFiles
    boolean shareHiddenFiles
    boolean searchComments
    boolean searchCollections
    boolean browseFiles
    boolean allowTracking
    
    boolean fileFeed
    boolean advertiseFeed
    boolean autoPublishSharedFiles
    boolean defaultFeedAutoDownload
    long defaultFeedUpdateInterval
    int defaultFeedItemsToKeep
    boolean defaultFeedSequential
    
    int messageSendInterval
    
    int peerConnections
    int leafConnections
    int connectionHistory
    
    int responderCacheSize
    
    
    boolean startChatServer
    int maxChatConnections
    boolean advertiseChat
    File chatWelcomeFile
    
    boolean allowMessages
    boolean allowOnlyTrustedMessages
    
    Set<String> watchedDirectories
    float downloadSequentialRatio
    int hostClearInterval, hostHopelessInterval, hostRejectInterval, hostHopelessPurgeInterval
    int hostProfileHistory, minHostProfileHistory
    int meshExpiration
    int speedSmoothSeconds
    boolean embeddedRouter
    boolean plugin
    boolean disableUpdates
    int inBw, outBw
    Set<String> watchedKeywords
    Set<String> watchedRegexes
    Set<String> negativeFileTree

    MuWireSettings() {
        this(new Properties())
    }

    MuWireSettings(Properties props) {
        isLeaf = Boolean.valueOf(props.get("leaf","false"))
        allowUntrusted = Boolean.valueOf(props.getProperty("allowUntrusted","true"))
        searchExtraHop = Boolean.valueOf(props.getProperty("searchExtraHop","false"))
        allowTrustLists = Boolean.valueOf(props.getProperty("allowTrustLists","true"))
        trustListInterval = Integer.valueOf(props.getProperty("trustListInterval","1"))
        crawlerResponse = CrawlerResponse.valueOf(props.get("crawlerResponse","REGISTERED"))
        nickname = props.getProperty("nickname","MuWireUser")
        downloadLocation = new File((String)props.getProperty("downloadLocation",
            System.getProperty("user.home")))
        String incompleteLocationProp = props.getProperty("incompleteLocation")
        if (incompleteLocationProp != null)
            incompleteLocation = new File(incompleteLocationProp)
        downloadRetryInterval = Integer.parseInt(props.getProperty("downloadRetryInterval","60"))
        downloadMaxFailures = Integer.parseInt(props.getProperty("downloadMaxFailures","10"))
        updateCheckInterval = Integer.parseInt(props.getProperty("updateCheckInterval","24"))
        lastUpdateCheck = Long.parseLong(props.getProperty("lastUpdateChec","0"))
        autoDownloadUpdate = Boolean.parseBoolean(props.getProperty("autoDownloadUpdate","true"))
        updateType = props.getProperty("updateType","jar")
        shareDownloadedFiles = Boolean.parseBoolean(props.getProperty("shareDownloadedFiles","true"))
        shareHiddenFiles = Boolean.parseBoolean(props.getProperty("shareHiddenFiles","false"))
        downloadSequentialRatio = Float.valueOf(props.getProperty("downloadSequentialRatio","0.8"))
        hostClearInterval = Integer.valueOf(props.getProperty("hostClearInterval","15"))
        hostHopelessInterval = Integer.valueOf(props.getProperty("hostHopelessInterval", "60"))
        hostRejectInterval = Integer.valueOf(props.getProperty("hostRejectInterval", "1"))
        hostHopelessPurgeInterval = Integer.valueOf(props.getProperty("hostHopelessPurgeInterval","1440"))
        hostProfileHistory = Integer.valueOf(props.getProperty("hostProfileHistory","100"))
        minHostProfileHistory = Integer.valueOf(props.getProperty("minHostProfileHistory","5"))
        meshExpiration = Integer.valueOf(props.getProperty("meshExpiration","60"))
        embeddedRouter = Boolean.valueOf(props.getProperty("embeddedRouter","false"))
        plugin = Boolean.valueOf(props.getProperty("plugin","false"))
        disableUpdates = Boolean.valueOf(props.getProperty("disableUpdates","false"))
        inBw = Integer.valueOf(props.getProperty("inBw","256"))
        outBw = Integer.valueOf(props.getProperty("outBw","128"))
        searchComments = Boolean.valueOf(props.getProperty("searchComments","true"))
        searchCollections = Boolean.valueOf(props.getProperty("searchCollections","true"))
        browseFiles = Boolean.valueOf(props.getProperty("browseFiles","true"))
        allowTracking = Boolean.valueOf(props.getProperty("allowTracking","true"))
        
        // feed settings
        fileFeed = Boolean.valueOf(props.getProperty("fileFeed","true"))
        advertiseFeed = Boolean.valueOf(props.getProperty("advertiseFeed","true"))
        autoPublishSharedFiles = Boolean.valueOf(props.getProperty("autoPublishSharedFiles", "false"))
        defaultFeedAutoDownload = Boolean.valueOf(props.getProperty("defaultFeedAutoDownload", "false"))
        defaultFeedItemsToKeep = Integer.valueOf(props.getProperty("defaultFeedItemsToKeep", "1000"))
        defaultFeedSequential = Boolean.valueOf(props.getProperty("defaultFeedSequential", "false"))
        defaultFeedUpdateInterval = Long.valueOf(props.getProperty("defaultFeedUpdateInterval", "3600000"))
        
        // messenger settings
        messageSendInterval = Integer.valueOf(props.getProperty("messageSendInterval","1"))
        
        // ultrapeer connection settings
        leafConnections = Integer.valueOf(props.getProperty("leafConnections","512"))
        peerConnections = Integer.valueOf(props.getProperty("peerConnections","128"))
        
        // connection stats history
        connectionHistory = Integer.valueOf(props.getProperty("connectionHistory","1024"))
        
        // responder cache settings
        responderCacheSize = Integer.valueOf(props.getProperty("responderCacheSize","32"))
        
        speedSmoothSeconds = Integer.valueOf(props.getProperty("speedSmoothSeconds","10"))
        totalUploadSlots = Integer.valueOf(props.getProperty("totalUploadSlots","-1"))
        uploadSlotsPerUser = Integer.valueOf(props.getProperty("uploadSlotsPerUser","-1"))
        
        // chat settings
        startChatServer = Boolean.valueOf(props.getProperty("startChatServer","false"))
        maxChatConnections = Integer.valueOf(props.getProperty("maxChatConnections", "-1"))
        advertiseChat = Boolean.valueOf(props.getProperty("advertiseChat","true"))
        String chatWelcomeProp = props.getProperty("chatWelcomeFile")
        if (chatWelcomeProp != null)
            chatWelcomeFile = new File(chatWelcomeProp)
            
        // messaging settings
        allowMessages = Boolean.valueOf(props.getProperty("allowMessages","true"))
        allowOnlyTrustedMessages = Boolean.valueOf(props.getProperty("allowOnlyTrustedMessages","false"))
        
        watchedDirectories = DataUtil.readEncodedSet(props, "watchedDirectories")
        watchedKeywords = DataUtil.readEncodedSet(props, "watchedKeywords")
        watchedRegexes = DataUtil.readEncodedSet(props, "watchedRegexes")
        negativeFileTree = DataUtil.readEncodedSet(props, "negativeFileTree")

        trustSubscriptions = new HashSet<>()
        if (props.containsKey("trustSubscriptions")) {
            props.getProperty("trustSubscriptions").split(",").each {
                trustSubscriptions.add(new Persona(new ByteArrayInputStream(Base64.decode(it))))
            }
        }
        
        
    }

    void write(Writer out) throws IOException {
        Properties props = new Properties()
        props.setProperty("leaf", isLeaf.toString())
        props.setProperty("allowUntrusted", allowUntrusted.toString())
        props.setProperty("searchExtraHop", String.valueOf(searchExtraHop))
        props.setProperty("allowTrustLists", String.valueOf(allowTrustLists))
        props.setProperty("trustListInterval", String.valueOf(trustListInterval))
        props.setProperty("crawlerResponse", crawlerResponse.toString())
        props.setProperty("nickname", nickname)
        props.setProperty("downloadLocation", downloadLocation.getAbsolutePath())
        if (incompleteLocation != null)
            props.setProperty("incompleteLocation", incompleteLocation.getAbsolutePath())
        props.setProperty("downloadRetryInterval", String.valueOf(downloadRetryInterval))
        props.setProperty("downloadMaxFailures", String.valueOf(downloadMaxFailures))
        props.setProperty("updateCheckInterval", String.valueOf(updateCheckInterval))
        props.setProperty("lastUpdateCheck", String.valueOf(lastUpdateCheck))
        props.setProperty("autoDownloadUpdate", String.valueOf(autoDownloadUpdate))
        props.setProperty("updateType",String.valueOf(updateType))
        props.setProperty("shareDownloadedFiles", String.valueOf(shareDownloadedFiles))
        props.setProperty("shareHiddenFiles", String.valueOf(shareHiddenFiles))
        props.setProperty("downloadSequentialRatio", String.valueOf(downloadSequentialRatio))
        props.setProperty("hostClearInterval", String.valueOf(hostClearInterval))
        props.setProperty("hostHopelessInterval", String.valueOf(hostHopelessInterval))
        props.setProperty("hostRejectInterval", String.valueOf(hostRejectInterval))
        props.setProperty("hostHopelessPurgeInterval", String.valueOf(hostHopelessPurgeInterval))
        props.setProperty("hostProfileHistory", String.valueOf(hostProfileHistory))
        props.setProperty("minHostProfileHistory", String.valueOf(minHostProfileHistory))
        props.setProperty("meshExpiration", String.valueOf(meshExpiration))
        props.setProperty("embeddedRouter", String.valueOf(embeddedRouter))
        props.setProperty("plugin", String.valueOf(plugin))
        props.setProperty("disableUpdates", String.valueOf(disableUpdates))
        props.setProperty("inBw", String.valueOf(inBw))
        props.setProperty("outBw", String.valueOf(outBw))
        props.setProperty("searchComments", String.valueOf(searchComments))
        props.setProperty("searchCollections", String.valueOf(searchCollections))
        props.setProperty("browseFiles", String.valueOf(browseFiles))
        props.setProperty("allowTracking", String.valueOf(allowTracking))
        
        // feed settings
        props.setProperty("fileFeed", String.valueOf(fileFeed))
        props.setProperty("advertiseFeed", String.valueOf(advertiseFeed))
        props.setProperty("autoPublishSharedFiles", String.valueOf(autoPublishSharedFiles))
        props.setProperty("defaultFeedAutoDownload", String.valueOf(defaultFeedAutoDownload))
        props.setProperty("defaultFeedItemsToKeep", String.valueOf(defaultFeedItemsToKeep))
        props.setProperty("defaultFeedSequential", String.valueOf(defaultFeedSequential))
        props.setProperty("defaultFeedUpdateInterval", String.valueOf(defaultFeedUpdateInterval))
        
        // messenger settings
        props.setProperty("messageSendInterval", String.valueOf(messageSendInterval))
        
        // ultrapeer connection settings
        props.setProperty("peerConnections", String.valueOf(peerConnections))
        props.setProperty("leafConnections", String.valueOf(leafConnections))
        
        // connection history
        props.setProperty("connectionHistory", String.valueOf(connectionHistory))
        
        // responder cache settings
        props.setProperty("responderCacheSize", String.valueOf(responderCacheSize))
        
        props.setProperty("speedSmoothSeconds", String.valueOf(speedSmoothSeconds))
        props.setProperty("totalUploadSlots", String.valueOf(totalUploadSlots))
        props.setProperty("uploadSlotsPerUser", String.valueOf(uploadSlotsPerUser))
        
        // chat settings
        props.setProperty("startChatServer", String.valueOf(startChatServer))
        props.setProperty("maxChatConnectios", String.valueOf(maxChatConnections))
        props.setProperty("advertiseChat", String.valueOf(advertiseChat))
        if (chatWelcomeFile != null)
            props.setProperty("chatWelcomeFile", chatWelcomeFile.getAbsolutePath())
            
        // messaging settings
        props.setProperty("allowMessages", String.valueOf(allowMessages))
        props.setProperty("allowOnlyTrustedMessages", String.valueOf(allowOnlyTrustedMessages))
        

        DataUtil.writeEncodedSet(watchedDirectories, "watchedDirectories", props)
        DataUtil.writeEncodedSet(watchedKeywords, "watchedKeywords", props)
        DataUtil.writeEncodedSet(watchedRegexes, "watchedRegexes", props)
        DataUtil.writeEncodedSet(negativeFileTree, "negativeFileTree", props)

        if (!trustSubscriptions.isEmpty()) {
            String encoded = trustSubscriptions.stream().
                map({it.toBase64()}).
                collect(Collectors.joining(","))
            props.setProperty("trustSubscriptions", encoded)
        }

        props.store(out, "This file is UTF-8")
    }

    boolean isLeaf() {
        isLeaf
    }

    boolean allowUntrusted() {
        allowUntrusted
    }

    void setAllowUntrusted(boolean allowUntrusted) {
        this.allowUntrusted = allowUntrusted
    }

    CrawlerResponse getCrawlerResponse() {
        crawlerResponse
    }

    void setCrawlerResponse(CrawlerResponse crawlerResponse) {
        this.crawlerResponse = crawlerResponse
    }

    String getNickname() {
        nickname
    }
}
