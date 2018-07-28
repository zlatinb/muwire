package com.muwire.core.connection

import com.muwire.core.Event

import net.i2p.data.Destination

class DisconnectionEvent extends Event {
	
	Destination destination

	@Override
	public String toString() {
		"DisconnectionEvent ${super.toString()} destination:${destination.toBase32()}"
	}
}
