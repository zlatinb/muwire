package com.muwire.core;

import java.io.File;

public class SharedFile {

	private final File file;
	private final InfoHash infoHash;
	
	public SharedFile(File file, InfoHash infoHash) {
		this.file = file;
		this.infoHash = infoHash;
	}

	public File getFile() {
		return file;
	}

	public InfoHash getInfoHash() {
		return infoHash;
	}
	
}
