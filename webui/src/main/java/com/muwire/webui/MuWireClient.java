package com.muwire.webui;

import net.i2p.app.ClientAppManager;
import net.i2p.app.ClientAppState;
import net.i2p.router.RouterContext;
import net.i2p.router.app.RouterApp;

public class MuWireClient implements RouterApp {
    
    private final RouterContext ctx;
    private final ClientAppManager mgr;
    private final String[] args;
    private final String version;
    private final String home;
    
    private ClientAppState state;
    
    public MuWireClient(RouterContext ctx, ClientAppManager mgr, String[]args) {
        this.ctx = ctx;
        this.mgr = mgr;
        this.args = args;
        String version = null;
        String home = null;
        for (String arg : args) {
            String [] split = arg.split("=");
            if (split[0].equals("version"))
                version = split[1];
            else if (split[0].equals("home"))
                home = split[1];
        }
        this.version = version;
        this.home = home;
    }

    @Override
    public void startup() throws Throwable {
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
