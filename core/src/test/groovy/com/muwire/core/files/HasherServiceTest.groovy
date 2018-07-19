package com.muwire.core.files

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

import org.junit.After
import org.junit.Before
import org.junit.Test

class HasherServiceTest {

	HasherService service
	FileHasher hasher
	def listener = new ArrayBlockingQueue(100) {
		void publish(def evt) {
			offer evt
		}
	}
	
	@Before
	void before() {
		hasher = new FileHasher()
		service = new HasherService(hasher, listener)
		service.start()
	}
	
	@After
	void after() {
		listener.clear()
	}
	
	@Test
	void testSingleFile() {
		File f = new File("build.gradle")
		service.onFileSharedEvent new FileSharedEvent(file: f)
		Thread.sleep(100)
		def hashed = listener.poll()
		assert hashed instanceof FileHashedEvent
		assert hashed.sharedFile.file == f.getCanonicalFile()
		assert hashed.sharedFile.infoHash != null
		assert listener.isEmpty()
	}
	
	@Test
	void testDirectory() {
		File f = new File(".")
		service.onFileSharedEvent new FileSharedEvent(file: f)
		Set<String> fileNames = new HashSet<>()
		while (true) {
			def hashed = listener.poll(100, TimeUnit.MILLISECONDS)
			if (hashed == null)
				break
			fileNames.add(hashed.sharedFile?.file?.getName())
		}
		assert fileNames.contains("build.gradle")
		assert fileNames.contains("HasherServiceTest.groovy")
	}
}
