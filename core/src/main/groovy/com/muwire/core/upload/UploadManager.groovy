package com.muwire.core.upload

import com.muwire.core.files.directories.Visibility
import com.muwire.core.profile.MWProfileHeader

import java.nio.charset.StandardCharsets

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.SharedFile
import com.muwire.core.connection.Endpoint
import com.muwire.core.download.DownloadManager
import com.muwire.core.download.Downloader
import com.muwire.core.download.SourceDiscoveredEvent
import com.muwire.core.download.SourceVerifiedEvent
import com.muwire.core.files.FileManager
import com.muwire.core.files.PersisterFolderService
import com.muwire.core.mesh.Mesh
import com.muwire.core.mesh.MeshManager

import groovy.util.logging.Log
import net.i2p.data.Base64

import java.util.function.BiPredicate
import java.util.function.Function

@Log
public class UploadManager {
    private final EventBus eventBus
    private final FileManager fileManager
    private final PersisterFolderService persisterService
    private final MeshManager meshManager
    private final DownloadManager downloadManager
    private final MuWireSettings props

    /** LOCKING: this on both structures */
    private int totalUploads
    private final Map<Persona, Integer> uploadsPerUser = new HashMap<>()
    
    private final BiPredicate<File, Persona> isVisible
    private final Function<File, Visibility> visibilityFunction

    public UploadManager() {}

    public UploadManager(EventBus eventBus, FileManager fileManager,
                         MeshManager meshManager, DownloadManager downloadManager,
                         PersisterFolderService persisterService,
                         BiPredicate<File, Persona> isVisible,
                         Function<File, Visibility> visibilityFunction,
                        MuWireSettings props) {
        this.eventBus = eventBus
        this.fileManager = fileManager
        this.persisterService = persisterService
        this.meshManager = meshManager
        this.downloadManager = downloadManager
        this.isVisible = isVisible
        this.visibilityFunction = visibilityFunction
        this.props = props
    }

    public void processGET(Endpoint e) throws IOException {
        byte [] infoHashStringBytes = new byte[44]
        DataInputStream dis = new DataInputStream(e.getInputStream())
        boolean first = true
        MWProfileHeader profileHeader = null
        while(true) {
            boolean wasFirst = false
            boolean head = false
            if (first) {
                first = false
                wasFirst = true
            } else {
                byte[] get = new byte[4]
                dis.readFully(get)
                if (get != "GET ".getBytes(StandardCharsets.US_ASCII)) {
                    if (get == "HEAD".getBytes(StandardCharsets.US_ASCII) && dis.readByte() == (byte)32) {
                        head = true
                    } else {
                        log.warning("received a method other than GET or HEAD on subsequent call")
                        e.close()
                        return
                    }
                }
            }
            dis.readFully(infoHashStringBytes)
            byte [] rn = new byte[2]
            dis.readFully(rn)
            if (rn != "\r\n".getBytes(StandardCharsets.US_ASCII)) {
                log.warning("Malformed GET/HEAD header")
                e.close()
                return
            }
            
            String infoHashString = new String(infoHashStringBytes, StandardCharsets.US_ASCII)
            log.info("Responding to upload request for root $infoHashString head $head")
            byte [] infoHashRoot = Base64.decode(infoHashString)
            InfoHash infoHash = new InfoHash(infoHashRoot)

            Request request
            if (head)
                request = Request.parseHeadRequest(infoHash, e.getInputStream())
            else
                request = Request.parseContentRequest(infoHash, e.getInputStream())
            if (profileHeader == null)
                profileHeader = request.profileHeader
            if (request.downloader == null || request.downloader.destination != e.destination) {
                log.info("Downloader persona doesn't match their destination or null")
                e.close()
                return
            }
            
            List<SharedFile> sharedFiles = Collections.emptyList()
            SharedFile[] sfs = fileManager.getSharedFiles(infoHashRoot)
            if (sfs != null) {
                sharedFiles = sfs.toList()
                sharedFiles.retainAll {isVisible.test(it.file.getParentFile(), request.downloader)}
            }
            Downloader downloader = downloadManager.downloaders.get(infoHash)
            if ( (downloader == null || downloader.isConfidential()) && sharedFiles.isEmpty()) {
                fourOhFour(e)
                return
            }


            if (request.have > 0)
                eventBus.publish(new SourceDiscoveredEvent(infoHash : request.infoHash, source : request.downloader))
                
            if (!incrementUploads(request.downloader)) {
                fourTwoNine(e)
                return
            }
            
            Mesh mesh
            File file
            int pieceSize
            boolean confidential = false
            if (!sharedFiles.isEmpty()) {
                sharedFiles.each { it.getDownloaders().add(request.downloader.getHumanReadableName()) }
                SharedFile sharedFile = sharedFiles.first();
                mesh = meshManager.getOrCreate(request.infoHash, sharedFile.NPieces, false)
                file = sharedFile.file
                confidential = isConfidential(sharedFiles)
                pieceSize = sharedFile.pieceSize
            } else if (!downloader.isConfidential()) {
                mesh = meshManager.get(infoHash)
                file = downloader.incompleteFile
                pieceSize = downloader.pieceSizePow2
            } else {
                fourOhFour(e)
                return
            }
            
            Uploader uploader
            if (head)
                uploader = new HeadUploader(file, (HeadRequest)request, e, mesh, confidential)
            else
                uploader = new ContentUploader(file, (ContentRequest)request, e, mesh, pieceSize, confidential)
            eventBus.publish(new UploadEvent(uploader : uploader, first: wasFirst, profileHeader: profileHeader))
            try {
                uploader.respond()
                if (!head)
                    eventBus.publish(new SourceVerifiedEvent(infoHash : request.infoHash, source : request.downloader.destination))
            } finally {
                decrementUploads(request.downloader)
                eventBus.publish(new UploadFinishedEvent(uploader : uploader))
            }
        }
    }

    public void processHashList(Endpoint e) {
        DataInputStream dis = new DataInputStream(e.getInputStream())
        
        byte [] infoHashStringBytes = new byte[44]
        dis.readFully(infoHashStringBytes)
        byte [] rn = new byte[2]
        dis.readFully(rn)
        if (rn != "\r\n".getBytes(StandardCharsets.US_ASCII)) {
            log.warning("Malformed HASHLIST header")
            e.close()
            return
        }
        
        String infoHashString = new String(infoHashStringBytes, StandardCharsets.US_ASCII)
        log.info("Responding to hashlist request for root $infoHashString")
        byte [] infoHashRoot = Base64.decode(infoHashString)
        InfoHash infoHash = new InfoHash(infoHashRoot)

        Request request = Request.parseHashListRequest(infoHash, e.getInputStream())
        MWProfileHeader profileHeader = request.profileHeader
        if (request.downloader == null || request.downloader.destination != e.destination) {
            log.info("Downloader persona doesn't match their destination or null")
            e.close()
            return
        }


        Downloader downloader = downloadManager.downloaders.get(infoHash)
        List<SharedFile> sharedFiles = Collections.emptyList()
        SharedFile[] sfs = fileManager.getSharedFiles(infoHashRoot)
        if (sfs != null) {
            sharedFiles = sfs.toList()
            sharedFiles.retainAll {isVisible.test(it.file.getParentFile(), request.downloader)}
        }
        if ( (downloader == null || downloader.isConfidential()) && sharedFiles.isEmpty()) {
            fourOhFour(e)
            return
        }

        InfoHash fullInfoHash
        boolean confidential = false
        if (!sharedFiles.isEmpty()) {
            fullInfoHash = persisterService.loadInfoHash(sharedFiles[0].file.getCanonicalFile())
            confidential = isConfidential(sharedFiles)
        } else if (!downloader.isConfidential()) {
            byte [] hashList = downloader.getInfoHash().getHashList()
            if (hashList != null && hashList.length > 0)
                fullInfoHash = downloader.getInfoHash()
            else {
                fourOhFour(e)
                return
            }
        } else {
            fourOhFour(e)
            return
        }
        
        if (!incrementUploads(request.downloader)) {
            fourTwoNine(e)
            return
        }

        Uploader uploader = new HashListUploader(e, fullInfoHash, (HashListRequest)request, confidential)
        eventBus.publish(new UploadEvent(uploader : uploader, first: true, profileHeader: profileHeader)) // hash list is always a first
        try {
            try {
                uploader.respond()
            } finally {
                eventBus.publish(new UploadFinishedEvent(uploader: uploader))
            }

            // proceed with content
            boolean first = true
            while(true) {
                boolean wasFirst = false
                if (first) {
                    first = false
                    wasFirst = true
                }
                byte[] get = new byte[4]
                dis.readFully(get)
                boolean head = false
                if (get != "GET ".getBytes(StandardCharsets.US_ASCII)) {
                    if (get == "HEAD".getBytes(StandardCharsets.US_ASCII) && dis.readByte() == (byte)32) {
                        head = true
                    } else {
                        log.warning("received a method other than GET or HEAD on subsequent call")
                        e.close()
                        return
                    }
                }
                
                dis.readFully(infoHashStringBytes)
                rn = new byte[2]
                dis.readFully(rn)
                if (rn != "\r\n".getBytes(StandardCharsets.US_ASCII)) {
                    log.warning("Malformed GET header")
                    e.close()
                    return
                }
                
                String contentInfoHashString = new String(infoHashStringBytes, StandardCharsets.US_ASCII)
                if (contentInfoHashString != infoHashString) {
                    log.warning("GET and HASHLIST infohash mismatch")
                    e.close()
                    return
                }
                
                log.info("Responding to upload request for root $infoHashString")
    
                if (head)
                    request = Request.parseHeadRequest(new InfoHash(infoHashRoot), e.getInputStream())
                else
                    request = Request.parseContentRequest(new InfoHash(infoHashRoot), e.getInputStream())
                if (request.downloader != null && request.downloader.destination != e.destination) {
                    log.info("Downloader persona doesn't match their destination")
                    e.close()
                    return
                }
    
                if (request.have > 0)
                    eventBus.publish(new SourceDiscoveredEvent(infoHash : request.infoHash, source : request.downloader))
    
                Mesh mesh
                File file
                int pieceSize
                if (sharedFiles.isEmpty()) {
                    mesh = meshManager.get(infoHash)
                    file = downloader.incompleteFile
                    pieceSize = downloader.pieceSizePow2
                } else {
                    sharedFiles.each { it.getDownloaders().add(request.downloader.getHumanReadableName()) }
                    SharedFile sharedFile = sharedFiles[0];
                    mesh = meshManager.getOrCreate(request.infoHash, sharedFile.NPieces, false)
                    file = sharedFile.file
                    pieceSize = sharedFile.pieceSize
                }
    
                if (head)
                    uploader = new HeadUploader(file, (HeadRequest)request, e, mesh, confidential)
                else
                    uploader = new ContentUploader(file, (ContentRequest)request, e, mesh, pieceSize, confidential)
                eventBus.publish(new UploadEvent(uploader : uploader, first: wasFirst, profileHeader: profileHeader))
                try {
                    uploader.respond()
                    if (!head)
                        eventBus.publish(new SourceVerifiedEvent(infoHash : request.infoHash, source : request.downloader.destination))
                } finally {
                    eventBus.publish(new UploadFinishedEvent(uploader : uploader))
                }
            }
        } finally {
            decrementUploads(request.downloader)
        }
    }
    
    private static void fourOhFour(Endpoint e) {
        log.info("file or infohash not found or not visible")
        e.getOutputStream().write("404 File Not Found\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
        e.getOutputStream().flush()
        e.close()
    }
    
    private static void fourTwoNine(Endpoint e) {
        log.info("rejecting due to slot limit")
        e.getOutputStream().write("429 Too Many Requests\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
        e.getOutputStream().flush()
        e.close()
    }
    
    private boolean isConfidential(List<SharedFile> sfs) {
        // only if all instances of this file are confidential is it considered such
        boolean rv = true
        sfs.each {
            rv &= visibilityFunction.apply(it.file.getParentFile()) != Visibility.EVERYONE
        }
        rv
    }
    
    /**
     * @param p downloader
     * @return true if this upload hasn't hit any slot limits
     */
    private synchronized boolean incrementUploads(Persona p) {
        if (props.totalUploadSlots >= 0 && totalUploads >= props.totalUploadSlots)
            return false
        if (props.uploadSlotsPerUser == 0)
            return false
        
        Integer currentUploads = uploadsPerUser.get(p)
        if (currentUploads == null)
            currentUploads = 0
        if (props.uploadSlotsPerUser > 0 && currentUploads >= props.uploadSlotsPerUser)
            return false
        uploadsPerUser.put(p, ++currentUploads)
        totalUploads++
        true
    }
    
    private synchronized void decrementUploads(Persona p) {
        totalUploads--
        Integer currentUploads = uploadsPerUser.get(p)
        if (currentUploads == null || currentUploads == 0)
            throw new IllegalStateException()
        currentUploads--
        if (currentUploads == 0)
            uploadsPerUser.remove(p)
        else
            uploadsPerUser.put(p, currentUploads)
    }
}

