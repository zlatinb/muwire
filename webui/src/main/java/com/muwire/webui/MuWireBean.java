package com.muwire.webui;

import java.io.Serializable;

import net.i2p.I2PAppContext;
import net.i2p.router.RouterContext;

public class MuWireBean implements Serializable {
   
    private final Object mwClient;
    
    public MuWireBean() {
        mwClient = ((RouterContext)I2PAppContext.getGlobalContext()).clientAppManager().getRegisteredApp("MuWire");
    }
    
    public Object getMwClient() {
        return mwClient;
    }
    
}
