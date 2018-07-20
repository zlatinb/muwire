package com.muwire.core;

import java.io.File;
import java.util.Set;

import net.i2p.data.Destination;

public class DownloadedFile extends SharedFile {
	
	private final Set<Destination> sources;

	public DownloadedFile(File file, InfoHash infoHash, Set<Destination> sources) {
		super(file, infoHash);
		this.sources = sources;
	}
	
	public Set<Destination> getSources() {
		return sources;
	}

}
