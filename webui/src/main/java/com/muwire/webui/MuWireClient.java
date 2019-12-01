package com.muwire.webui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.muwire.core.Core;
import com.muwire.core.MuWireSettings;
import com.muwire.core.connection.ConnectionEvent;
import com.muwire.core.connection.DisconnectionEvent;
import com.muwire.core.download.DownloadStartedEvent;
import com.muwire.core.search.UIResultBatchEvent;

import net.i2p.app.ClientAppManager;
import net.i2p.app.ClientAppState;
import net.i2p.router.RouterContext;
import net.i2p.router.app.RouterApp;
import net.i2p.util.Log;

public class MuWireClient {
    
    private final RouterContext ctx;
    private final ServletContext servletContext;
    private final String version;
    private final String home;
    private final File mwProps;
    
    private volatile Core core;
    
    public MuWireClient(RouterContext ctx, String home, String version, ServletContext servletContext) {
        this.ctx = ctx;
        this.servletContext = servletContext;
        this.version = version;
        this.home = home;
        this.mwProps = new File(home, "MuWire.properties");
    }

    public void start() throws Throwable {
        if (needsMWInit())
            return;
        
        Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        while(loggerNames.hasMoreElements()) {
            String name = loggerNames.nextElement();
            Logger logger = LogManager.getLogManager().getLogger(name);
            for (Handler h : logger.getHandlers()) {
                logger.removeHandler(h);
                h.close();
            }
            logger.addHandler(new I2PLogHandler(ctx));
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(mwProps), StandardCharsets.UTF_8));
        Properties props = new Properties();
        props.load(reader);
        reader.close();
        
        MuWireSettings settings = new MuWireSettings(props);
        MWStarter starter = new MWStarter(settings, new File(home), version, this);
        starter.start();
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
    
    public ServletContext getServletContext() {
        return servletContext;
    }
    
    void setCore(Core core) {
        this.core = core;
        servletContext.setAttribute("core", core);
        
        SearchManager searchManager = new SearchManager(core);
        core.getEventBus().register(UIResultBatchEvent.class, searchManager);
        
        DownloadManager downloadManager = new DownloadManager(core);
        core.getEventBus().register(DownloadStartedEvent.class, downloadManager);
        
        ConnectionCounter connectionCounter = new ConnectionCounter();
        core.getEventBus().register(ConnectionEvent.class, connectionCounter);
        core.getEventBus().register(DisconnectionEvent.class, connectionCounter);
        
        servletContext.setAttribute("searchManager", searchManager);
        servletContext.setAttribute("downloadManager", downloadManager);
        servletContext.setAttribute("connectionCounter", connectionCounter);
    }
    
    public String getHome() {
        return home;
    }
}
