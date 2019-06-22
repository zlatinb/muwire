package com.muwire.core

import java.util.stream.Collectors

import com.muwire.core.hostcache.CrawlerResponse
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64

class MuWireSettings {
	
    final boolean isLeaf
    boolean allowUntrusted
    int downloadRetryInterval
    int updateCheckInterval
    String nickname
    File downloadLocation
    CrawlerResponse crawlerResponse
    boolean shareDownloadedFiles
    Set<String> watchedDirectories
    
	MuWireSettings() {
        this(new Properties())
    }
	
	MuWireSettings(Properties props) {
		isLeaf = Boolean.valueOf(props.get("leaf","false"))
		allowUntrusted = Boolean.valueOf(props.get("allowUntrusted","true"))
		crawlerResponse = CrawlerResponse.valueOf(props.get("crawlerResponse","REGISTERED"))
        nickname = props.getProperty("nickname","MuWireUser")
        downloadLocation = new File((String)props.getProperty("downloadLocation", 
            System.getProperty("user.home")))
        downloadRetryInterval = Integer.parseInt(props.getProperty("downloadRetryInterval","1"))
        updateCheckInterval = Integer.parseInt(props.getProperty("updateCheckInterval","24"))
        shareDownloadedFiles = Boolean.parseBoolean(props.getProperty("shareDownloadedFiles","true"))
        
        watchedDirectories = new HashSet<>()
        if (props.containsKey("watchedDirectories")) {
            String[] encoded = props.getProperty("watchedDirectories").split(",")
            encoded.each { watchedDirectories << DataUtil.readi18nString(Base64.decode(it)) }
        }
        
	}
    
    void write(OutputStream out) throws IOException {
        Properties props = new Properties()
        props.setProperty("leaf", isLeaf.toString())
        props.setProperty("allowUntrusted", allowUntrusted.toString())
        props.setProperty("crawlerResponse", crawlerResponse.toString())
        props.setProperty("nickname", nickname)
        props.setProperty("downloadLocation", downloadLocation.getAbsolutePath())
        props.setProperty("downloadRetryInterval", String.valueOf(downloadRetryInterval))
        props.setProperty("updateCheckInterval", String.valueOf(updateCheckInterval))
        props.setProperty("shareDownloadedFiles", String.valueOf(shareDownloadedFiles))
        
        if (!watchedDirectories.isEmpty()) {
            String encoded = watchedDirectories.stream().
                map({Base64.encode(DataUtil.encodei18nString(it))}).
                collect(Collectors.joining(","))
            props.setProperty("watchedDirectories", encoded)
        }
        
        props.store(out, "")
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
