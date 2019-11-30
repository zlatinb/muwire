package com.muwire.webui;

import java.io.File;

import com.muwire.core.Core;
import com.muwire.core.MuWireSettings;

class MWStarter extends Thread {
    private final MuWireSettings settings;
    private final File home;
    private final String version;
    private final MuWireClient client;
    
    MWStarter(MuWireSettings settings, File home, String version, MuWireClient client) {
        this.settings = settings;
        this.home = home;
        this.version = version;
        this.client = client;
        setName("MW starter");
        setDaemon(true);
    }
    
    public void run() {
        Core core = new Core(settings, home, version);
        core.startServices();
        client.setCore(core);
    }
}
