package com.muwire.core.search

import com.muwire.core.Event
import com.muwire.core.Persona

import net.i2p.data.Destination

class QueryEvent extends Event {
	
    SearchEvent searchEvent
	boolean firstHop
	Destination replyTo
    Persona originator
	Destination receivedOn

}
