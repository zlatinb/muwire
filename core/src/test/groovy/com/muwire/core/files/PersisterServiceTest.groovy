package com.muwire.core.files

import org.junit.Test

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile

import groovy.json.JsonOutput
import groovy.util.GroovyTestCase
import net.i2p.data.Base32

class PersisterServiceTest extends GroovyTestCase {

	@Test
	void testName() {
		File sharedDir = new File("sharedDir")
		sharedDir.mkdir()
		sharedDir.deleteOnExit()
		
		File sharedFile1 = new File(sharedDir,"file1")
		sharedFile1.deleteOnExit()
		FileOutputStream fos = new FileOutputStream(sharedFile1);
		fos.write new byte[1]
		fos.close()
		
		FileHasher fh = new FileHasher()
		InfoHash ih1 = fh.hashFile(sharedFile1)
		
		def json = [:]
		json.file = sharedFile1.getCanonicalFile().toString()
		json.length = 1
		json.infoHash = Base32.encode(ih1.getRoot())
		json.hashList = [Base32.encode(ih1.getHashList())]
		
		json = JsonOutput.toJson(json)
		
		File persisted = new File("persisted")
		if (persisted.exists())
			persisted.delete()
		persisted.deleteOnExit()
		persisted.write json
		
		SharedFile loadedFile
		def eventListener = new Object() {
			def onFileLoadedEvent(FileLoadedEvent e) {
				loadedFile = e.loadedFile
			}
		}
		
		EventBus eventBus = new EventBus()
		eventBus.register(FileLoadedEvent.class, eventListener)
		
		PersisterService ps = new PersisterService(persisted, eventBus, 100, null)
		ps.start()
		Thread.sleep(2000)
		
		assert loadedFile != null
		assert loadedFile.file == sharedFile1.getCanonicalFile()
		assert loadedFile.infoHash == ih1
	}

}
