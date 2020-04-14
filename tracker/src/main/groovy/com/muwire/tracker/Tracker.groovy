package com.muwire.tracker

import com.muwire.core.MuWireSettings

class Tracker {
    
    private static final String VERSION = "0.6.12"
    
    public static void main(String [] args) {
        println "Launching MuWire Tracker version $VERSION"
        
        File home = new File(System.getProperty("user.home"))
        home = new File(home, ".mwtrackerd")
        home.mkdir()
        
        File mwProps = new File(home, "MuWire.properties")
        File i2pProps = new File(home, "i2p.properties")
        File trackerProps = new File(home, "tracker.properties")
        
        boolean launchSetup = false
        
        if (args.length > 0 && args[0] == "setup") {
            println "Setup requested, entering setup wizard"
            launchSetup = true
        } else if (!(mwProps.exists() && i2pProps.exists() && trackerProps.exists())) {
            println "Config files not found, entering setup wizard"
            launchSetup = true
        }
        
        if (launchSetup) {
            SetupWizard wizard = new SetupWizard(home)
            Properties props = wizard.performSetup()
            
            // nickname goes to mw.props
            MuWireSettings mwSettings = new MuWireSettings()
            mwSettings.nickname = props['nickname']
            
            mwProps.withPrintWriter("UTF-8", { 
                mwSettings.write(it)
            })
            
            // i2cp host & port go in i2p.properties
            def i2cpProps = new Properties()
            i2cpProps['i2cp.tcp.port'] = props['i2cp.tcp.port']
            i2cpProps['i2cp.tcp.host'] = props['i2cp.tcp.host']
            
            i2pProps.withPrintWriter { i2cpProps.store(it, "") }
            
            // json rcp props go in tracker.properties
            def jsonProps = new Properties()
            jsonProps['jsonrpc.iface'] = props['jsonrpc.iface']
            jsonProps['jsonrpc.port'] = props['jsonrpc.port']
            
            trackerProps.withPrintWriter { jsonProps.store(it, "") }
        }
    }
}
