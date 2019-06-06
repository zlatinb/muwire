package com.muwire.core.update

import java.util.logging.Level

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.SendMessageOptions
import net.i2p.client.datagram.I2PDatagramDissector
import net.i2p.client.datagram.I2PDatagramMaker
import net.i2p.util.VersionComparator

@Log
class UpdateClient {
    final EventBus eventBus
    final I2PSession session
    final String myVersion
    final MuWireSettings settings
    
    private final Timer timer
    
    private long lastUpdateCheckTime
    
    UpdateClient(EventBus eventBus, I2PSession session, String myVersion, MuWireSettings settings) {
        this.eventBus = eventBus
        this.session = session
        this.myVersion = myVersion
        this.settings = settings
        timer = new Timer("update-client",true)
    }
    
    void start() {
        session.addMuxedSessionListener(new Listener(), I2PSession.PROTO_DATAGRAM, 2)
        timer.schedule({checkUpdate()} as TimerTask, 30000, 60 * 60 * 1000)
    }
    
    void stop() {
        timer.cancel()
    }
    
    private void checkUpdate() {
        final long now = System.currentTimeMillis()
        if (lastUpdateCheckTime > 0) {
            if (now - lastUpdateCheckTime < settings.updateCheckInterval * 60 * 60 * 1000)
                return
        }
        lastUpdateCheckTime = now
    
        log.info("checking for update")
        
        def ping = [version : 1, myVersion : myVersion]
        ping = JsonOutput.toJson(ping)
        def maker = new I2PDatagramMaker(session)
        ping = maker.makeI2PDatagram(ping.bytes)
        def options = new SendMessageOptions() 
        options.setSendLeaseSet(true)
        session.sendMessage(UpdateServers.UPDATE_SERVER, ping, 0, ping.length, I2PSession.PROTO_DATAGRAM, 2, 0, options)   
    }
    
    class Listener implements I2PSessionMuxedListener {
        
        final JsonSlurper slurper = new JsonSlurper()

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size) {
        }

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size, int proto, int fromport, int toport) {
            if (proto != I2PSession.PROTO_DATAGRAM) {
                log.warning "Received unexpected protocol $proto"
                return
            }
            
            def payload = session.receiveMessage(msgId)
            def dissector = new I2PDatagramDissector()
            try {
                dissector.loadI2PDatagram(payload)
                def sender = dissector.getSender()
                if (sender != UpdateServers.UPDATE_SERVER) {
                    log.warning("received something not from update server " + sender.toBase32())
                    return
                }
                
                log.info("Received something from update server")
                
                payload = dissector.getPayload()
                payload = slurper.parse(payload)
                
                if (payload.version == null) {
                    log.warning("version missing")
                    return
                }
                
                if (payload.signer == null) {
                    log.warning("signer missing")
                }
                
                if (VersionComparator.comp(myVersion, payload.version) >= 0) {
                    log.info("no new version available")
                    return
                }
                
                log.info("new version $payload.version available, publishing event")
                eventBus.publish(new UpdateAvailableEvent(version : payload.version, signer : payload.signer, infoHash : payload.infoHash))
                
            } catch (Exception e) {
                log.log(Level.WARNING,"Invalid datagram",e)
            }
        }

        @Override
        public void reportAbuse(I2PSession session, int severity) {
        }

        @Override
        public void disconnected(I2PSession session) {
            log.severe("I2P session disconnected")
        }

        @Override
        public void errorOccurred(I2PSession session, String message, Throwable error) {
            log.log(Level.SEVERE, message, error)
        }
        
    }
}
