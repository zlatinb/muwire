package com.muwire.core.update

import java.util.logging.Level

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.download.UIDownloadEvent
import com.muwire.core.files.FileDownloadedEvent
import com.muwire.core.files.FileManager
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.util.DataUtil

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.client.I2PSession
import net.i2p.client.I2PSessionMuxedListener
import net.i2p.client.SendMessageOptions
import net.i2p.client.datagram.I2PDatagramDissector
import net.i2p.client.datagram.I2PDatagramMaker
import net.i2p.crypto.DSAEngine
import net.i2p.data.Base64
import net.i2p.data.Signature
import net.i2p.data.SigningPrivateKey
import net.i2p.util.VersionComparator

@Log
class UpdateClient {
    final EventBus eventBus
    final I2PSession session
    final String myVersion
    final MuWireSettings settings
    final FileManager fileManager
    final Persona me
    final SigningPrivateKey spk

    private final Timer timer

    private long lastUpdateCheckTime

    private volatile InfoHash updateInfoHash
    private volatile String version, signer
    private volatile boolean updateDownloading
    
    private volatile String text
    private volatile boolean shutdown

    UpdateClient(EventBus eventBus, I2PSession session, String myVersion, MuWireSettings settings, 
        FileManager fileManager, Persona me, SigningPrivateKey spk) {
        this.eventBus = eventBus
        this.session = session
        this.myVersion = myVersion
        this.settings = settings
        this.fileManager = fileManager
        this.me = me
        this.spk = spk
        this.lastUpdateCheckTime = settings.lastUpdateCheck
        timer = new Timer("update-client",true)
    }

    void start() {
        session.addMuxedSessionListener(new Listener(), I2PSession.PROTO_DATAGRAM, Constants.UPDATE_PORT)
        timer.schedule({checkUpdate()} as TimerTask, 60000, 60 * 60 * 1000)
    }

    void stop() {
        shutdown = true
        timer.cancel()
    }

    void onUIResultBatchEvent(UIResultBatchEvent results) {
        if (results.results[0].infohash != updateInfoHash)
            return
        if (updateDownloading)
            return
        updateDownloading = true
        def file = new File(settings.downloadLocation, results.results[0].name)
        def downloadEvent = new UIDownloadEvent(result: results.results[0], sources : results.results[0].sources, target : file)
        eventBus.publish(downloadEvent)
    }

    void onFileDownloadedEvent(FileDownloadedEvent e) {
        if (e.infoHash != updateInfoHash)
            return
        updateDownloading = false
        eventBus.publish(new UpdateDownloadedEvent(version : version, signer : signer, text : text))
        if (!settings.shareDownloadedFiles)
            eventBus.publish(new FileSharedEvent(file : e.downloadedFile))
    }

    private void checkUpdate() {
        final long now = System.currentTimeMillis()
        if (lastUpdateCheckTime > 0) {
            if (now - lastUpdateCheckTime < settings.updateCheckInterval * 60 * 60 * 1000)
                return
        }
        lastUpdateCheckTime = now
        settings.lastUpdateCheck = now

        log.info("checking for update")

        def ping = [version : 1, myVersion : myVersion]
        ping = JsonOutput.toJson(ping)
        def maker = new I2PDatagramMaker(session)
        ping = maker.makeI2PDatagram(ping.bytes)
        def options = new SendMessageOptions()
        options.setSendLeaseSet(true)
        session.sendMessage(UpdateServers.UPDATE_SERVER, ping, 0, ping.length, I2PSession.PROTO_DATAGRAM, Constants.UPDATE_PORT, 0, options)
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

                String infoHash
                if (settings.updateType == "jar") {
                    infoHash = payload.infoHash
                } else
                    infoHash = payload[settings.updateType]

                text = payload.text
                    
                if (!settings.autoDownloadUpdate) {
                    log.info("new version $payload.version available, publishing event")
                    eventBus.publish(new UpdateAvailableEvent(version : payload.version, signer : payload.signer, infoHash : infoHash, text : text))
                } else {
                    log.info("new version $payload.version available")
                    updateInfoHash = new InfoHash(Base64.decode(infoHash))
                    if (fileManager.rootToFiles.containsKey(updateInfoHash))
                        eventBus.publish(new UpdateDownloadedEvent(version : payload.version, signer : payload.signer, text : text))
                    else {
                        updateDownloading = false
                        version = payload.version
                        signer = payload.signer
                        log.info("starting search for new version hash $payload.infoHash")
                        Signature sig = DSAEngine.getInstance().sign(updateInfoHash.getRoot(), spk)
                        UUID uuid = UUID.randomUUID()
                        long timestamp = System.currentTimeMillis()
                        byte [] sig2 = DataUtil.signUUID(uuid, timestamp, spk)
                        def searchEvent = new SearchEvent(searchHash : updateInfoHash.getRoot(), uuid : uuid, oobInfohash : true, persona : me)
                        def queryEvent = new QueryEvent(searchEvent : searchEvent, firstHop : true, replyTo : me.destination,
                            receivedOn : me.destination, originator : me, sig : sig.data, queryTime : timestamp, sig2 : sig2)
                        eventBus.publish(queryEvent)
                    }
                }

            } catch (Exception e) {
                log.log(Level.WARNING,"Invalid datagram",e)
            }
        }

        @Override
        public void reportAbuse(I2PSession session, int severity) {
        }

        @Override
        public void disconnected(I2PSession session) {
            if (!shutdown)
                log.severe("I2P session disconnected")
        }

        @Override
        public void errorOccurred(I2PSession session, String message, Throwable error) {
            log.log(Level.SEVERE, message, error)
        }

    }
}
