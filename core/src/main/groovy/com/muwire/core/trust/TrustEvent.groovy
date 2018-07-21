package com.muwire.core.trust

import com.muwire.core.Event

import net.i2p.data.Destination

class TrustEvent extends Event {

	Destination destination
	TrustLevel level
}
