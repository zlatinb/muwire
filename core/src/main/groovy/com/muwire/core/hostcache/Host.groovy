package com.muwire.core.hostcache

import net.i2p.data.Destination

class Host {

    private static final int MAX_FAILURES = 3

    final Destination destination
    private final int clearInterval, hopelessInterval, rejectionInterval
    int failures,successes
    long lastAttempt
    long lastSuccessfulAttempt
    long lastRejection

    public Host(Destination destination, int clearInterval, int hopelessInterval, int rejectionInterval) {
        this.destination = destination
        this.clearInterval = clearInterval
        this.hopelessInterval = hopelessInterval
        this.rejectionInterval = rejectionInterval
    }
    
    private void connectSuccessful() {
        failures = 0
        successes++
        lastAttempt = System.currentTimeMillis()
    }

    synchronized void onConnect() {
        connectSuccessful()
        lastSuccessfulAttempt = lastAttempt
    }
    
    synchronized void onReject() {
        connectSuccessful()
        lastRejection = lastAttempt;
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

    synchronized boolean canTryAgain(final long now) {
        lastSuccessfulAttempt > 0 && 
            now - lastAttempt > (clearInterval * 60 * 1000)
    }
    
    synchronized boolean isHopeless(final long now) {
        isFailed() && 
            now - lastSuccessfulAttempt > (hopelessInterval * 60 * 1000)
    }
    
    synchronized boolean isRecentlyRejected(final long now) {
        now - lastRejection < (rejectionInterval * 60 * 1000)
    }
}
