package com.muwire.core.files

import org.junit.Before
import org.junit.Test

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile

import groovy.json.JsonOutput
import groovy.util.GroovyTestCase
import net.i2p.data.Base32

class PersisterServiceTest {

	class Listener {
		def publishedFiles = []
		def onFileLoadedEvent(FileLoadedEvent e) {
			publishedFiles.add(e.loadedFile)
		}
	}
	
	EventBus eventBus
	Listener listener
	File sharedDir
	File sharedFile1
	
	@Before
	void setup() {
		eventBus = new EventBus()
		listener = new Listener()
		eventBus.register(FileLoadedEvent.class, listener)
		
		sharedDir = new File("sharedDir")
		sharedDir.mkdir()
		sharedDir.deleteOnExit()
		
		sharedFile1 = new File(sharedDir,"file1")
		sharedFile1.deleteOnExit()
	}
	
	private void writeToSharedFile(int size) {
		FileOutputStream fos = new FileOutputStream(sharedFile1);
		fos.write new byte[size]
		fos.close()
	}
	
	private File initPersisted() {
		File persisted = new File("persisted")
		if (persisted.exists())
			persisted.delete()
		persisted.deleteOnExit()
		persisted
	}
	
	@Test
	void test1SharedFile1Piece() {
		writeToSharedFile(1)
		FileHasher fh = new FileHasher()
		InfoHash ih1 = fh.hashFile(sharedFile1)
		
		def json = [:]
		json.file = sharedFile1.getCanonicalFile().toString()
		json.length = 1
		json.infoHash = Base32.encode(ih1.getRoot())
		json.hashList = [Base32.encode(ih1.getHashList())]
		
		json = JsonOutput.toJson(json)
		
		File persisted = initPersisted()
		persisted.write json
		
		PersisterService ps = new PersisterService(persisted, eventBus, 100, null)
		ps.start()
		Thread.sleep(2000)
		
		assert listener.publishedFiles.size() == 1
		def loadedFile = listener.publishedFiles[0]
		assert loadedFile != null
		assert loadedFile.file == sharedFile1.getCanonicalFile()
		assert loadedFile.infoHash == ih1
	}

	@Test
	public void test1SharedFile2Pieces() {
		writeToSharedFile((0x1 << 18) + 1)
		FileHasher fh = new FileHasher()
		InfoHash ih1 = fh.hashFile(sharedFile1)
		
		assert ih1.getHashList().length == 2 * 32
		
		def json = [:]
		json.file = sharedFile1.getCanonicalFile().toString()
		json.length = sharedFile1.length()
		json.infoHash = Base32.encode ih1.getRoot()
		
		byte [] tmp = new byte[32]
		System.arraycopy(ih1.getHashList(), 0, tmp, 0, 32)
		String hash1 = Base32.encode(tmp)
		System.arraycopy(ih1.getHashList(), 32, tmp, 0, 32)
		String hash2 = Base32.encode(tmp)
		json.hashList = [hash1, hash2]
		
		json = JsonOutput.toJson(json)
		
		File persisted = initPersisted()
		persisted.write json
		
		PersisterService ps = new PersisterService(persisted, eventBus, 100, null)
		ps.start()
		Thread.sleep(2000)
		
		assert listener.publishedFiles.size() == 1
		def loadedFile = listener.publishedFiles[0]
		assert loadedFile != null
		assert loadedFile.file == sharedFile1.getCanonicalFile()
		assert loadedFile.infoHash == ih1
	}
}
