package com.muwire.core.hostcache

import java.math.RoundingMode

import static com.muwire.core.connection.ConnectionAttemptStatus.*

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
    
    /**
     * @param current status of this host
     * @param nextStatus the next status of this host
     * @return probability of such status
     */
    private BigDecimal transition(def currentStatus, def nextStatus) {
        switch(currentStatus) {
            case SUCCESSFUL :
                switch(nextStatus) {
                    case SUCCESSFUL : return SS
                    case REJECTED : return SR
                    case FAILED : return SF
                }
            case REJECTED :
                switch(nextStatus) {
                    case SUCCESSFUL : return RS
                    case REJECTED : return RR
                    case FAILED : return RF
                }
            case FAILED :
                switch(nextStatus) {
                    case SUCCESSFUL : return FS
                    case REJECTED : return FR
                    case FAILED : return FF
                }
        }
    }
    
    /**
     * @param lastStatus last observed status
     * @param periods how many periods ago
     * @return probability of next status being SUCCESSFUL
     */
    public BigDecimal connectSuccessProbability(def lastStatus, int periods) {
        if (periods == 0) {
            return transition(lastStatus, SUCCESSFUL)
        } else {
            def toSuccess = transition(lastStatus, SUCCESSFUL)
            def toReject = transition(lastStatus, REJECTED)
            def toFail = transition(lastStatus, FAILED)
            final newPeriods = periods - 1
            BigDecimal rv = toSuccess * connectSuccessProbability(SUCCESSFUL, newPeriods) +
                toReject * connectSuccessProbability(REJECTED, newPeriods) +
                toFail * connectSuccessProbability(FAILED, newPeriods)
            rv.setScale(SCALE, MODE)
            return rv
        }
    }
}
