package com.muwire.core.search

import net.i2p.data.Destination

class QueryEvent {
	
    SearchEvent searchEvent
	boolean firstHop
	Destination replyTo
	Destination receivedOn

}
