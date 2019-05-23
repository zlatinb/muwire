package com.muwire.core

import com.muwire.core.hostcache.CrawlerResponse

class MuWireSettings {
	
    final boolean isLeaf
    boolean allowUntrusted
    String nickname
    CrawlerResponse crawlerResponse
    
	MuWireSettings() {
        this(new Properties())
    }
	
	MuWireSettings(Properties props) {
		isLeaf = Boolean.valueOf(props.get("leaf","false"))
		allowUntrusted = Boolean.valueOf(props.get("allowUntrusted","true"))
		crawlerResponse = CrawlerResponse.valueOf(props.get("crawlerResponse","REGISTERED"))
        nickname = props.getProperty("nickname","MuWireUser")
	}
    
    void write(OutputStream out) throws IOException {
        Properties props = new Properties()
        props.setProperty("leaf", isLeaf.toString())
        props.setProperty("allowUntrusted", allowUntrusted.toString())
        props.setProperty("crawlerResponse", crawlerResponse.toString())
        props.setProperty("nickname", nickname)
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
