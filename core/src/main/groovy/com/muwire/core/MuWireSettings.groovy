package com.muwire.core

import com.muwire.core.hostcache.CrawlerResponse

class MuWireSettings {
	
    final boolean isLeaf
    boolean allowUntrusted
    int downloadRetryInterval
    String nickname
    File downloadLocation
    String sharedFiles
    CrawlerResponse crawlerResponse
    
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
        sharedFiles = props.getProperty("sharedFiles")
        downloadRetryInterval = Integer.parseInt(props.getProperty("downloadRetryInterval","15"))
	}
    
    void write(OutputStream out) throws IOException {
        Properties props = new Properties()
        props.setProperty("leaf", isLeaf.toString())
        props.setProperty("allowUntrusted", allowUntrusted.toString())
        props.setProperty("crawlerResponse", crawlerResponse.toString())
        props.setProperty("nickname", nickname)
        props.setProperty("downloadLocation", downloadLocation.getAbsolutePath())
        props.setProperty("downloadRetryInterval", String.valueOf(downloadRetryInterval))
        if (sharedFiles != null)
            props.setProperty("sharedFiles", sharedFiles)
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
