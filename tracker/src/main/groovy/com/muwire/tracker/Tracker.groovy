package com.muwire.tracker

import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import com.github.arteam.simplejsonrpc.server.JsonRpcServer
import com.muwire.core.Core
import com.muwire.core.MuWireSettings
import com.muwire.core.UILoadedEvent
import com.muwire.core.files.AllFilesLoadedEvent

class Tracker {
    
    private static final String VERSION = "0.6.12"
    
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool()
    
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
            i2cpProps['inbound.nickname'] = "MuWire Tracker"
            i2cpProps['outbound.nickname'] = "MuWire Tracker"
            
            i2pProps.withPrintWriter { i2cpProps.store(it, "") }
            
            // json rcp props go in tracker.properties
            def jsonProps = new Properties()
            jsonProps['jsonrpc.iface'] = props['jsonrpc.iface']
            jsonProps['jsonrpc.port'] = props['jsonrpc.port']
            
            trackerProps.withPrintWriter { jsonProps.store(it, "") }
        }
        
        Properties p = new Properties()
        mwProps.withReader("UTF-8", { p.load(it) } )
        MuWireSettings muSettings = new MuWireSettings(p)
        p = new Properties()
        trackerProps.withInputStream { p.load(it) }
        
        InetAddress toBind = InetAddress.getByName(p['jsonrpc.iface'])
        int port = Integer.parseInt(p['jsonrpc.port'])
        ServerSocket ss = new ServerSocket(port, Integer.MAX_VALUE, toBind)
        println "json rpc listening on $toBind:$port"
        JsonRpcServer rpcServer = new JsonRpcServer()
        
        Core core = new Core(muSettings, home, VERSION)
        

        // init json service object
        TrackerService trackerService = new TrackerService()
        core.eventBus.with { 
            register(UILoadedEvent.class, trackerService)
        }
                
        Thread coreStarter = new Thread({ 
            core.startServices()
            core.eventBus.publish(new UILoadedEvent()) 
        } as Runnable)
        coreStarter.start()
        
        
        try {
            while(true) {
                Socket s = ss.accept()
                println "accepted connection from " + s.getInetAddress()
                EXECUTOR_SERVICE.submit {
                    try {
                        def reader = new BufferedReader(new InputStreamReader(s.getInputStream()))
                        String request;
                        while((request = reader.readLine()) != null) {
                            println "got request \"$request\""
                            String response = rpcServer.handle(request, trackerService)
                            println "sending response \"$response\""
                            s.getOutputStream().newWriter("UTF-8").write(response)
                            s.getOutputStream().write("\n".getBytes(StandardCharsets.US_ASCII))
                            s.getOutputStream().flush()
                        }
                    } catch (Exception bad) {
                        bad.printStackTrace()
                    } finally {
                        s.close()
                    }
                } as Runnable
            }
        } catch (Exception bad) {
            bad.printStackTrace()
        }
        
    }
}
