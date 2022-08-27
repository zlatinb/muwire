package com.muwire.core.search

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.collections.CollectionManager
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.filecert.CertificateManager
import com.muwire.core.files.FileManager
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.util.DataUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.BiPredicate
import java.util.logging.Level
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Log
class BrowseManager {
    
    private final I2PConnector connector
    private final EventBus eventBus
    private final Persona me
    private final FileManager fileManager
    private final CertificateManager certificateManager
    private final CollectionManager collectionManager
    private final BiPredicate<File, Persona> isVisible
    
    private final Executor browserThread = Executors.newCachedThreadPool()
    
    BrowseManager(I2PConnector connector, EventBus eventBus, Persona me, FileManager fileManager,
            CertificateManager certificateManager, CollectionManager collectionManager,
        BiPredicate<File, Persona> isVisible) {
        this.connector = connector
        this.eventBus = eventBus
        this.me = me
        this.fileManager = fileManager
        this.certificateManager = certificateManager
        this.collectionManager = collectionManager
        this.isVisible = isVisible
    }
    
    void onUIBrowseEvent(UIBrowseEvent e) {
        browserThread.execute(new BrowseSession(eventBus, connector, e, me))
    }
    
    void processV1Request(Persona browser, Endpoint endpoint, boolean showPaths) {
        def sharedFiles = fileManager.getSharedFiles().values()
        sharedFiles.retainAll {isVisible.test(it.file.getParentFile(), browser)}
        def dos = null
        JsonOutput jsonOutput = new JsonOutput()
        try {
            dos = new DataOutputStream(new GZIPOutputStream(endpoint.getOutputStream()))
            sharedFiles.each {
                it.hit(browser, System.currentTimeMillis(), "Browse Host");
                InfoHash ih = new InfoHash(it.getRoot())
                int certificates = certificateManager.getByInfoHash(ih).size()
                Set<InfoHash> collections = collectionManager.collectionsForFile(ih)
                def obj = ResultsSender.sharedFileToObj(it, false, certificates, collections, showPaths)
                def json = jsonOutput.toJson(obj)
                dos.writeShort((short)json.length())
                dos.write(json.getBytes(StandardCharsets.US_ASCII))
            }
        } finally {
            try {
                dos?.flush()
                dos?.close()
            } catch (Exception ignore) {}
        }
    }
}
