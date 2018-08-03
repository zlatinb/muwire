package com.muwire.core.search

import com.muwire.core.Event

import net.i2p.data.Base32
import net.i2p.data.Destination

class UpsertEvent extends Event {

	Set<String> names
	byte [] infoHash
	Destination leaf
	
	@Override
	public String toString() {
		"UpsertEvent ${super.toString()} names:$names infoHash:${Base32.encode(infoHash)} leaf:${leaf.toBase32()}"
	}
}
