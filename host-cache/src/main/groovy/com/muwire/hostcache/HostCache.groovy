package com.muwire.hostcache

import net.i2p.client.I2PClientFactory
import net.i2p.util.SystemVersion

public class HostCache {

    public static void main(String[] args) {
        if (SystemVersion.isWindows()) {
            println "This will likely not run on windows"
            System.exit(1)
        }
            
        def home = System.getProperty("user.home") + "/.MuWireHostCache"
        home = new File(home)
        if (home.exists() && !home.isDirectory()) {
            println "${home} exists but not a directory?  Delete it or make it a directory"
            System.exit(1)
        }
        
        if (!home.exists()) {
            home.mkdir()
        }
        
        def keyfile = new File(home, "key.dat")
        
        def i2pClientFactory = new I2PClientFactory()
        def i2pClient = i2pClientFactory.createClient()
        
        def myDest
        def session
        if (!keyfile.exists()) {
            def os = new FileOutputStream(keyfile);
            myDest = i2pClient.createDestination(os)
            os.close()
            println "No key.dat file was found, so creating a new destination."
            println "This is the destination you want to give out for your new HostCache"
            println myDest.toBase64()
        } 
        
        session = i2pClient.createSession(new FileInputStream(keyfile), System.getProperties())
        myDest = session.getMyDestination()
    }

}
