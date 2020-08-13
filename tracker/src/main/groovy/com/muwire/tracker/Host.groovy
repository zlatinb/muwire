package com.muwire.tracker

import com.muwire.core.Persona

/**
 * A participant in a swarm.  The same persona can be a member of multiple
 * swarms, but in that case it would have multiple Host objects
 */
class Host {
    final Persona persona
    long lastPinged
    long lastResponded
    int failures
    volatile String xHave
    
    Host(Persona persona) {
        this.persona = persona
    }
    
    boolean isExpired(long cutoff, int maxFailures) {
        lastPinged > lastResponded && lastResponded <= cutoff && failures >= maxFailures 
    }

    @Override    
    public String toString() {
        "Host:[${persona.getHumanReadableName()} lastPinged:$lastPinged lastResponded:$lastResponded failures:$failures xHave:$xHave]"
    }
}
