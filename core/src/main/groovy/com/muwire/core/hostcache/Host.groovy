package com.muwire.core.hostcache

import net.i2p.data.Destination

class Host {

    private static final int MAX_FAILURES = 3

    final Destination destination
    private final int clearInterval, hopelessInterval
    int failures,successes
    long lastAttempt
    long lastSuccessfulAttempt

    public Host(Destination destination, int clearInterval, int hopelessInterval) {
        this.destination = destination
        this.clearInterval = clearInterval
        this.hopelessInterval = hopelessInterval
    }

    synchronized void onConnect() {
        failures = 0
        successes++
        lastAttempt = System.currentTimeMillis()
        lastSuccessfulAttempt = lastAttempt
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
        lastSuccessfulAttempt > 0 && 
            System.currentTimeMillis() - lastAttempt > (clearInterval * 60 * 1000)
    }
    
    synchronized void isHopeless() {
        isFailed() && 
            System.currentTimeMillis() - lastSuccessfulAttempt > (hopelessInterval * 60 * 1000)
    }
}
