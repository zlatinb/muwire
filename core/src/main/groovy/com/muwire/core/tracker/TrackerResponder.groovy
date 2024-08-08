package com.muwire.core.tracker

import com.muwire.core.RouterConnectedEvent
import com.muwire.core.RouterDisconnectedEvent

import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.stream.Collectors

import com.muwire.core.Constants
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.download.DownloadManager
import com.muwire.core.download.Pieces
import com.muwire.core.files.FileManager
import com.muwire.core.mesh.Mesh
import com.muwire.core.mesh.MeshManager
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService
import com.muwire.core.util.DataUtil

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.SendMessageOptions
import net.i2p.client.datagram.I2PDatagramDissector
import net.i2p.client.datagram.I2PDatagramMaker
import net.i2p.data.Base64

@Log
class TrackerResponder {
    private final MuWireSettings muSettings
    private final FileManager fileManager
    private final DownloadManager downloadManager
    private final MeshManager meshManager
    private final TrustService trustService
    private final Persona me
    
    private final Map<UUID,Long> uuids = new HashMap<>()
    private final Timer expireTimer = new Timer("tracker-responder-timer", true)
    
    private static final long UUID_LIFETIME = 10 * 60 * 1000
    
    private volatile boolean shutdown
    
    private volatile I2PSession i2pSession
    private volatile boolean connected
    
    TrackerResponder(MuWireSettings muSettings,
        FileManager fileManager, DownloadManager downloadManager,
        MeshManager meshManager, TrustService trustService,
        Persona me) {
        this.muSettings = muSettings
        this.fileManager = fileManager
        this.downloadManager = downloadManager
        this.meshManager = meshManager
        this.trustService = trustService
        this.me = me
    }
    
    void start() {
        expireTimer.schedule({expireUUIDs()} as TimerTask, UUID_LIFETIME, UUID_LIFETIME)
    }
    
    void stop() {
        shutdown = true
        expireTimer.cancel()
    }
    
    void onRouterConnectedEvent(RouterConnectedEvent event) {
        connected = true
        if (i2pSession == null) {
            i2pSession = event.session
            i2pSession.addMuxedSessionListener(new Listener(), I2PSession.PROTO_DATAGRAM, Constants.TRACKER_PORT)
        }
    }
    
    void onRouterDisconnectedEvent(RouterDisconnectedEvent event) {
        connected = false
    }
    
    private void expireUUIDs() {
        final long now = System.currentTimeMillis()
        synchronized(uuids) {
            for (Iterator<UUID> iter = uuids.keySet().iterator(); iter.hasNext();) {
                UUID uuid = iter.next();
                Long time = uuids.get(uuid)
                if (now - time > UUID_LIFETIME)
                    iter.remove()
            }
        }
    }
    
    private void respond(host, json) {
        if (!connected)
            return
        I2PSession i2pSession = this.i2pSession
        if (i2pSession == null)
            return
        log.info("responding to host $host with json $json")
        
        def message = JsonOutput.toJson(json)
        def maker = new I2PDatagramMaker(i2pSession)
        message = maker.makeI2PDatagram(message.bytes)
        def options = new SendMessageOptions()
        options.setSendLeaseSet(false)
        i2pSession.sendMessage(host, message, 0, message.length, I2PSession.PROTO_DATAGRAM, Constants.TRACKER_PORT, Constants.TRACKER_PORT, options)
    }
    
    class Listener implements I2PSessionMuxedListener {

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size) {
        }

        @Override
        public void messageAvailable(I2PSession session, int msgId, long size, int proto, int fromport, int toport) {
            if (proto != I2PSession.PROTO_DATAGRAM) {
                log.warning "Received unexpected protocol $proto"
                return
            }
            
            byte[] payload = session.receiveMessage(msgId)
            def dissector = new I2PDatagramDissector()
            try {
                dissector.loadI2PDatagram(payload)
                def sender = dissector.getSender()
                
                log.info("got a tracker datagram from ${sender.toBase32()}")
                
                // if not trusted, just drop it
                TrustLevel trustLevel = trustService.getLevel(sender)
                
                if (trustLevel == TrustLevel.DISTRUSTED || 
                    (trustLevel == TrustLevel.NEUTRAL && !muSettings.allowUntrusted)) {
                    log.info("dropping, untrusted")
                    return
                }
                
                payload = dissector.getPayload()
                def slurper = new JsonSlurper()
                def json = slurper.parse(payload)
                
                if (json.type != "TrackerPing") {
                    log.warning("unknown type $json.type")
                    return
                }
                
                def response = [:]
                response.type = "TrackerPong"
                response.me = me.toBase64()

                if (json.infoHash == null) {
                    log.warning("infoHash missing")
                    return
                }
                
                if (json.uuid == null) {
                    log.warning("uuid missing")
                    return
                }
                
                UUID uuid = UUID.fromString(json.uuid)
                synchronized(uuids) {
                    if (uuids.containsKey(uuid)) {
                        log.warning("duplicate uuid $uuid")
                        return
                    }
                    uuids.put(uuid, System.currentTimeMillis())
                }
                response.uuid = json.uuid
                                
                if (!muSettings.allowTracking) {
                    response.code = 403
                    respond(sender, response)
                    return
                }
                
                if (json.version != 1) {
                    log.warning("unknown version $json.version")
                    response.code = 400
                    response.message = "I only support version 1"
                    respond(sender,response)
                    return
                }
                
                byte[] infoHashBytes = Base64.decode(json.infoHash)
                InfoHash infoHash = new InfoHash(infoHashBytes)
                
                log.info("servicing request for infoHash ${json.infoHash} with uuid ${json.uuid}")
                
                if (!(fileManager.isShared(infoHash) || downloadManager.isDownloading(infoHash))) {
                    response.code = 404
                    respond(sender, response)
                    return
                }

                Mesh mesh = meshManager.get(infoHash)   
                  
                if (fileManager.isShared(infoHash))
                    response.code = 200
                else if (mesh != null) {
                    response.code = 206
                    Pieces pieces = mesh.getPieces()
                    response.xHave = DataUtil.encodeXHave(pieces, pieces.getnPieces())
                }
                
                if (mesh != null)
                    response.altlocs = mesh.getRandom(10, me).stream().map({it.toBase64()}).collect(Collectors.toList())
                    
                respond(sender,response)
            } catch (Exception e) {
                log.log(Level.WARNING, "invalid datagram", e)
            }
        }

        @Override
        public void reportAbuse(I2PSession session, int severity) {
        }

        @Override
        public void disconnected(I2PSession session) {
            if (!shutdown)
                log.severe("Tracker Responder session disconnected")
        }

        @Override
        public void errorOccurred(I2PSession session, String message, Throwable error) {
            log.log(Level.SEVERE, message, error)
        }
        
    }
    
    
}
