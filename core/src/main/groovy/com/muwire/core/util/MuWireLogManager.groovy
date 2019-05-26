package com.muwire.core.util

import net.i2p.I2PAppContext
import net.i2p.util.Log
import net.i2p.util.LogManager

class MuWireLogManager extends LogManager {
    
    private static final Map<Class<?>, Log> classLogs = new HashMap<>()
    private static final Map<String, Log> stringLogs = new HashMap<>()
    
    MuWireLogManager() {
        super(I2PAppContext.getGlobalContext())
    }
    

    @Override
    public synchronized Log getLog(Class<?> cls, String name) {
        if (cls != null) {
            Log rv = classLogs.get(cls)
            if (rv == null) {
                rv = new JULLog(cls)
                classLogs.put(cls, rv)
            }
            return rv
        }
        
        Log rv = stringLogs.get(name)
        if (rv == null) {
            rv = new JULLog(name)
            stringLogs.put(name, rv)
        }
        rv
    }
    
}
