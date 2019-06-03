package com.muwire.core.upload

import java.nio.charset.StandardCharsets

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile
import com.muwire.core.connection.Endpoint
import com.muwire.core.files.FileManager

import groovy.util.logging.Log
import net.i2p.data.Base64

@Log
public class UploadManager {
    private final EventBus eventBus
    private final FileManager fileManager
    
    public UploadManager() {}
    
    public UploadManager(EventBus eventBus, FileManager fileManager) {
        this.eventBus = eventBus
        this.fileManager = fileManager
    }
    
    public void processEndpoint(Endpoint e) throws IOException {
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
            Set<SharedFile> sharedFiles = fileManager.getSharedFiles(infoHashRoot)
            if (sharedFiles == null || sharedFiles.isEmpty()) {
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

            Request request = Request.parse(new InfoHash(infoHashRoot), e.getInputStream())
            if (request.downloader != null && request.downloader.destination != e.destination) {
                log.info("Downloader persona doesn't match their destination")
                e.close()
                return
            }
            Uploader uploader = new Uploader(sharedFiles.iterator().next().file, request, e)
            eventBus.publish(new UploadEvent(uploader : uploader))
            try {
                uploader.respond()
            } finally {
                eventBus.publish(new UploadFinishedEvent(uploader : uploader))
            }
        }
        
    }
}

