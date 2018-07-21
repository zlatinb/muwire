package com.muwire.core.search

import net.i2p.data.Destination

class QueryEvent extends SearchEvent {
	
	boolean firstHop
	Destination replyTo

}
