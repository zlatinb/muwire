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
    
    @Override
    public String toString() {
        "Host[b32:${destination.toBase32()} verifyTime:$verifyTime verificationFailures:$verificationFailures]"
    }
}
