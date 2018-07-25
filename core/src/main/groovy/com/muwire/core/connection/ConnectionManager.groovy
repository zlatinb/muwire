package com.muwire.core.connection

import com.muwire.core.EventBus
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel

import net.i2p.data.Destination

abstract class ConnectionManager {

	final EventBus eventBus
	
	ConnectionManager(EventBus eventBus) {
		this.eventBus = eventBus
	}
	
	void onTrustEvent(TrustEvent e) {
		if (e.level == TrustLevel.DISTRUSTED)
			drop(e.destination)
	}
	
	abstract void drop(Destination d)
	
	abstract Collection<Connection> getConnections()
	
	protected abstract int getDesiredConnections()
	
	boolean needsConnections() {
		return getConnections().size() < getDesiredConnections()
	}
}
