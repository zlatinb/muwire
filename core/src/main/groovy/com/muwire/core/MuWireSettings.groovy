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
    int downloadRetryInterval
    int totalUploadSlots
    int uploadSlotsPerUser
    int updateCheckInterval
    boolean autoDownloadUpdate
    String updateType
    String nickname
    File downloadLocation
    File incompleteLocation
    CrawlerResponse crawlerResponse
    boolean shareDownloadedFiles
    boolean shareHiddenFiles
    boolean searchComments
    boolean browseFiles
    Set<String> watchedDirectories
    float downloadSequentialRatio
    int hostClearInterval, hostHopelessInterval, hostRejectInterval
    int meshExpiration
    int speedSmoothSeconds
    boolean embeddedRouter
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
        updateCheckInterval = Integer.parseInt(props.getProperty("updateCheckInterval","24"))
        autoDownloadUpdate = Boolean.parseBoolean(props.getProperty("autoDownloadUpdate","true"))
        updateType = props.getProperty("updateType","jar")
        shareDownloadedFiles = Boolean.parseBoolean(props.getProperty("shareDownloadedFiles","true"))
        shareHiddenFiles = Boolean.parseBoolean(props.getProperty("shareHiddenFiles","false"))
        downloadSequentialRatio = Float.valueOf(props.getProperty("downloadSequentialRatio","0.8"))
        hostClearInterval = Integer.valueOf(props.getProperty("hostClearInterval","15"))
        hostHopelessInterval = Integer.valueOf(props.getProperty("hostHopelessInterval", "1440"))
        hostRejectInterval = Integer.valueOf(props.getProperty("hostRejectInterval", "1"))
        meshExpiration = Integer.valueOf(props.getProperty("meshExpiration","60"))
        embeddedRouter = Boolean.valueOf(props.getProperty("embeddedRouter","false"))
        inBw = Integer.valueOf(props.getProperty("inBw","256"))
        outBw = Integer.valueOf(props.getProperty("outBw","128"))
        searchComments = Boolean.valueOf(props.getProperty("searchComments","true"))
        browseFiles = Boolean.valueOf(props.getProperty("browseFiles","true"))
        speedSmoothSeconds = Integer.valueOf(props.getProperty("speedSmoothSeconds","60"))
        totalUploadSlots = Integer.valueOf(props.getProperty("totalUploadSlots","-1"))
        uploadSlotsPerUser = Integer.valueOf(props.getProperty("uploadSlotsPerUser","-1"))

        watchedDirectories = readEncodedSet(props, "watchedDirectories")
        watchedKeywords = readEncodedSet(props, "watchedKeywords")
        watchedRegexes = readEncodedSet(props, "watchedRegexes")
        negativeFileTree = readEncodedSet(props, "negativeFileTree")

        trustSubscriptions = new HashSet<>()
        if (props.containsKey("trustSubscriptions")) {
            props.getProperty("trustSubscriptions").split(",").each {
                trustSubscriptions.add(new Persona(new ByteArrayInputStream(Base64.decode(it))))
            }
        }
        
        
    }

    void write(OutputStream out) throws IOException {
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
        props.setProperty("updateCheckInterval", String.valueOf(updateCheckInterval))
        props.setProperty("autoDownloadUpdate", String.valueOf(autoDownloadUpdate))
        props.setProperty("updateType",String.valueOf(updateType))
        props.setProperty("shareDownloadedFiles", String.valueOf(shareDownloadedFiles))
        props.setProperty("shareHiddenFiles", String.valueOf(shareHiddenFiles))
        props.setProperty("downloadSequentialRatio", String.valueOf(downloadSequentialRatio))
        props.setProperty("hostClearInterval", String.valueOf(hostClearInterval))
        props.setProperty("hostHopelessInterval", String.valueOf(hostHopelessInterval))
        props.setProperty("hostRejectInterval", String.valueOf(hostRejectInterval))
        props.setProperty("meshExpiration", String.valueOf(meshExpiration))
        props.setProperty("embeddedRouter", String.valueOf(embeddedRouter))
        props.setProperty("inBw", String.valueOf(inBw))
        props.setProperty("outBw", String.valueOf(outBw))
        props.setProperty("searchComments", String.valueOf(searchComments))
        props.setProperty("browseFiles", String.valueOf(browseFiles))
        props.setProperty("speedSmoothSeconds", String.valueOf(speedSmoothSeconds))
        props.setProperty("totalUploadSlots", String.valueOf(totalUploadSlots))
        props.setProperty("uploadSlotsPerUser", String.valueOf(uploadSlotsPerUser))

        writeEncodedSet(watchedDirectories, "watchedDirectories", props)
        writeEncodedSet(watchedKeywords, "watchedKeywords", props)
        writeEncodedSet(watchedRegexes, "watchedRegexes", props)
        writeEncodedSet(negativeFileTree, "negativeFileTree", props)

        if (!trustSubscriptions.isEmpty()) {
            String encoded = trustSubscriptions.stream().
                map({it.toBase64()}).
                collect(Collectors.joining(","))
            props.setProperty("trustSubscriptions", encoded)
        }

        props.store(out, "")
    }
    
    private static Set<String> readEncodedSet(Properties props, String property) {
        Set<String> rv = new ConcurrentHashSet<>()
        if (props.containsKey(property)) {
            String[] encoded = props.getProperty(property).split(",")
            encoded.each { rv << DataUtil.readi18nString(Base64.decode(it)) }
        }
        rv
    }
    
    private static void writeEncodedSet(Set<String> set, String property, Properties props) {
        if (set.isEmpty())
            return
        String encoded = set.stream().
            map({Base64.encode(DataUtil.encodei18nString(it))}).
            collect(Collectors.joining(","))
        props.setProperty(property, encoded)
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
