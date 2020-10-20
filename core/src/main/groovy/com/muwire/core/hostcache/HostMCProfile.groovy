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
    
    private final BigDecimal SS, SR, SF
    private final BigDecimal RS, RR, RF
    private final BigDecimal FS, FR, FF
    
    // start with S
    ConnectionAttemptStatus state = ConnectionAttemptStatus.SUCCESSFUL
    
    /**
     * constructs an "optimistic" predictor for newly discovered hosts.
     */
    HostMCProfile() {
        SS = 0.98
        SR = 0.01
        SF = 0.01
        RS = 0.98
        RR = 0.01
        RF = 0.01
        FS = 0.98
        FR = 0.01
        FF = 0.01
    }
    
    /**
     * historical predictor loaded from database
     */
    HostMCProfile(GroovyRowResult fromDB) {
        SS = new BigDecimal(fromDB.SS)
        SR = new BigDecimal(fromDB.SR)
        SF = new BigDecimal(fromDB.SF)
        RS = new BigDecimal(fromDB.RS)
        RR = new BigDecimal(fromDB.RR)
        RF = new BigDecimal(fromDB.RF)
        FS = new BigDecimal(fromDB.FS)
        FR = new BigDecimal(fromDB.FR)
        FF = new BigDecimal(fromDB.FF)
        setScale()
    }
    
    private void setScale() {
        SS.setScale(SCALE, MODE)
        SR.setScale(SCALE, MODE)
        SF.setScale(SCALE, MODE)
        RS.setScale(SCALE, MODE)
        RR.setScale(SCALE, MODE)
        RF.setScale(SCALE, MODE)
        FS.setScale(SCALE, MODE)
        FR.setScale(SCALE, MODE)
        FF.setScale(SCALE, MODE)
    }
    
    ConnectionAttemptStatus transition() {
        
        SortedMap<BigDecimal, ConnectionAttemptStatus> ignitionMap = new TreeMap<>()
        switch(state) {
            case ConnectionAttemptStatus.SUCCESSFUL :
                ignitionMap.put(SS, ConnectionAttemptStatus.SUCCESSFUL)
                ignitionMap.put(SR, ConnectionAttemptStatus.REJECTED)
                ignitionMap.put(SF, ConnectionAttemptStatus.FAILED)
                break
            case ConnectionAttemptStatus.REJECTED :
                ignitionMap.put(RS, ConnectionAttemptStatus.SUCCESSFUL)
                ignitionMap.put(RR, ConnectionAttemptStatus.REJECTED)
                ignitionMap.put(RF, ConnectionAttemptStatus.FAILED)
                break
            case ConnectionAttemptStatus.FAILED:
                ignitionMap.put(FS, ConnectionAttemptStatus.SUCCESSFUL)
                ignitionMap.put(FR, ConnectionAttemptStatus.REJECTED)
                ignitionMap.put(FF, ConnectionAttemptStatus.FAILED)
                break
        }
        
        BigDecimal[] probs = new BigDecimal[3]
        ConnectionAttemptStatus[] states = new ConnectionAttemptStatus[3]
        
        Iterator<BigDecimal> iter = ignitionMap.keySet().iterator()
        probs[0] = iter.next()
        states[0] = ignitionMap.get(probs[0])
        probs[1] = iter.next()
        states[1] = ignitionMap.get(probs[1])
        probs[2] = iter.next()
        states[2] = ignitionMap.get(probs[2])
        
        probs[1] += probs[0]
        probs[2] += probs[1]
        
        final double random = Math.random()
        if (random < probs[0]) 
            state = states[0]
        else if (random < probs[1])
            state = states[1]
        else
            state = states[2] 

        state
    }

}
