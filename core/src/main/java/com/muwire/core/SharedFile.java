package com.muwire.core;

import java.io.File;
import java.io.IOException;

public class SharedFile {

	private final File file;
	private final InfoHash infoHash;
	private final int pieceSize;
	
	private final String cachedPath;
	private final long cachedLength;
	
	public SharedFile(File file, InfoHash infoHash, int pieceSize) throws IOException {
		this.file = file;
		this.infoHash = infoHash;
		this.pieceSize = pieceSize;
		this.cachedPath = file.getAbsolutePath();
		this.cachedLength = file.length();
	}

	public File getFile() {
		return file;
	}

	public InfoHash getInfoHash() {
		return infoHash;
	}
	
	public int getPieceSize() {
	    return pieceSize;
	}
	
	public int getNPieces() {
	    long length = file.length();
	    int rawPieceSize = 0x1 << pieceSize;
	    int rv = (int) (length / rawPieceSize);
	    if (length % rawPieceSize != 0)
	        rv++;
	    return rv;
	}
	
	public String getCachedPath() {
	    return cachedPath;
	}
	
	public long getCachedLength() {
	    return cachedLength;
	}
	
	@Override
	public int hashCode() {
	    return file.hashCode() ^ infoHash.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
	    if (!(o instanceof SharedFile))
	        return false;
	    SharedFile other = (SharedFile)o;
	    return file.equals(other.file) && infoHash.equals(other.infoHash);
	}
}
