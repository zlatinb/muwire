package com.muwire.core.files

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.muwire.core.Destinations
import com.muwire.core.DownloadedFile
import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile

import groovy.json.JsonSlurper
import net.i2p.data.Base32

class PersisterServiceSavingTest {

	File f
	FileHasher fh = new FileHasher()
	InfoHash ih
	SharedFile sf
	def fileSource
	EventBus eventBus = new EventBus()
	File persisted
	PersisterService ps
	
	@Before
	void before() {
		f = new File("build.gradle")
		f = f.getCanonicalFile()
		ih = fh.hashFile(f)
		fileSource = new Object() {
					Map<File, SharedFile> getSharedFiles() {
						Map<File, SharedFile> rv = new HashMap<>()
						rv.put(f, sf)
						rv
					}
				}
		persisted = new File("persisted")
		persisted.delete()
		persisted.deleteOnExit()
	}
	
	@After
	void after() {
		ps?.stop()
	}
	
	@Test
	void testSavingSharedFile() {
		sf = new SharedFile(f, ih)
		
		ps = new PersisterService(persisted, eventBus, 100, fileSource)
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
	
	@Test
	void testSavingDownloadedFile() {
		Destinations dests = new Destinations()
		sf = new DownloadedFile(f, ih, new HashSet([dests.dest1, dests.dest2]))
		
		ps = new PersisterService(persisted, eventBus, 100, fileSource)
		ps.start()
		Thread.sleep(1500)
	
		JsonSlurper jsonSlurper = new JsonSlurper()
		persisted.eachLine {
			def json = jsonSlurper.parseText(it)
			assert json.file == f.toString()
			assert json.length == f.length()
			assert json.infoHash == Base32.encode(ih.getRoot())
			assert json.hashList == [Base32.encode(ih.getHashList())]
			assert json.sources.size() == 2
			assert json.sources.contains(dests.dest1.toBase64())
			assert json.sources.contains(dests.dest2.toBase64())
		}
	}

}
