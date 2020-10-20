package com.muwire.core.hostcache

import java.math.RoundingMode

import com.muwire.core.connection.ConnectionAttemptStatus

import groovy.sql.GroovyRowResult

/**
 * This is the Markov chain with 3 states and 3^2 transitions.
 * @author zab
 */
class HostMCProfile {
    
    private static final int SCALE = 10
    private static final RoundingMode MODE = RoundingMode.HALF_UP
    
    private final Probability[] S
    private final Probability[] R
    private final Probability[] F
    
    private final String toString
        
    // start with S
    ConnectionAttemptStatus state = ConnectionAttemptStatus.SUCCESSFUL
    
    /**
     * constructs an "optimistic" predictor for newly discovered hosts.
     */
    HostMCProfile() {
        S = new Probability[3]
        R = new Probability[3]
        F = new Probability[3]
        S[0] = new Probability(0.98, ConnectionAttemptStatus.SUCCESSFUL)
        S[1] = new Probability(0.01, ConnectionAttemptStatus.REJECTED)
        S[2] = new Probability(0.01, ConnectionAttemptStatus.FAILED)
        R[0] = new Probability(0.98, ConnectionAttemptStatus.SUCCESSFUL)
        R[1] = new Probability(0.01, ConnectionAttemptStatus.REJECTED)
        R[2] = new Probability(0.01, ConnectionAttemptStatus.FAILED)
        F[0] = new Probability(0.98, ConnectionAttemptStatus.SUCCESSFUL)
        F[1] = new Probability(0.01, ConnectionAttemptStatus.REJECTED)
        F[2] = new Probability(0.01, ConnectionAttemptStatus.FAILED)
        
        toString = "SS:${S[0].probability}," +
            "SR:${S[1].probability},"+
            "SF:${S[2].probability},"+
            "RS:${R[0].probability},"+
            "RR:${R[1].probability},"+
            "RF:${R[2].probability},"+
            "FS:${F[0].probability},"+
            "FR:${F[1].probability},"+
            "FF:${F[2].probability}"
        
        Arrays.sort(S)
        S[1].probability += S[0].probability
        S[2].probability += S[1].probability
        Arrays.sort(R)
        R[1].probability += R[0].probability
        R[2].probability += R[1].probability
        Arrays.sort(F)
        F[1].probability += F[0].probability
        F[2].probability += F[1].probability
        
    }
    
    /**
     * historical predictor loaded from database
     */
    HostMCProfile(def fromDB) {
        S = new Probability[3]
        R = new Probability[3]
        F = new Probability[3]

        BigDecimal bd
        
        bd = new BigDecimal(fromDB.SS)
        bd.setScale(SCALE, MODE)
        S[0] = new Probability(bd, ConnectionAttemptStatus.SUCCESSFUL)
        bd = new BigDecimal(fromDB.SR)
        bd.setScale(SCALE, MODE)
        S[1] = new Probability(bd, ConnectionAttemptStatus.REJECTED)
        bd = new BigDecimal(fromDB.SF)
        bd.setScale(SCALE, MODE)
        S[2] = new Probability(bd, ConnectionAttemptStatus.FAILED)
        
        bd = new BigDecimal(fromDB.RS)
        bd.setScale(SCALE, MODE)
        R[0] = new Probability(bd, ConnectionAttemptStatus.SUCCESSFUL)
        bd = new BigDecimal(fromDB.RR)
        bd.setScale(SCALE, MODE)
        R[1] = new Probability(bd, ConnectionAttemptStatus.REJECTED)
        bd = new BigDecimal(fromDB.RF)
        bd.setScale(SCALE, MODE)
        R[2] = new Probability(bd, ConnectionAttemptStatus.FAILED)
        
        bd = new BigDecimal(fromDB.FS)
        bd.setScale(SCALE, MODE)
        F[0] = new Probability(bd, ConnectionAttemptStatus.SUCCESSFUL)
        bd = new BigDecimal(fromDB.FR)
        bd.setScale(SCALE, MODE)
        F[1] = new Probability(bd, ConnectionAttemptStatus.REJECTED)
        bd = new BigDecimal(fromDB.FF)
        bd.setScale(SCALE, MODE)
        F[2] = new Probability(bd, ConnectionAttemptStatus.FAILED)
        
        toString = "SS:${S[0].probability}," +
        "SR:${S[1].probability},"+
        "SF:${S[2].probability},"+
        "RS:${R[0].probability},"+
        "RR:${R[1].probability},"+
        "RF:${R[2].probability},"+
        "FS:${F[0].probability},"+
        "FR:${F[1].probability},"+
        "FF:${F[2].probability}"
        
        Arrays.sort(S)
        S[1].probability += S[0].probability
        S[2].probability += S[1].probability
        Arrays.sort(R)
        R[1].probability += R[0].probability
        R[2].probability += R[1].probability
        Arrays.sort(F)
        F[1].probability += F[0].probability
        F[2].probability += F[1].probability
        
    }
    
    ConnectionAttemptStatus transition() {
        
        Probability[] lookup
        switch(state) {
            case ConnectionAttemptStatus.SUCCESSFUL :
                lookup = S
                break
            case ConnectionAttemptStatus.REJECTED :
                lookup = R
                break
            case ConnectionAttemptStatus.FAILED:
                lookup = F
                break
        }
        
        ConnectionAttemptStatus state
        final double random = Math.random()
        if (random < lookup[0].probability)
            state = lookup[0].state
        else if (random < lookup[1].probability)
            state = lookup[1].state
        else
            state = lookup[2].state
        
        this.state = state
        return state
    }

    @Override
    public String toString() {
        toString
    }
    
    private static class Probability implements Comparable<Probability> {
        
        private BigDecimal probability
        private final ConnectionAttemptStatus state
        
        Probability(BigDecimal probability, ConnectionAttemptStatus state) {
            this.probability = probability
            this.state = state
        }
        
        public int compareTo(Probability other) {
            if (probability == other.probability)
                return 0
            if (probability < other.probability)
                return -1
            return 1
        }
        
    }
}
