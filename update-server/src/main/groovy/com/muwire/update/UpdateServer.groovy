package com.muwire.update

import net.i2p.data.Base64
import net.i2p.util.VersionComparator

import java.util.logging.Level

import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.client.I2PClientFactory
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.datagram.I2PDatagramDissector
import net.i2p.client.datagram.I2PDatagramMaker
import net.i2p.crypto.SigType


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
            myDest = i2pClient.createDestination(os, SigType.EdDSA_SHA512_Ed25519)
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

        byte [] json = loadJson(update)
        log.info("update.json sanity check passed")
        
        Properties props = System.getProperties().clone()
        props["inbound.nickname"] = "MuWire UpdateServer"
        def i2pPropsFile = new File(home, "i2p.properties")
        if (i2pPropsFile.exists()) {
            i2pPropsFile.withInputStream { props.load(it) }
        }
        session = i2pClient.createSession(new FileInputStream(keyFile), props)
        myDest = session.getMyDestination()

        session.addMuxedSessionListener(new Listener(json), I2PSession.PROTO_DATAGRAM, I2PSession.PORT_ANY)
        session.connect()
        log.info("Connected, going to sleep")
        while(true)
            Thread.sleep(Integer.MAX_VALUE)

    }
    
    /** also performs sanity checks */
    private static byte[] loadJson(File file) {
        // 1. Check if valid json
        JsonSlurper slurper = new JsonSlurper()
        def parsed = slurper.parse(file)
        
        // 2. check if version parses
        if (VersionComparator.comp("0.0.0", (String)parsed.version) >= 0)
            throw new Exception("version invalid ${parsed.version}")
        
        // 3. check string fields
        ["signer","text"].each { checkString(parsed[it])}
        
        // 4. check infohashes
        ["infoHash","exe","mac","appimage","appimage-aarch64"].each {checkInfoHash(parsed[it])}
        
        // 5. check beta
        if (parsed.beta != null) {
            
            // 1. Version must be an integer
            Integer.parseInt(parsed.beta.version)
            
            // 2. text fields
            ["signer","text"].each {checkString(parsed.beta[it])}
            
            // 3. infohashes
            ["exe","appimage","mac"].each {
                if (parsed.beta[it] != null)
                    checkInfoHash(parsed.beta[it])
            }
        }
        
        // if we got here we're good
        file.bytes
    }
    
    private static void checkString(def obj) {
        if (!(obj instanceof String))
            throw new Exception("object not a string")
    }
    
    private static void checkInfoHash(def obj) {
        String s = (String) obj
        byte [] decoded = Base64.decode(s)
        if (decoded.length != 32)
            throw new Exception("invalid infohash")
    }

    static class Listener implements I2PSessionMuxedListener {

        private final byte[] json
        private final def slurper = new JsonSlurper()
        Listener(byte[] json) {
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
                payload = slurper.parse(dissector.getPayload())
                log.info("Got an update ping from "+sender.toBase32() + " reported version "+payload?.myVersion)

                def maker = new I2PDatagramMaker(session)
                def response = maker.makeI2PDatagram(json)
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
            log.severe("Disconnected from I2P router")
            System.exit(1)
        }

        @Override
        public void errorOccurred(I2PSession session, String message, Throwable error) {
            log.log(Level.SEVERE, message, error)
        }

    }
}
