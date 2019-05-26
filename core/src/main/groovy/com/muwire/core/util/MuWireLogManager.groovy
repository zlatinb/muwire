package com.muwire.core.util

import net.i2p.I2PAppContext
import net.i2p.util.Log
import net.i2p.util.LogManager

class MuWireLogManager extends LogManager {
    
    MuWireLogManager() {
        super(I2PAppContext.getGlobalContext())
    }
    

    @Override
    public Log getLog(Class<?> cls, String name) {
        if (cls != null)
            return new JULLog(cls)
        new JULLog(name)
    }
    
}
