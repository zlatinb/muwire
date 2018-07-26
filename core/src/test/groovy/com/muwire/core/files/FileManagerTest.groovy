package com.muwire.core.files

import org.junit.Before
import org.junit.Test

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile
import com.muwire.core.search.ResultsEvent
import com.muwire.core.search.SearchEvent

class FileManagerTest {

	EventBus eventBus
	
	FileManager manager
	volatile ResultsEvent results
	
	def listener = new Object() {
		void onResultsEvent(ResultsEvent e) {
			results = e
		}
	}
	
	@Before
	void before() {
		eventBus = new EventBus()
		eventBus.register(ResultsEvent.class, listener)
		manager = new FileManager(eventBus)
		results = null
	}
	
	@Test
	void testHash1Result() {
		File f = new File("a b.c")
		InfoHash ih = InfoHash.fromHashList(new byte[32])
		SharedFile sf = new SharedFile(f,ih)
		FileHashedEvent fhe = new FileHashedEvent(sharedFile: sf)
		manager.onFileHashedEvent(fhe)
		
		UUID uuid = UUID.randomUUID()
		SearchEvent se = new SearchEvent(searchHash: ih.getRoot(), uuid: uuid)
		
		manager.onSearchEvent(se)
		Thread.sleep(20)
		
		assert results != null
		assert results.results.size() == 1
		assert results.results.contains(sf)
		assert results.uuid == uuid
	}
	
	@Test
	void testHash2Results() {
		InfoHash ih = InfoHash.fromHashList(new byte[32])
		SharedFile sf1 = new SharedFile(new File("a b.c"), ih)
		SharedFile sf2 = new SharedFile(new File("d e.f"), ih)
		manager.onFileLoadedEvent new FileLoadedEvent(loadedFile : sf1)
		manager.onFileLoadedEvent new FileLoadedEvent(loadedFile : sf2)

		UUID uuid = UUID.randomUUID()
		SearchEvent se = new SearchEvent(searchHash: ih.getRoot(), uuid: uuid)
		
		manager.onSearchEvent(se)
		Thread.sleep(20)
		
		assert results != null
		assert results.results.size() == 2
		assert results.results.contains(sf1)
		assert results.results.contains(sf2)
		assert results.uuid == uuid
	}
	
	@Test
	void testHash0Results() {
		File f = new File("a b.c")
		InfoHash ih = InfoHash.fromHashList(new byte[32])
		SharedFile sf = new SharedFile(f,ih)
		FileHashedEvent fhe = new FileHashedEvent(sharedFile: sf)
		manager.onFileHashedEvent(fhe)
		
		manager.onSearchEvent new SearchEvent(searchHash: new byte[32], uuid: UUID.randomUUID())
		Thread.sleep(20)
		
		assert results == null
	}
	
	@Test
	void testKeyword1Result() {
		File f = new File("a b.c")
		InfoHash ih = InfoHash.fromHashList(new byte[32])
		SharedFile sf = new SharedFile(f,ih)
		FileHashedEvent fhe = new FileHashedEvent(sharedFile: sf)
		manager.onFileHashedEvent(fhe)
		
		UUID uuid = UUID.randomUUID()
		manager.onSearchEvent new SearchEvent(searchTerms: ["a"], uuid:uuid)
		Thread.sleep(20)
		
		assert results != null
		assert results.results.size() == 1
		assert results.results.contains(sf)
		assert results.uuid == uuid		
	}
	
	@Test
	void testKeyword2Results() {
		File f1 = new File("a b.c")
		InfoHash ih1 = InfoHash.fromHashList(new byte[32])
		SharedFile sf1 = new SharedFile(f1, ih1)
		manager.onFileLoadedEvent new FileLoadedEvent(loadedFile: sf1)
		
		File f2 = new File("c d.e")
		InfoHash ih2 = InfoHash.fromHashList(new byte[64])
		SharedFile sf2 = new SharedFile(f2, ih2)
		manager.onFileLoadedEvent new FileLoadedEvent(loadedFile: sf2)
		
		UUID uuid = UUID.randomUUID()
		manager.onSearchEvent new SearchEvent(searchTerms: ["c"], uuid:uuid)
		Thread.sleep(20)
		
		assert results != null
		assert results.results.size() == 2
		assert results.results.contains(sf1)
		assert results.results.contains(sf2)
		assert results.uuid == uuid
	}
	
	@Test
	void testKeyword0Results() {
		File f = new File("a b.c")
		InfoHash ih = InfoHash.fromHashList(new byte[32])
		SharedFile sf = new SharedFile(f,ih)
		FileHashedEvent fhe = new FileHashedEvent(sharedFile: sf)
		manager.onFileHashedEvent(fhe)
		
		manager.onSearchEvent new SearchEvent(searchTerms: ["d"], uuid: UUID.randomUUID())
		Thread.sleep(20)
		
		assert results == null
	}
	
	@Test
	void testRemoveFileExistingHash() {
		InfoHash ih = InfoHash.fromHashList(new byte[32])
		SharedFile sf1 = new SharedFile(new File("a b.c"), ih)
		SharedFile sf2 = new SharedFile(new File("d e.f"), ih)
		manager.onFileLoadedEvent new FileLoadedEvent(loadedFile : sf1)
		manager.onFileLoadedEvent new FileLoadedEvent(loadedFile : sf2)
		
		manager.onFileUnsharedEvent new FileUnsharedEvent(unsharedFile: sf2)
		
		manager.onSearchEvent new SearchEvent(searchHash : ih.getRoot())
		Thread.sleep(20)
		assert results != null
		assert results.results.size() == 1
		assert results.results.contains(sf1)
	}
	
	@Test
	void testRemoveFile() {
		File f1 = new File("a b.c")
		InfoHash ih1 = InfoHash.fromHashList(new byte[32])
		SharedFile sf1 = new SharedFile(f1, ih1)
		manager.onFileLoadedEvent new FileLoadedEvent(loadedFile: sf1)
		
		File f2 = new File("c d.e")
		InfoHash ih2 = InfoHash.fromHashList(new byte[64])
		SharedFile sf2 = new SharedFile(f2, ih2)
		manager.onFileLoadedEvent new FileLoadedEvent(loadedFile: sf2)
		
		manager.onFileUnsharedEvent new FileUnsharedEvent(unsharedFile: sf2)
		
		// 1 match left
		manager.onSearchEvent new SearchEvent(searchTerms: ["c"])
		Thread.sleep(20)
		assert results != null
		assert results.results.size() == 1
		assert results.results.contains(sf1)
		
		// no match
		results = null
		manager.onSearchEvent new SearchEvent(searchTerms: ["d"])
		assert results == null
		
	}
}
