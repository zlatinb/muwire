package com.muwire.tracker

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.muwire.core.Constants
import com.muwire.core.Core

import groovy.json.JsonOutput
import groovy.util.logging.Log
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.SendMessageOptions
import net.i2p.client.datagram.I2PDatagramMaker
import net.i2p.data.Base64

@Component
@Log
class Pinger {
    @Autowired
    private Core core
    
    @Autowired
    private SwarmManager swarmManager
    
    @Autowired
    private TrackerProperties trackerProperties
    
    private final Map<UUID, PingInProgress> inFlight = new ConcurrentHashMap<>()
    private final Timer expiryTimer = new Timer("pinger-timer",true)
    
    @PostConstruct
    private void registerListener() {
        core.getI2pSession().addMuxedSessionListener(new Listener(), I2PSession.PROTO_DATAGRAM, Constants.TRACKER_PORT)
        expiryTimer.schedule({expirePings()} as TimerTask, 1000, 1000)
    }
    
    private void expirePings() {
        final long now = System.currentTimeMillis()
        for(Iterator<UUID> iter = inFlight.keySet().iterator(); iter.hasNext();) {
            UUID uuid = iter.next()
            PingInProgress ping = inFlight.get(uuid) 
            if (now - ping.pingTime > trackerProperties.getSwarmParameters().getPingTimeout() * 1000L) {
                iter.remove()
                swarmManager.fail(ping.target)
            }
        }
    }
    
    void ping(SwarmManager.HostAndIH target, long now) {
        UUID uuid = UUID.randomUUID()
        def ping = new PingInProgress(target, now)
        inFlight.put(uuid, ping)
        
        def message = [:]
        message.type = "TrackerPing"
        message.version = 1
        message.infoHash = Base64.encode(target.getInfoHash().getRoot())
        message.uuid = uuid.toString()
        
        message = JsonOutput.toJson(message)
        def maker = new I2PDatagramMaker(core.getI2pSession())
        message = maker.makeI2PDatagram(message.bytes)
        def options = new SendMessageOptions()
        options.setSendLeaseSet(true)
        core.getI2pSession().sendMessage(target.getHost().getPersona().getDestination(), message, 0, message.length, I2PSession.PROTO_DATAGRAM,
            Constants.TRACKER_PORT, Constants.TRACKER_PORT, options)
    }
    
    private static class PingInProgress {
        private final SwarmManager.HostAndIH target
        private final long pingTime
        PingInProgress(SwarmManager.HostAndIH target, long pingTime) {
            this.target = target
            this.pingTime = pingTime
        }
    }
    
    private class Listener implements I2PSessionMuxedListener {

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size) {
        }

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size, int proto, int fromport, int toport) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void reportAbuse(I2PSession session, int severity) {
            log.warning("reportabuse $session $severity")
        }

        @Override
        public void disconnected(I2PSession session) {
            log.severe("disconnected")
        }

        @Override
        public void errorOccurred(I2PSession session, String message, Throwable error) {
            log.log(Level.SEVERE,message,error)
        }
        
    }
    
}
