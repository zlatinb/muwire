package com.muwire.core.upload

import java.nio.charset.StandardCharsets

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile
import com.muwire.core.connection.Endpoint
import com.muwire.core.download.DownloadManager
import com.muwire.core.download.Downloader
import com.muwire.core.download.SourceDiscoveredEvent
import com.muwire.core.files.FileManager
import com.muwire.core.mesh.Mesh
import com.muwire.core.mesh.MeshManager

import groovy.util.logging.Log
import net.i2p.data.Base64

@Log
public class UploadManager {
    private final EventBus eventBus
    private final FileManager fileManager
    private final MeshManager meshManager
    private final DownloadManager downloadManager
    
    public UploadManager() {}
    
    public UploadManager(EventBus eventBus, FileManager fileManager, 
        MeshManager meshManager, DownloadManager downloadManager) {
        this.eventBus = eventBus
        this.fileManager = fileManager
        this.meshManager = meshManager
        this.downloadManager = downloadManager
    }
    
    public void processGET(Endpoint e) throws IOException {
        byte [] infoHashStringBytes = new byte[44]
        DataInputStream dis = new DataInputStream(e.getInputStream())
        boolean first = true
        while(true) {
            if (first)
                first = false
            else {
                byte[] get = new byte[4]
                dis.readFully(get)
                if (get != "GET ".getBytes(StandardCharsets.US_ASCII)) {
                    log.warning("received a method other than GET on subsequent call")
                    e.close()
                    return
                }
            }
            dis.readFully(infoHashStringBytes)
            String infoHashString = new String(infoHashStringBytes, StandardCharsets.US_ASCII)
            log.info("Responding to upload request for root $infoHashString")

            byte [] infoHashRoot = Base64.decode(infoHashString)
            InfoHash infoHash = new InfoHash(infoHashRoot)
            Set<SharedFile> sharedFiles = fileManager.getSharedFiles(infoHashRoot)
            Downloader downloader = downloadManager.downloaders.get(infoHash)
            if (downloader == null && (sharedFiles == null || sharedFiles.isEmpty())) {
                log.info "file not found"
                e.getOutputStream().write("404 File Not Found\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
                e.getOutputStream().flush()
                e.close()
                return
            }

            byte [] rn = new byte[2]
            dis.readFully(rn)
            if (rn != "\r\n".getBytes(StandardCharsets.US_ASCII)) {
                log.warning("Malformed GET header")
                e.close()
                return
            }

            ContentRequest request = Request.parseContentRequest(infoHash, e.getInputStream())
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
                SharedFile sharedFile = sharedFiles.iterator().next();
                mesh = meshManager.getOrCreate(request.infoHash, sharedFile.NPieces)
                file = sharedFile.file
                pieceSize = sharedFile.pieceSize
            }
                
            Uploader uploader = new ContentUploader(file, request, e, mesh, pieceSize)
            eventBus.publish(new UploadEvent(uploader : uploader))
            try {
                uploader.respond()
            } finally {
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
        Set<SharedFile> sharedFiles = fileManager.getSharedFiles(infoHashRoot)
        if (downloader == null && (sharedFiles == null || sharedFiles.isEmpty())) {
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
            fullInfoHash = sharedFiles.iterator().next().infoHash
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
        
        Uploader uploader = new HashListUploader(e, fullInfoHash, request)
        eventBus.publish(new UploadEvent(uploader : uploader))
        try {
            uploader.respond()
        } finally {
            eventBus.publish(new UploadFinishedEvent(uploader : uploader))
        }
        
        // proceed with content
        while(true) {
            byte[] get = new byte[4]
            dis.readFully(get)
            if (get != "GET ".getBytes(StandardCharsets.US_ASCII)) {
                log.warning("received a method other than GET on subsequent call")
                e.close()
                return
            }
            dis.readFully(infoHashStringBytes)
            infoHashString = new String(infoHashStringBytes, StandardCharsets.US_ASCII)
            log.info("Responding to upload request for root $infoHashString")

            infoHashRoot = Base64.decode(infoHashString)
            infoHash = new InfoHash(infoHashRoot)
            sharedFiles = fileManager.getSharedFiles(infoHashRoot)
            downloader = downloadManager.downloaders.get(infoHash)
            if (downloader == null && (sharedFiles == null || sharedFiles.isEmpty())) {
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
                SharedFile sharedFile = sharedFiles.iterator().next();
                mesh = meshManager.getOrCreate(request.infoHash, sharedFile.NPieces)
                file = sharedFile.file
                pieceSize = sharedFile.pieceSize
            }

            uploader = new ContentUploader(file, request, e, mesh, pieceSize)
            eventBus.publish(new UploadEvent(uploader : uploader))
            try {
                uploader.respond()
            } finally {
                eventBus.publish(new UploadFinishedEvent(uploader : uploader))
            }
        }
    }
}

