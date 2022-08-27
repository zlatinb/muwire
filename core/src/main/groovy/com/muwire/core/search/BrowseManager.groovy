package com.muwire.core.search

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.collections.CollectionManager
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.filecert.CertificateManager
import com.muwire.core.files.FileListCallback
import com.muwire.core.files.FileManager
import com.muwire.core.files.FileTree
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.util.DataUtil
import com.muwire.core.util.PathTree
import com.muwire.core.util.PathTreeListCallback
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
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
        endpoint.getOutputStream().write("\r\n".getBytes(StandardCharsets.US_ASCII))
        
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
    
    void processV2Request(Persona browser, Endpoint endpoint) {
        // 1. build the tree the browser will see
        PathTree<SharedFile> tempTree = new PathTree<>()
        for (SharedFile sf : fileManager.getSharedFiles().values()) {
            if (!isVisible.test(sf.file.getParentFile(), browser))
                continue
            Path path = sf.getPathToSharedParent()
            if (path == null) {
                path = Path.of(sf.getFile().getName())
            } else {
                String first = path.getName(0)
                String[] more = new String[path.getNameCount()]
                for (int i = 1; i < path.getNameCount(); i ++)
                    more[i - 1] = path.getName(i)
                more[more.length - 1] = sf.getFile().getName()
                path = Path.of(first, more)
            }
            tempTree.add(path, sf)
        }
        
        // 2. Grab the top-level items (top level is actually hidden roots, so level 2)
        def hiddenRoots = new ListCallback()
        tempTree.list(null, hiddenRoots)
        def topLevelItems = new ListCallback()
        for (Path path : hiddenRoots.dirs) {
            tempTree.list(path, topLevelItems)
        }
        
        topLevelItems.files.values().each {it.hit(browser, System.currentTimeMillis(), "Browse Host")}
        OutputStream os = endpoint.getOutputStream()
        os.write("Files:${topLevelItems.files.size()}\r\n".getBytes(StandardCharsets.US_ASCII))
        os.write("Dirs:${topLevelItems.dirs.size()}\r\n".getBytes(StandardCharsets.US_ASCII))
        os.write("\r\n".getBytes(StandardCharsets.US_ASCII))

        JsonOutput jsonOutput = new JsonOutput()
        DataOutputStream dos = null
        GZIPOutputStream gzos = null
        try {
            gzos = new GZIPOutputStream(os)
            dos = new DataOutputStream(gzos)
            writeFiles(topLevelItems.files.values(), dos, jsonOutput)
            writeDirs(topLevelItems.dirs, dos, jsonOutput)
            dos.flush()
            gzos.finish()
            
            while(true) {
                String firstLine = DataUtil.readTillRN(endpoint.getInputStream())
                if (firstLine.startsWith("PING")) {
                    os.write("PONG\r\n".getBytes(StandardCharsets.US_ASCII))
                    os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
                    os.flush()
                    continue
                } 
                if (!firstLine.startsWith("GET ")) 
                    throw new Exception("=Unknown verb")
                
                firstLine = firstLine.substring(4)
                String[] elements = firstLine.split(",")
                if (elements.length == 0)
                    throw new Exception("invalid GET request")
                def decoded = elements.collect {DataUtil.readi18nString(Base64.decode(it))}
                String first = decoded.remove(0)
                String[] more = decoded.toArray(new String[0])
                Path requestedPath = Path.of(first, more)

                os.write("200 OK\r\n".getBytes(StandardCharsets.US_ASCII))
                
                // TODO: add recursive
                def cb = new ListCallback()
                tempTree.list(requestedPath, cb)
                
                os.write("Files:${cb.files.size()}\r\n".getBytes(StandardCharsets.US_ASCII))
                os.write("Dirs:${cb.dirs.size()}\r\n".getBytes(StandardCharsets.US_ASCII))
                os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
                
                gzos = new GZIPOutputStream(os)
                dos = new DataOutputStream(gzos)
                writeFiles(cb.files.values(), dos, jsonOutput)  
                writeDirs(cb.dirs, dos, jsonOutput)
                dos.flush()
                gzos.finish()
            }
        } finally {
            try {
                dos?.flush()
                dos?.close()
            } catch (Exception ignored) {}
        }
    }
    
    private void writeFiles(Collection<SharedFile> files, DataOutputStream dos, JsonOutput jsonOutput) {
        for (SharedFile sharedFile : files) {
            InfoHash ih = sharedFile.getRootInfoHash()
            int certificates = certificateManager.getByInfoHash(ih).size()
            Set<InfoHash> collections = collectionManager.collectionsForFile(ih)
            def obj = ResultsSender.sharedFileToObj(sharedFile, false, certificates, collections, true)
            def json = jsonOutput.toJson(obj)
            dos.writeShort((short)json.length())
            dos.write(json.getBytes(StandardCharsets.US_ASCII))
        }
    }
    
    private void writeDirs(Collection<Path> dirs, DataOutputStream dos, JsonOutput jsonOutput) {
        for(Path path : dirs) {
            def obj = [:]
            obj.directory = true
            List<String> toStringPaths = []
            for (Path element : path) {
                String toString = element.toString()
                if (!toString.isEmpty())
                    toStringPaths << Base64.encode(DataUtil.encodei18nString(toString))
            }
            obj.path = toStringPaths
            def json = jsonOutput.toJson(obj)
            dos.writeShort((short)json.length())
            dos.write(json.getBytes(StandardCharsets.US_ASCII))
        }
    }
    
    private static class ListCallback implements PathTreeListCallback<SharedFile> {

        final Map<Path, SharedFile> files = new HashMap<>()
        final Set<Path> dirs = new HashSet<>()

        @Override
        void onLeaf(Path path, SharedFile value) {
            files.put(path, value)
        }

        @Override
        void onDirectory(Path path) {
            dirs.add(path)
        }
    }
}
