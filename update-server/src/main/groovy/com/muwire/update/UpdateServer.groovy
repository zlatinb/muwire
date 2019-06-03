package com.muwire.update

import java.util.logging.Level

import groovy.util.logging.Log
import net.i2p.client.I2PClientFactory
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.datagram.I2PDatagramDissector
import net.i2p.client.datagram.I2PDatagramMaker


@Log
class UpdateServer {
    public static void main(String[] args) {
        def home = System.getProperty("user.home") + "/.MuWireUpdateServer"
        home = new File(home)
        if (!home.exists())
            home.mkdirs()
        
        def keyFile = new File(home, "key.dat")
        
        def i2pClientFactory = new I2PClientFactory()
        def i2pClient = i2pClientFactory.createClient()
        
        def myDest
        def session
        if (!keyFile.exists()) {
            def os = new FileOutputStream(keyFile);
            myDest = i2pClient.createDestination(os)
            os.close()
            log.info "No key.dat file was found, so creating a new destination."
            log.info "This is the destination you want to give out for your new UpdateServer"
            log.info myDest.toBase64()
        }
        
        def update = new File(home, "update.json")
        if (!update.exists()) {
            log.warning("update file doesn't exist, exiting")
            System.exit(1)
        }
        
        def props = System.getProperties().clone()
        props.putAt("inbound.nickname", "MuWire UpdateServer")
        session = i2pClient.createSession(new FileInputStream(keyFile), props)
        myDest = session.getMyDestination()
        
        session.addMuxedSessionListener(new Listener(update), I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY)
        session.connect()
        log.info("Connected, going to sleep")
        Thread.sleep(Integer.MAX_VALUE)
        
    }
    
    static class Listener implements I2PSessionMuxedListener {
        
        private final File json
        
        Listener(File json) {
            this.json = json
        }

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size) {
        }

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size, int proto, int fromport, int toport) {
            if (proto != I2PSession.PROTO_DATAGRAM) {
                log.warning("received uknown protocol $proto")
                return
            }
            
            def payload = session.receiveMessage(msgId)
            def dissector = new I2PDatagramDissector()
            try {
                dissector.loadI2PDatagram(payload)
                def sender = dissector.getSender()
                log.info("Got an update ping from "+sender.toBase32())
                // I don't think we care about the payload at this point
                def maker = new I2PDatagramMaker(session)
                def response = maker.makeI2PDatagram(json.bytes)
                session.sendMessage(sender, response, I2PSession.PROTO_DATAGRAM, 0, 2)
            } catch (Exception e) {
                log.log(Level.WARNING, "exception responding to update request",e)
            }
        }

        @Override
        public void reportAbuse(I2PSession session, int severity) {
        }

        @Override
        public void disconnected(I2PSession session) {
            Log.severe("Disconnected from I2P router")
            System.exit(1)
        }

        @Override
        public void errorOccurred(I2PSession session, String message, Throwable error) {
            log.log(Level.SEVERE, message, error)
        }
        
    }
}
