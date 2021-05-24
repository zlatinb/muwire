package com.muwire.core.upload

import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.mesh.Mesh

import java.nio.charset.StandardCharsets

class HeadUploader extends MeshUploader {
    
    private final Persona downloader
    
    HeadUploader(File file, HeadRequest request, Endpoint endpoint, Mesh mesh) {
        super(file, request, endpoint, mesh)
        this.downloader = request.downloader
    }
    
    @Override
    void respond() {
        OutputStream os = endpoint.getOutputStream()
        os.write("200 OK\r\n".getBytes(StandardCharsets.US_ASCII)) // what else can be said?
        writeMesh(downloader)
        writeHeadSupport()
        os.write("\r\n".getBytes(StandardCharsets.US_ASCII))
        os.flush()
    }

    @Override
    int getProgress() {
        return 100
    }
}
