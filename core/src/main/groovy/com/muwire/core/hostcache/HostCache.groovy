package com.muwire.core.hostcache

import com.muwire.core.trust.TrustService

class HostCache {

	final TrustService trustService
	public HostCache(TrustService trustService) {
		this.trustService = trustService
	}

}
