package com.muwire.core.upload

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
    
    public UploadManager() {}

    public UploadManager(EventBus eventBus, FileManager fileManager,
        MeshManager meshManager, DownloadManager downloadManager,
        PersisterFolderService persisterService,
        MuWireSettings props) {
        this.eventBus = eventBus
        this.fileManager = fileManager
        this.persisterService = persisterService
        this.meshManager = meshManager
        this.downloadManager = downloadManager
        this.props = props
    }

    public void processGET(Endpoint e) throws IOException {
        byte [] infoHashStringBytes = new byte[44]
        DataInputStream dis = new DataInputStream(e.getInputStream())
        boolean first = true
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
            String infoHashString = new String(infoHashStringBytes, StandardCharsets.US_ASCII)
            log.info("Responding to upload request for root $infoHashString head $head")

            byte [] infoHashRoot = Base64.decode(infoHashString)
            InfoHash infoHash = new InfoHash(infoHashRoot)
            SharedFile[] sharedFiles = fileManager.getSharedFiles(infoHashRoot)
            Downloader downloader = downloadManager.downloaders.get(infoHash)
            if (downloader == null && (sharedFiles == null || sharedFiles.length == 0)) {
                log.info "file not found"
                e.getOutputStream().write("404 File Not Found\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                e.getOutputStream().flush()
                e.close()
                return
            }

            byte [] rn = new byte[2]
            dis.readFully(rn)
            if (rn != "\r\n".getBytes(StandardCharsets.US_ASCII)) {
                log.warning("Malformed GET/HEAD header")
                e.close()
                return
            }

            HeadRequest request
            if (head)
                request = Request.parseHeadRequest(infoHash, e.getInputStream())
            else
                request = Request.parseContentRequest(infoHash, e.getInputStream())
            if (request.downloader != null && request.downloader.destination != e.destination) {
                log.info("Downloader persona doesn't match their destination")
                e.close()
                return
            }

            if (request.have > 0)
                eventBus.publish(new SourceDiscoveredEvent(infoHash : request.infoHash, source : request.downloader))
                
            if (!incrementUploads(request.downloader)) {
                log.info("rejecting due to slot limit")
                e.getOutputStream().write("429 Too Many Requests\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                e.getOutputStream().flush()
                e.close()
                return
            }
            
            Mesh mesh
            File file
            int pieceSize
            if (downloader != null) {
                mesh = meshManager.get(infoHash)
                file = downloader.incompleteFile
                pieceSize = downloader.pieceSizePow2
            } else {
                sharedFiles.each { it.getDownloaders().add(request.downloader.getHumanReadableName()) }
                SharedFile sharedFile = sharedFiles.iterator().next();
                mesh = meshManager.getOrCreate(request.infoHash, sharedFile.NPieces, false)
                file = sharedFile.file
                pieceSize = sharedFile.pieceSize
            }
            
            Uploader uploader
            if (head)
                uploader = new HeadUploader(file, request, e, mesh)
            else
                uploader = new ContentUploader(file, request, e, mesh, pieceSize)
            eventBus.publish(new UploadEvent(uploader : uploader, first: wasFirst))
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
        byte [] infoHashStringBytes = new byte[44]
        DataInputStream dis = new DataInputStream(e.getInputStream())
        dis.readFully(infoHashStringBytes)
        String infoHashString = new String(infoHashStringBytes, StandardCharsets.US_ASCII)
        log.info("Responding to hashlist request for root $infoHashString")

        byte [] infoHashRoot = Base64.decode(infoHashString)
        InfoHash infoHash = new InfoHash(infoHashRoot)
        Downloader downloader = downloadManager.downloaders.get(infoHash)
        SharedFile[] sharedFiles = fileManager.getSharedFiles(infoHashRoot)
        if (downloader == null && (sharedFiles == null || sharedFiles.length == 0)) {
            log.info "file not found"
            e.getOutputStream().write("404 File Not Found\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
            e.getOutputStream().flush()
            e.close()
            return
        }

        byte [] rn = new byte[2]
        dis.readFully(rn)
        if (rn != "\r\n".getBytes(StandardCharsets.US_ASCII)) {
            log.warning("Malformed HASHLIST header")
            e.close()
            return
        }

        Request request = Request.parseHashListRequest(infoHash, e.getInputStream())
        if (request.downloader != null && request.downloader.destination != e.destination) {
            log.info("Downloader persona doesn't match their destination")
            e.close()
            return
        }

        InfoHash fullInfoHash
        if (downloader == null) {
            fullInfoHash = persisterService.loadInfoHash(sharedFiles[0].file.getCanonicalFile())
        } else {
            byte [] hashList = downloader.getInfoHash().getHashList()
            if (hashList != null && hashList.length > 0)
                fullInfoHash = downloader.getInfoHash()
            else {
                log.info("infohash not found in downloader")
                e.getOutputStream().write("404 File Not Found\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                e.getOutputStream().flush()
                e.close()
                return
            }
        }
        
        if (!incrementUploads(request.downloader)) {
            log.info("rejecting due to slot limit")
            e.getOutputStream().write("429 Too Many Requests\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
            e.getOutputStream().flush()
            e.close()
            return
        }

        Uploader uploader = new HashListUploader(e, fullInfoHash, request)
        eventBus.publish(new UploadEvent(uploader : uploader, first: true)) // hash list is always a first
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
                infoHashString = new String(infoHashStringBytes, StandardCharsets.US_ASCII)
                log.info("Responding to upload request for root $infoHashString")
    
                infoHashRoot = Base64.decode(infoHashString)
                infoHash = new InfoHash(infoHashRoot)
                sharedFiles = fileManager.getSharedFiles(infoHashRoot)
                downloader = downloadManager.downloaders.get(infoHash)
                if (downloader == null && (sharedFiles == null || sharedFiles.length == 0)) {
                    log.info "file not found"
                    e.getOutputStream().write("404 File Not Found\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                    e.getOutputStream().flush()
                    e.close()
                    return
                }
    
                rn = new byte[2]
                dis.readFully(rn)
                if (rn != "\r\n".getBytes(StandardCharsets.US_ASCII)) {
                    log.warning("Malformed GET header")
                    e.close()
                    return
                }
    
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
                if (downloader != null) {
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
                    uploader = new HeadUploader(file, request, e, mesh)
                else
                    uploader = new ContentUploader(file, request, e, mesh, pieceSize)
                eventBus.publish(new UploadEvent(uploader : uploader, first: wasFirst))
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

