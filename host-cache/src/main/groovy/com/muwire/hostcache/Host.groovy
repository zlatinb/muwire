package com.muwire.hostcache

class Host {

    def destination
    def verifyTime
    boolean leafSlots
    boolean peerSlots
    int verificationFailures

    public int hashCode() {
        return destination.hashCode()
    }

    public boolean equals(other) {
        return destination.equals(other.destination)
    }
}
