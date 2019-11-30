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

public class MuWireClient {
    
    private final RouterContext ctx;
    private final String version;
    private final String home;
    private final File mwProps;
    
    private volatile Core core;
    
    public MuWireClient(RouterContext ctx, String home, String version) {
        this.ctx = ctx;
        this.version = version;
        this.home = home;
        this.mwProps = new File(home, "MuWire.properties");
    }

    public void start() throws Throwable {
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
    
    public void stop() throws Throwable {
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
    
    public String getHome() {
        return home;
    }
}
