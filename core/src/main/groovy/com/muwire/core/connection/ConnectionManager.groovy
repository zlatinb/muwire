package com.muwire.core.connection

import com.muwire.core.EventBus

abstract class ConnectionManager {

	final EventBus eventBus
	
	ConnectionManager(EventBus eventBus) {
		this.eventBus = eventBus
	}
	
}
