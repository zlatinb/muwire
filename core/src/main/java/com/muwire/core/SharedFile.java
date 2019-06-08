package com.muwire.core;

import java.io.File;

public class SharedFile {

	private final File file;
	private final InfoHash infoHash;
	private final int pieceSize;
	
	public SharedFile(File file, InfoHash infoHash, int pieceSize) {
		this.file = file;
		this.infoHash = infoHash;
		this.pieceSize = pieceSize;
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
}
