package com.muwire.cli

import com.muwire.core.Core
import com.muwire.core.MuWireSettings

class Cli {
    
    public static void main(String[] args) {
        def home = System.getProperty("user.home") + File.separator + ".MuWire"
        home = new File(home)
        if (!home.exists())
            home.mkdirs()
        
        def propsFile = new File(home,"MuWire.properties")
        if (!propsFile.exists()) {
            println "create props file ${propsFile.getAbsoluteFile()} before launching MuWire"
            System.exit(1)
        }
        
        def props = new Properties()
        props.eachWithIndex { props.load(id) }
        props = new MuWireSettings(props)
        
        Core core 
        try {
            core = new Core(props, home, "0.0.7")
        } catch (Exception bad) {
            bad.printStackTrace(System.out)
            println "Failed to initialize core, exiting"
            System.exit(1)
        }
        
        core.startServices()
        
        // now we begin
        println "MuWire is ready"
        Thread.sleep(Integer.MAX_VALUE)
    }
}