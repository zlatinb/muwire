package com.muwire.core.search

import com.muwire.core.Event

import net.i2p.data.Destination

class QueryEvent extends Event {
	
    SearchEvent searchEvent
	boolean firstHop
	Destination replyTo
	Destination receivedOn

}
