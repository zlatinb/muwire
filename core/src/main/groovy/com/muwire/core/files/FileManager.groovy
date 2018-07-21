package com.muwire.core.files

import com.muwire.core.EventBus
import com.muwire.core.SharedFile

class FileManager {


	final EventBus eventBus
	final Map<byte[], Set<SharedFile>> rootToFiles = new HashMap<>()
	final Map<File, SharedFile> fileToSharedFile = new HashMap<>()
	
	FileManager(EventBus eventBus) {
		this.eventBus = eventBus
	}
	
	void onFileHashedEvent(FileHashedEvent e) {
		addToIndex(e.sharedFile)
	}
	
	void onFileLoadedEvent(FileLoadedEvent e) {
		addToIndex(e.loadedFile)
	}
	
	void onFileDownloadedEvent(FileDownloadedEvent e) {
		addToIndex(e.downloadedFile)
	}
	
	private void addToIndex(SharedFile sf) {
		byte [] root = sf.getInfoHash().getRoot()
		Set<SharedFile> existing = rootToFiles.get(root)
		if (existing == null) {
			existing = new HashSet<>()
			rootToFiles.put(root, existing);
		}
		existing.add(sf)
		fileToSharedFile.put(sf.file, sf)
	}
	
	Map<File, SharedFile> getSharedFiles() {
		// TODO: figure out locking
		fileToSharedFile
	}
	
	void onSearchEvent(SearchEvent e) {
		// hash takes precedence
		ResultsEvent re = null
		if (e.searchHash != null) {
			Set<SharedFile> found = rootToFiles.get e.searchHash
			if (found != null && !found.isEmpty())
				re = new ResultsEvent(results: found.asList(), uuid: e.uuid)
		} else {
			// TODO: keyword search
		}
		
		if (re != null)
			eventBus.publish(re)
	}
}
