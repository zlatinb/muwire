package com.muwire.core.files

import com.muwire.core.*
import com.muwire.core.filefeeds.UIFilePublishedEvent
import com.muwire.core.filefeeds.UIFileUnpublishedEvent
import com.muwire.core.files.directories.WatchedDirectoryManager
import com.muwire.core.util.DataUtil
import groovy.json.JsonOutput
import groovy.json.JsonParserType
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64

import javax.crypto.Mac
import javax.crypto.SecretKey
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.Key
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.stream.Stream

/**
 * A persister that stores information about the files shared using
 * individual JSON files in directories.
 *
 * The absolute path's 32bit hash to the shared file is used
 * to build the directory and filename.
 *
 * This persister only starts working once the old persister has finished loading
 * @see PersisterFolderService#getJsonPath
 */
@Log
class PersisterFolderService extends BasePersisterService {

    final static int CUT_LENGTH = 6
    
    
    private static final int PARALLEL_UNSHARE = 128
    
    private final byte[] salt
    private final String saltHash
    private final Map<Path, String> cachedRoots = Collections.synchronizedMap(new HashMap<>())
    private final Core core;
    final File location
    final EventBus listener
    final int interval
    final Timer timer
    final ExecutorService persisterExecutor = Executors.newSingleThreadExecutor({ r ->
        new Thread(r, "file persister")
    } as ThreadFactory)

    PersisterFolderService(Core core, File location, EventBus listener) {
        this.core = core;
        this.location = location
        this.listener = listener
        this.interval = interval
        timer = new Timer("file-folder persister timer", true)
        
        location.mkdirs()
        File saltFile = new File(location, "salt.bin")
        if (saltFile.exists()) {
            log.info("loading salt from file")
            salt = saltFile.bytes
        } else {
            log.info("generating new salt")
            Random r = new SecureRandom()
            salt = new byte[32]
            r.nextBytes(salt)
            saltFile.bytes = salt
        }
        
        saltHash = Base64.encode(hash(salt))
    }
    
    private static byte[] hash(byte[] src) {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256")
        sha256.update(src)
        sha256.digest()
    }

    void stop() {
        timer.cancel()
        persisterExecutor.shutdown()
    }

    void onPersisterDoneEvent(PersisterDoneEvent persisterDoneEvent) {
        log.info("Old persister done")
        persisterExecutor.execute({load()} as Runnable)
    }
    
    void onInfoHashEvent(InfoHashEvent event) {
        persistHashList(event.file, event.infoHash)
    }

    void onFileHashedEvent(FileHashedEvent hashedEvent) {
        if (hashedEvent.sharedFile == null)
            return
        if (core.getMuOptions().getAutoPublishSharedFiles() && hashedEvent.sharedFile != null) 
            hashedEvent.sharedFile.publish(System.currentTimeMillis())
        
        File file = hashedEvent.sharedFile.file
        Path root = findSharedParent(file)
        log.fine("PFS: for $file found root $root")
        hashedEvent.sharedFile.setPathToSharedParent(root)
        
        persistFile(hashedEvent.sharedFile)
    }

    void onFileDownloadedEvent(FileDownloadedEvent downloadedEvent) {
        if (core.getMuOptions().getShareDownloadedFiles() && !downloadedEvent.confidential) {
            if (core.getMuOptions().getAutoPublishSharedFiles())
                downloadedEvent.downloadedFile.publish(System.currentTimeMillis())
            
            File file = downloadedEvent.downloadedFile.file
            File parent = downloadedEvent.parentToShare
            Path sharedParent
            if (parent != null)
                sharedParent = explicitSharedParent(file, parent)
            else
                sharedParent = findSharedParent(file)
            downloadedEvent.downloadedFile.setPathToSharedParent(sharedParent)
            
            persistHashList(downloadedEvent.downloadedFile.file.getCanonicalFile(), downloadedEvent.infoHash)
            persistFile(downloadedEvent.downloadedFile)
        }
    }

    /**
     * Get rid of the json and hashlists of unshared files
     * @param unsharedEvent
     */
    void onFileUnsharedEvent(FileUnsharedEvent unsharedEvent) {
        persisterExecutor.submit({
            if (unsharedEvent.unsharedFiles.length < PARALLEL_UNSHARE) {
                for (SharedFile sharedFile : unsharedEvent.unsharedFiles)
                    unshareFile(sharedFile)
            } else
                unsharedEvent.unsharedFiles.toList().stream().parallel().forEach { unshareFile(it) }        
        } as Runnable)
    }
    
    void onFileModifiedEvent(FileModifiedEvent event) {
        persisterExecutor.submit( {
            unshareFile(event.sharedFile)
        } as Runnable)
    }
    
    private void unshareFile(SharedFile sharedFile) {
        def jsonPath = getJsonPath(sharedFile)
        def jsonFile = jsonPath.toFile()
        if(jsonFile.isFile()){
            jsonFile.delete()
        }
        def hashListPath = getHashListPath(sharedFile)
        def hashListFile = hashListPath.toFile()
        if (hashListFile.isFile())
            hashListFile.delete()
        def parent = hashListFile.getParentFile()
        if (parent.list().length == 0)
            parent.delete()
    }
    
    void onFileLoadedEvent(FileLoadedEvent loadedEvent) {
        if(loadedEvent.source == "PersisterService"){
            log.info("Migrating persisted file from PersisterService: "
                    + loadedEvent.loadedFile.file.absolutePath.toString())
            persistHashList(loadedEvent.loadedFile.file.getCanonicalFile(), loadedEvent.infoHash)
            persistFile(loadedEvent.loadedFile)
        }
    }
    
    void onUICommentEvent(UICommentEvent e) {
        persistFile(e.sharedFile)
    }
    
    void onUIFilePublishedEvent(UIFilePublishedEvent e) {
        persistFile(e.sf)
    }
    
    void onUIFileUnpublishedEvent(UIFileUnpublishedEvent e) {
        persistFile(e.sf)
    }

    void load() {
        log.fine("Loading...")
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY)

        if (location.exists() && location.isDirectory()) {
            try {
                _load()
            }
            catch (Exception e) {
                log.log(Level.WARNING, "couldn't load files", e)
            }
        } else {
            listener.publish(new AllFilesLoadedEvent())
        }
        loaded = true
    }

    /**
     * Loads every JSON into memory.  If this is the plugin, load right away.
     * If it's the standalone throttle and use a single thread if configured.
     */
    private void _load() {
        int loaded = 0
        AtomicInteger failed = new AtomicInteger()
        Stream<Path> stream = Files.walk(location.toPath())
        stream = stream.filter({
            it.getFileName().toString().endsWith(".json")
        })
        if (core.muOptions.plugin || !core.muOptions.throttleLoadingFiles)
            stream = stream.parallel()
        stream.forEach({
            log.fine("processing path $it")
            def slurper = new JsonSlurper(type: JsonParserType.LAX)
            try {
                def parsed = slurper.parse(it.toFile())
                def event = fromJsonLite parsed
                if (event == null) return

                if (core.muOptions.ignoredFileTypes.contains(DataUtil.getFileExtension(event.loadedFile.file))) {
                    log.fine("ignoring file ${event.loadedFile.file}")
                    return
                }
                
                log.fine("loaded file $event.loadedFile.file")
                
                if (event.loadedFile.getPathToSharedParent() == null) {
                    log.fine("trying to find shared parent for $event.loadedFile.file")
                    Path path = findSharedParent(event.loadedFile.file)
                    if (path != null) {
                        log.fine("found path $path")
                        event.loadedFile.setPathToSharedParent(path)
                        _persistFile(event.loadedFile)
                    }
                } else
                    log.fine("loaded shared parent from json ${event.loadedFile.getPathToSharedParent()}")
                
                listener.publish event
                
                if (!core.muOptions.plugin && core.muOptions.throttleLoadingFiles) {
                    loaded++
                    if (loaded % 10 == 0)
                        Thread.sleep(20)
                }
            } catch (Exception e) {
                log.log(Level.WARNING,"failed to load $it",e)
                failed.incrementAndGet()
            }
        })
        listener.publish(new AllFilesLoadedEvent(failed : failed.get()))
    }

    /**
     * Finds the path to the file to send over the network.  The parts of the
     * path that are before the topmost shared parent are obfuscated, the
     * rest are visible so that the receiver can construct a tree.
     */
    private Path findSharedParent(File file) {
        File parent = file.getParentFile()
        if (parent == null)
            return Path.of(saltHash)
        
        WatchedDirectoryManager wdm = core.getWatchedDirectoryManager()
        
        File root = file
        while(true) {
            File parent2 = root.getParentFile()
            if (parent2 == null)
                break
            if (!wdm.isWatched(parent2))
                break
            root = parent2
        }
        
        if (root == file)
            return Path.of(saltHash)
        
        Path toParent = root.toPath().relativize(parent.toPath())
        Path visible = Path.of(root.getName(), toParent.toString())
        Path invisible = root.getParentFile().toPath()
        
        String invisbleRoot = cachedRoots.computeIfAbsent(invisible, {mac(it)})
        return Path.of(invisbleRoot, visible.toString())
    }
    
    private String mac(Path path) {
        Mac mac = Mac.getInstance("HmacSHA256")
        Key key = new HMACKey(salt)
        mac.init(key)
        mac.update(path.toString().getBytes(StandardCharsets.UTF_8))
        Base64.encode(mac.doFinal())
    }

    /**
     * Generates a path with explicit shared parent
     * @param file that is being shared
     * @param explicitParent explicit parent
     * @return a Path from parent to file
     */
    private Path explicitSharedParent(File file, File explicitParent) {
        File parent = file.getParentFile()
        if (parent == explicitParent)
            return Path.of(saltHash)
        
        Path toParent = explicitParent.toPath().relativize(parent.toPath())
        Path visible = Path.of(explicitParent.getName(), toParent.toString())
        Path invisible = explicitParent.getParentFile().toPath()
        String invisibleRoot = cachedRoots.computeIfAbsent(invisible,{mac(it)})
        return Path.of(invisibleRoot, visible.toString())
    }

    private void persistFile(SharedFile sf) {
        persisterExecutor.submit({
            _persistFile(sf)
        } as Runnable)
    }
    
    private void _persistFile(SharedFile sf) {
        def jsonPath = getJsonPath(sf)

        def startTime = System.currentTimeMillis()
        jsonPath.parent.toFile().mkdirs()
        jsonPath.toFile().withPrintWriter { writer ->
            def json = toJson sf
            json = JsonOutput.toJson(json)
            writer.println json
        }
        
        log.fine("Time(ms) to write json: " + (System.currentTimeMillis() - startTime))
    }
    
    private void persistHashList(File canonical, InfoHash infoHash) {
        persisterExecutor.submit({_persistHashList(canonical, infoHash)} as Runnable)
    }
    
    private void _persistHashList(File canonical, InfoHash infoHash) {
        File target = getHashListPath(canonical).toFile()
        target.getParentFile().mkdirs()
        target.bytes = infoHash.hashList
    }
    
    private Path getJsonPath(SharedFile sf){
        def pathHash = sf.getB64PathHash()
        return Paths.get(
                location.getAbsolutePath(),
                pathHash.substring(0, CUT_LENGTH),
                pathHash.substring(CUT_LENGTH) + ".json"
        )
    }
    
    private Path getHashListPath(SharedFile sf) {
        def pathHash = sf.getB64PathHash()
        getHashListPath(pathHash)
    }

    private Path getHashListPath(File file) {
        MessageDigest digester = MessageDigest.getInstance("SHA-256")
        digester.update(file.getCanonicalPath().getBytes())
        String hashB64 = Base64.encode(digester.digest())
        getHashListPath(hashB64)
    }
    
    private Path getHashListPath(String prefix) {
        return Paths.get(
                location.getAbsolutePath(),
                prefix.substring(0, CUT_LENGTH),
                prefix.substring(CUT_LENGTH) + ".hashlist"
        )
    }
    
    InfoHash loadInfoHash(File canonical) {
        def path = getHashListPath(canonical)
        File file = path.toFile()
        if (!file.exists() || !file.isFile())
            return null
        InfoHash.fromHashList(file.bytes)
    }
    
    private static final class HMACKey implements SecretKey {
        private final byte[] _data;

        public HMACKey(byte[] data) { _data = data; }

        public String getAlgorithm() { return "HmacSHA256"; }
        public byte[] getEncoded() { return Arrays.copyOf(_data, 32); }
        public String getFormat() { return "RAW"; }
    }
}
