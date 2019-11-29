package com.muwire.webui;

import net.i2p.app.ClientAppManager;
import net.i2p.app.ClientAppState;
import net.i2p.router.RouterContext;
import net.i2p.router.app.RouterApp;

public class MuWireClient implements RouterApp {
    
    private final RouterContext ctx;
    private final ClientAppManager mgr;
    private final String[] args;
    
    private ClientAppState state;
    
    public MuWireClient(RouterContext ctx, ClientAppManager mgr, String[]args) {
        this.ctx = ctx;
        this.mgr = mgr;
        this.args = args;
    }

    @Override
    public void startup() throws Throwable {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void shutdown(String[] args) throws Throwable {
        // TODO Auto-generated method stub
        
    }

    @Override
    public synchronized ClientAppState getState() {
        return state;
    }

    @Override
    public String getName() {
        return "MuWire";
    }

    @Override
    public String getDisplayName() {
        return "MuWire";
    }

}
