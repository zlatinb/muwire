package com.muwire.webui;

import com.muwire.core.Core;
import com.muwire.core.UILoadedEvent;

import net.i2p.I2PAppContext;
import net.i2p.router.RouterContext;

class MWStarter extends Thread {
    private final Core core;
    
    MWStarter(Core core) {
        this.core = core;
        setName("MW starter");
        setDaemon(true);
    }
    
    public void run() {
        RouterContext ctx = (RouterContext) I2PAppContext.getGlobalContext();
        
        while(!ctx.clientManager().isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        core.startServices();
        core.getEventBus().publish(new UILoadedEvent());
    }
}
