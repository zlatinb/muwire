package com.muwire.core.files

import org.junit.Test

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile

import groovy.json.JsonSlurper
import net.i2p.data.Base32

class PersisterServiceSavingTest {

	@Test
	void testSaving() {
		File f = new File("build.gradle")
		f = f.getCanonicalFile()
		FileHasher fh = new FileHasher()
		InfoHash ih = fh.hashFile(f)
		SharedFile sf = new SharedFile(f, ih)
		def fileSource = new Object() {
			Map<File, SharedFile> getSharedFiles() {
				Map<File, SharedFile> rv = new HashMap<>()
				rv.putAt(f, sf)
				rv
			}
		}
		
		File persisted = new File("persisted")
		persisted.delete()
		persisted.deleteOnExit()
		
		EventBus eventBus = new EventBus()
		PersisterService ps = new PersisterService(persisted, eventBus, 100, fileSource)
		ps.start()
		Thread.sleep(1500)
	
		JsonSlurper jsonSlurper = new JsonSlurper()	
		persisted.eachLine { 
			def json = jsonSlurper.parseText(it)
			assert json.file == f.toString()
			assert json.length == f.length()
			assert json.infoHash == Base32.encode(ih.getRoot())
			assert json.hashList == [Base32.encode(ih.getHashList())]
		}
	}

}
