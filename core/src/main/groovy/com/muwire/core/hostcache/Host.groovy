package com.muwire.core.hostcache

import net.i2p.data.Destination

class Host {

	private static final int MAX_FAILURES = 3
	
	final Destination destination
    private final int clearInterval
	int failures,successes
    long lastAttempt
	
	public Host(Destination destination, int clearInterval) {
		this.destination = destination
        this.clearInterval = clearInterval
	}

	synchronized void onConnect() {
		failures = 0
		successes++
        lastAttempt = System.currentTimeMillis()
	}
	
	synchronized void onFailure() {
		failures++
		successes = 0
        lastAttempt = System.currentTimeMillis()
	}
	
	synchronized boolean isFailed() {
		failures >= MAX_FAILURES
	}
	
	synchronized boolean hasSucceeded() {
		successes > 0
	}
    
    synchronized void clearFailures() {
        failures = 0
    }
    
    synchronized void canTryAgain() {
        System.currentTimeMillis() - lastAttempt > (clearInterval * 60 * 1000)
    }
}
