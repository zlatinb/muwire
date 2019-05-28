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
    
    public UploadManager(EventBus eventBus, FileManager fileManager) {
        this.eventBus = eventBus
        this.fileManager = fileManager
    }
    
    public void processEndpoint(Endpoint e) throws IOException {
        while(true) {
            byte [] infoHashStringBytes = new byte[44]
            DataInputStream dis = new DataInputStream(e.getInputStream())
            dis.readFully(infoHashStringBytes)
            String infoHashString = new String(infoHashStringBytes, StandardCharsets.US_ASCII)
            log.info("Responding to upload request for root $infoHashString")

            byte [] infoHashRoot = Base64.decode(infoHashStringBytes)
            Set<SharedFile> sharedFiles = fileManager.getSharedFiles(infoHashRoot)
            if (sharedFiles == null || sharedFiles.isEmpty()) {
                log.info "file not found"
                e.getOutputStream().write("404 File Not Found".getBytes(StandardCharsets.US_ASCII))
                e.getOutputStream().flush()
                return
            }

            byte [] rn = new byte[2]
            dis.readFully(rn)
            if (rn != "\r\n".getBytes(StandardCharsets.US_ASCII)) {
                log.warning("Malformed GET header")
                return
            }

            Request request = Request.parse(new InfoHash(infoHashRoot), e.getInputStream())
            Uploader uploader = new Uploader(request, e)
            eventBus.publish(new UploadEvent(uploader))
            try {
                uploader.respond()
            } finally {
                eventBus.publish(new UploadFinishedEvent(uploader))
            }
        }
        
    }
}

