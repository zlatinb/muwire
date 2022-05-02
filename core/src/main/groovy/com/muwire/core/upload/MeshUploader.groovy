package com.muwire.core.upload

import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.mesh.Mesh
import com.muwire.core.util.DataUtil

import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

abstract class MeshUploader extends Uploader {
    
    protected final Mesh mesh
    protected final File file
    
    private final HeadRequest request

    MeshUploader(File file, HeadRequest request, Endpoint endpoint, Mesh mesh, boolean confidential) {
        super(endpoint, mesh.infoHash, confidential)
        this.mesh = mesh
        this.file = file
        this.request = request
    }

    protected void writeMesh(Persona toExclude) {
        String xHave = DataUtil.encodeXHave(mesh.pieces.getDownloaded(), mesh.pieces.nPieces)
        endpoint.getOutputStream().write("X-Have: $xHave\r\n".getBytes(StandardCharsets.US_ASCII))

        Set<Persona> sources = mesh.getRandom(9, toExclude)
        if (!sources.isEmpty()) {
            String xAlts = sources.stream().map({ it.toBase64() }).collect(Collectors.joining(","))
            endpoint.getOutputStream().write("X-Alt: $xAlts\r\n".getBytes(StandardCharsets.US_ASCII))
        }
    }
    
    protected void writeHeadSupport() {
        endpoint.getOutputStream().write("Head: true\r\n".getBytes(StandardCharsets.US_ASCII))
    }
    
    protected void writeConfidential() {
        endpoint.getOutputStream().write("Confidential: $confidential\r\n".getBytes(StandardCharsets.US_ASCII))
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getDownloader() {
        request.downloader.getHumanReadableName()
    }

    @Override
    public int getDonePieces() {
        return request.have;
    }

    @Override
    public int getTotalPieces() {
        return mesh.pieces.nPieces;
    }

    @Override
    public long getTotalSize() {
        return file.length();
    }

    @Override
    public boolean isBrowseEnabled() {
        request.browse
    }

    @Override
    public boolean isFeedEnabled() {
        request.feed
    }

    @Override
    public boolean isChatEnabled() {
        request.chat
    }

    @Override
    public boolean isMessageEnabled() {
        request.message
    }

    @Override
    public Persona getDownloaderPersona() {
        request.downloader
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MeshUploader))
            return false
        MeshUploader other = (MeshUploader) o
        request.infoHash == other.request.infoHash &&
                request.getDownloader() == other.request.getDownloader()
    }
}
