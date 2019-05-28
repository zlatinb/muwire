package com.muwire.core.files

import com.muwire.core.EventBus
import com.muwire.core.SharedFile
import com.muwire.core.search.ResultsEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.SearchIndex

class FileManager {


	final EventBus eventBus
	final Map<byte[], Set<SharedFile>> rootToFiles = Collections.synchronizedMap(new HashMap<>())
	final Map<File, SharedFile> fileToSharedFile = Collections.synchronizedMap(new HashMap<>())
	final Map<String, Set<File>> nameToFiles = new HashMap<>()
	final SearchIndex index = new SearchIndex()
	
	FileManager(EventBus eventBus) {
		this.eventBus = eventBus
	}
	
	void onFileHashedEvent(FileHashedEvent e) {
		if (e.sharedFile != null)
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
		
		String name = sf.getFile().getName()
		Set<File> existingFiles = nameToFiles.get(name)
		if (existingFiles == null) {
			existingFiles = new HashSet<>()
			nameToFiles.put(name, existingFiles)
		}
		existingFiles.add(sf.getFile())
		
		index.add(name)
	}
	
	void onFileUnsharedEvent(FileUnsharedEvent e) {
		SharedFile sf = e.unsharedFile
		byte [] root = sf.getInfoHash().getRoot()
        Set<SharedFile> existing
		Set<SharedFile> existing = rootToFiles.get(root)
		if (existing != null) {
			existing.remove(sf)
			if (existing.isEmpty()) {
				rootToFiles.remove(root)
			}
		}
		
		fileToSharedFile.remove(sf.file)
		
		String name = sf.getFile().getName()
		Set<File> existingFiles = nameToFiles.get(name)
		if (existingFiles != null) {
			existingFiles.remove(sf.file)
			if (existingFiles.isEmpty()) {
				nameToFiles.remove(name)
			}
		}
		
		index.remove(name)
	}
	
	Map<File, SharedFile> getSharedFiles() {
        synchronized(fileToSharedFile) {
            return new HashMap<>(fileToSharedFile)
        }
	}
    
    Set<SharedFile> getSharedFiles(byte []root) {
            return rootToFiles.get(root)
    }
	
	void onSearchEvent(SearchEvent e) {
		// hash takes precedence
		ResultsEvent re = null
		if (e.searchHash != null) {
            Set<SharedFile> found
            found = rootToFiles.get e.searchHash
			if (found != null && !found.isEmpty())
				re = new ResultsEvent(results: found.asList(), uuid: e.uuid)
		} else {
			def names = index.search e.searchTerms
			Set<File> files = new HashSet<>()
			names.each { files.addAll nameToFiles.getOrDefault(it, []) }
			Set<SharedFile> sharedFiles = new HashSet<>()
			files.each { sharedFiles.add fileToSharedFile[it] }
			if (!sharedFiles.isEmpty())
				re = new ResultsEvent(results: sharedFiles.asList(), uuid: e.uuid)
			
		}
		
		if (re != null)
			eventBus.publish(re)
	}
}
