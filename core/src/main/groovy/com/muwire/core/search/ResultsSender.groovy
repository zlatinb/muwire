package com.muwire.core.search

import com.muwire.core.SharedFile
import com.muwire.core.chat.ChatServer
import com.muwire.core.collections.CollectionManager
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.filecert.CertificateManager
import com.muwire.core.files.FileHasher
import com.muwire.core.util.DataUtil
import com.muwire.core.Persona

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.stream.Collectors
import java.util.zip.GZIPOutputStream

import com.muwire.core.DownloadedFile
import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings

import groovy.json.JsonOutput
import groovy.util.logging.Log
import net.i2p.data.Base64
import net.i2p.data.Destination

@Log
class ResultsSender {

    private static final AtomicInteger THREAD_NO = new AtomicInteger()

    private final Executor executor = Executors.newCachedThreadPool(
        new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread rv = new Thread(r)
                rv.setName("Results Sender "+THREAD_NO.incrementAndGet())
                rv.setDaemon(true)
                rv
            }
        })

    private final I2PConnector connector
    private final Persona me
    private final EventBus eventBus
    private final MuWireSettings settings
    private final CertificateManager certificateManager
    private final ChatServer chatServer
    private final CollectionManager collectionManager

    ResultsSender(EventBus eventBus, I2PConnector connector, Persona me, MuWireSettings settings, 
        CertificateManager certificateManager, ChatServer chatServer, CollectionManager collectionManager) {
        this.connector = connector;
        this.eventBus = eventBus
        this.me = me
        this.settings = settings
        this.certificateManager = certificateManager
        this.chatServer = chatServer
        this.collectionManager = collectionManager
    }

    void sendResults(UUID uuid, SharedFile[] results, Destination target, boolean oobInfohash, boolean compressedResults) {
        log.info("Sending $results.length results for uuid $uuid to ${target.toBase32()} oobInfohash : $oobInfohash")
        if (target.equals(me.destination)) {
            def uiResultEvents = []
            results.each {
                long length = it.getFile().length()
                int pieceSize = it.getPieceSize()
                if (pieceSize == 0)
                    pieceSize = FileHasher.getPieceSize(length)
                Set<Destination> suggested = Collections.emptySet()
                if (it instanceof DownloadedFile)
                    suggested = it.sources
                def comment = null
                if (it.getComment() != null) {
                    comment = DataUtil.readi18nString(Base64.decode(it.getComment()))
                }
                InfoHash ih = new InfoHash(it.getRoot())
                int certificates = certificateManager.getByInfoHash(ih).size()
                def uiResultEvent = new UIResultEvent( sender : me,
                    name : it.getFile().getName(),
                    size : length,
                    infohash : ih,
                    pieceSize : pieceSize,
                    uuid : uuid,
                    browse : settings.browseFiles,
                    sources : suggested,
                    comment : comment,
                    certificates : certificates,
                    chat : chatServer.isRunning() && settings.advertiseChat,
                    feed : settings.fileFeed && settings.advertiseFeed,
                    collections : collectionManager.collectionsForFile(ih)
                    )
                uiResultEvents << uiResultEvent
            }
            eventBus.publish(new UIResultBatchEvent(uuid : uuid, results : uiResultEvents))
        } else {
            executor.execute(new ResultSendJob(uuid : uuid, results : results,
                target: target, oobInfohash : oobInfohash, compressedResults : compressedResults))
        }
    }

    private class ResultSendJob implements Runnable {
        UUID uuid
        SharedFile [] results
        Destination target
        boolean oobInfohash
        boolean compressedResults

        @Override
        public void run() {
            try {
                JsonOutput jsonOutput = new JsonOutput()
                Endpoint endpoint = null;
                if (!compressedResults) { // TODO: deprecate this completely
                    try {
                        endpoint = connector.connect(target)
                        DataOutputStream os = new DataOutputStream(endpoint.getOutputStream())
                        os.write("POST $uuid\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                        me.write(os)
                        os.writeShort((short)results.length)
                        results.each {
                            InfoHash ih = new InfoHash(it.getRoot())
                            int certificates = certificateManager.getByInfoHash(ih).size()
                            int collections = collectionManager.collectionsForFile(ih)
                            def obj = sharedFileToObj(it, settings.browseFiles, certificates, collections)
                            def json = jsonOutput.toJson(obj)
                            os.writeShort((short)json.length())
                            os.write(json.getBytes(StandardCharsets.US_ASCII))
                        }
                        os.flush()
                    } finally {
                        endpoint?.close()
                    }
                } else {
                    DataOutputStream dos = null
                    try {
                        endpoint = connector.connect(target)
                        OutputStream os = endpoint.getOutputStream()
                        os.write("RESULTS $uuid\r\n".getBytes(StandardCharsets.US_ASCII))
                        os.write("Sender: ${me.toBase64()}\r\n".getBytes(StandardCharsets.US_ASCII))
                        os.write("Count: $results.length\r\n".getBytes(StandardCharsets.US_ASCII))
                        boolean chat = chatServer.isRunning() && settings.advertiseChat
                        os.write("Chat: $chat\r\n".getBytes(StandardCharsets.US_ASCII))
                        boolean feed = settings.fileFeed && settings.advertiseFeed
                        os.write("Feed: $feed\r\n".getBytes(StandardCharsets.US_ASCII))
                        os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
                        dos = new DataOutputStream(new GZIPOutputStream(os))
                        results.each { 
                            InfoHash ih = new InfoHash(it.getRoot())
                            int certificates = certificateManager.getByInfoHash(ih).size()
                            int collections = collectionManager.collectionsForFile(ih)
                            def obj = sharedFileToObj(it, settings.browseFiles, certificates, collections)
                            def json = jsonOutput.toJson(obj)
                            dos.writeShort((short)json.length())
                            dos.write(json.getBytes(StandardCharsets.US_ASCII))
                        }
                    } finally {
                        try {
                            dos?.close()
                        } catch (Exception ignore) {}
                        endpoint?.close()
                    }
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "problem sending results",e)
            }
        }
    }
    
    public static def sharedFileToObj(SharedFile sf, boolean browseFiles, int certificates, int collections) {
        byte [] name = sf.getFile().getName().getBytes(StandardCharsets.UTF_8)
        def baos = new ByteArrayOutputStream()
        def daos = new DataOutputStream(baos)
        daos.writeShort((short) name.length)
        daos.write(name)
        daos.flush()
        String encodedName = Base64.encode(baos.toByteArray())
        def obj = [:]
        obj.type = "Result"
        obj.version = 2
        obj.name = encodedName
        obj.infohash = Base64.encode(sf.getRoot())
        obj.size = sf.getCachedLength()
        obj.pieceSize = sf.getPieceSize()

        if (sf instanceof DownloadedFile)
            obj.sources = sf.sources.stream().map({dest -> dest.toBase64()}).collect(Collectors.toSet())

        if (sf.getComment() != null)
            obj.comment = sf.getComment()

        obj.browse = browseFiles 
        obj.certificates = certificates
        obj.collections = collections
        obj
    }
}
