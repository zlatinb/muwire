package com.muwire.hostcache

import net.i2p.client.I2PClientFactory
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.datagram.I2PDatagramDissector
import net.i2p.util.SystemVersion
import net.i2p.data.*

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
        
        def props = System.getProperties().clone()
        props.putAt("inbound.nickname", "MuWire HostCache")
        session = i2pClient.createSession(new FileInputStream(keyfile), props)
        myDest = session.getMyDestination()
        
        session.addMuxedSessionListener(new Listener(), 
            I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY)
        session.connect()
        println "INFO: connected, going to sleep"
        Thread.sleep(Integer.MAX_VALUE)
        
    }
    
    static class Listener implements I2PSessionMuxedListener {
        void reportAbuse(I2PSession sesison, int severity) {}
        void disconnected(I2PSession session) {
            println "ERROR: session disconnected, exiting"
            System.exit(1)
        }
        void errorOccurred(I2PSession session, String message, Throwable error) {
            println "ERROR: ${message} ${error}"
        }
        void messageAvailable(I2PSession session, int msgId, long size, int proto,
            int fromport, int toport) {
            if (proto != I2PSession.PROTO_DATAGRAM) {
                println "WARN: received unexpected protocol ${proto}"
                return
            }
            
            def payload = session.receiveMessage(msgId)
            def dissector = new I2PDatagramDissector()
            try {
                dissector.loadI2PDatagram(payload)
                def sender = dissector.getSender()
                def query = dissector.getPayload()
                println "INFO: from ${sender.toBase64()} received query ${query}"
            } catch (DataFormatException dfe) {
                println "WARN: invalid datagram ${dfe}"
            }
        }
        void messageAvailable(I2PSession session, int msgId, long size) {
        }
    }
}
