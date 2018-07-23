package com.muwire.core

import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustService

import groovy.util.logging.Log

@Log
class Core {

	static main(args) {
		def home = System.getProperty("user.home") + File.separator + ".MuWire"
		home = new File(home)
		if (!home.exists()) {
			log.info("creating home dir")
			home.mkdir()
		}
		
		def props = new Properties()
		def propsFile = new File(home, "MuWire.properties")
		if (propsFile.exists()) {
			log.info("loading existing props file")
			propsFile.withInputStream {
				props.load(it)
			}
		} else
			log.info("creating default properties")
		
		props = new MuWireSettings(props)
		
		
		EventBus eventBus = new EventBus()
		
		log.info("initializing trust service")
		File goodTrust = new File(home, "trust.good")
		File badTrust = new File(home, "trust.bad")
		TrustService trustService = new TrustService(goodTrust, badTrust, 5000)
		eventBus.register(TrustEvent.class, trustService)
		trustService.start()
		trustService.waitForLoad()
		
	}

}
