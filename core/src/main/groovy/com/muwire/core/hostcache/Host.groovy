package com.muwire.core.hostcache

import net.i2p.data.Destination

class Host {

	private static final int MAX_FAILURES = 3
    private static final int CLEAR_INTERVAL = 60 * 60 * 1000
	
	final Destination destination
	int failures,successes
    long lastAttempt
	
	public Host(Destination destination) {
		this.destination = destination
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
        System.currentTimeMillis() - lastAttempt > CLEAR_INTERVAL
    }
}
