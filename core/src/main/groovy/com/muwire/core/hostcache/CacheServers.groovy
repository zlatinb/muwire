package com.muwire.core.hostcache

import net.i2p.data.Destination

class CacheServers {

	private static final int TO_GIVE = 3
	private static List<Destination> CACHES = [
			new Destination("KvwWPKMSAtzf7Yruj8TQaHi2jaQpSNsXJskbpmSBTxkcYlDB2GllH~QBu-cs4FSYdaRmKDUUx7793jjnYJgTMbrjqeIL5-BTORZ09n6PUfhSejDpJjdkUxaV1OHRatfYs70RNBv7rvdj1-nXUow5tMfOJtoWVocUoKefUGFQFbJLDDkBqjm1kFyKFZv6m6S6YqXxBgVB1qYicooy67cNQF5HLUFtP15pk5fMDNGz5eNCjPfC~2Gp8FF~OpSy92HT0XN7uAMJykPcbdnWfcvVwqD7eS0K4XEnsqnMPLEiMAhqsugEFiFqtB3Wmm7UHVc03lcAfRhr1e2uZBNFTtM2Uol4MD5sCCKRZVHGcH-WGPSEz0BM5YO~Xi~dQ~N3NVud32PVzhh8xoGcAlhTqMqAbRJndCv-H6NflX90pYmbirCTIDOaR9758mThrqX0d4CwCn4jFXer52l8Qe8CErGoLuB-4LL~Gwrn7R1k7ZQc2PthkqeW8MfigyiN7hZVkul9AAAA")
		]

	static List<Destination> getCacheServers() {
		List<Destination> allCaches = new ArrayList<>(CACHES)
		Collections.shuffle(allCaches)
		if (allCaches.size() <= TO_GIVE)
			return allCaches
		allCaches[0..TO_GIVE-1]
	}
}
