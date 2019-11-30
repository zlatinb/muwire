package com.muwire.webui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.muwire.core.Core;
import com.muwire.core.MuWireSettings;

import net.i2p.app.ClientAppManager;
import net.i2p.app.ClientAppState;
import net.i2p.router.RouterContext;
import net.i2p.router.app.RouterApp;

public class MuWireClient implements RouterApp {
    
    private final RouterContext ctx;
    private final ClientAppManager mgr;
    private final String version;
    private final String home;
    private final File mwProps;
    
    private ClientAppState state;
    
    private volatile Core core;
    
    public MuWireClient(RouterContext ctx, ClientAppManager mgr, String[]args) {
        this.ctx = ctx;
        this.mgr = mgr;
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
        this.mwProps = new File(home, "MuWire.properties");
        this.state = ClientAppState.INITIALIZED;
        mgr.register(this);
    }

    @Override
    public void startup() throws Throwable {
        changeState(ClientAppState.STARTING, null, null);
        try {
            start();
            changeState(ClientAppState.RUNNING, null, null);
        } catch (Exception bad) {
            changeState(ClientAppState.START_FAILED, "Failed to start", bad);
            stop();
        }
    }

    @Override
    public void shutdown(String[] args) throws Throwable {
        if (state == ClientAppState.STOPPED)
            return;
        changeState(ClientAppState.STOPPING,null,null);
        stop();
        changeState(ClientAppState.STOPPED, null, null);
    }
    
    private synchronized void changeState(ClientAppState state, String msg, Exception e) {
        this.state = state;
        mgr.notify(this, state, msg, e);
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

    private void start() throws Throwable {
        if (needsMWInit())
            return;
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(mwProps), StandardCharsets.UTF_8));
        Properties props = new Properties();
        props.load(reader);
        reader.close();
        
        MuWireSettings settings = new MuWireSettings(props);
        core = new Core(settings, new File(home), version);
        core.startServices();
    }
    
    private void stop() throws Throwable {
        Core core = this.core;
        if (core == null)
            return;
        core.shutdown();
        this.core = null;
    }
    
    public boolean needsMWInit() {
        return !mwProps.exists();
    }
    
    public void initMWProps(String nickname, File downloadLocation, File incompleteLocation) throws Exception {
        if (!downloadLocation.exists())
            downloadLocation.mkdirs();
        if (!incompleteLocation.exists())
            incompleteLocation.mkdirs();
        
        MuWireSettings settings = new MuWireSettings();
        settings.setNickname(nickname);
        settings.setDownloadLocation(downloadLocation);
        settings.setIncompleteLocation(incompleteLocation);
        
        PrintWriter pw = new PrintWriter(mwProps, StandardCharsets.UTF_8);
        settings.write(pw);
        pw.close();
    }
    
    public Core getCore() {
        return core;
    }
}
