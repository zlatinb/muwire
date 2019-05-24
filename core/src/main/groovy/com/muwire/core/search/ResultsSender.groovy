package com.muwire.core.search

import com.muwire.core.SharedFile
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.files.FileHasher
import com.muwire.core.Persona

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import com.muwire.core.EventBus
import com.muwire.core.InfoHash

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
                Thread rv = new Thread("Results Sender "+THREAD_NO.incrementAndGet(), r)
                rv.setDaemon(true)
                rv
            }
        })
    
    private final I2PConnector connector
    private final Persona me
    private final EventBus eventBus
    
    ResultsSender(EventBus eventBus, I2PConnector connector, Persona me) {
        this.connector = connector;
        this.eventBus = eventBus
        this.me = me
    }
    
    void sendResults(UUID uuid, SharedFile[] results, Destination target) {
        log.info("Sending $results.length results for uuid $uuid to ${target.toBase32()}")
        if (target.equals(me.destination)) {
            def resultEvent = new ResultsEvent( uuid : uuid, results : results )
            def uiResultEvent = new UIResultEvent(sender: me, resultsEvent : resultEvent)
            eventBus.publish(uiResultEvent)
        } else {
            executor.execute(new ResultSendJob(uuid : uuid, results : results, target: target))
        }
    }
    
    private class ResultSendJob implements Runnable {
        UUID uuid
        SharedFile [] results
        Destination target
        
        @Override
        public void run() {
            byte [] tmp = new byte[InfoHash.SIZE]
            JsonOutput jsonOutput = new JsonOutput()
            Endpoint endpoint = null;
            try {
                endpoint = connector.connect(target)
                DataOutputStream os = new DataOutputStream(endpoint.getOutputStream())
                os.write("POST $uuid\r\n".getBytes(StandardCharsets.US_ASCII))
                me.write(os)
                os.writeShort((short)results.length)
                results.each {
                    byte [] name = it.getFile().getName().getBytes(StandardCharsets.UTF_8)
                    def baos = new ByteArrayOutputStream()
                    def daos = new DataOutputStream(baos)
                    daos.writeShort((short) name.length)
                    daos.write(name)
                    daos.flush()
                    name = Base64.encode(baos.toByteArray())
                    def obj = [:]
                    obj.type = "Result"
                    obj.version = 1
                    obj.name = name
                    obj.infohash = Base64.encode(it.getInfoHash().getRoot())
                    obj.size = it.getFile().length()
                    obj.pieceSize = FileHasher.getPieceSize(it.getFile().length())
                    byte [] hashList = it.getInfoHash().getHashList()
                    def hashListB64 = []
                    for (int i = 0; i < hashList.length / InfoHash.SIZE; i++) {
                        System.arraycopy(hashList, InfoHash.SIZE * i, tmp, 0, InfoHash.SIZE)
                        hashListB64 << Base64.encode(tmp)
                    }
                    obj.hashList = hashListB64

                    def json = jsonOutput.toJson(obj)
                    os.writeShort((short)json.length())
                    os.write(json.getBytes(StandardCharsets.US_ASCII))
                }
                os.flush()
            } finally {
                if (endpoint != null)
                    endpoint.closeQuietly()
            }
        }
    }
}
