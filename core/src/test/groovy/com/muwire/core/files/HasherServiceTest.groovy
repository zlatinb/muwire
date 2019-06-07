package com.muwire.core.files

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

import org.junit.After
import org.junit.Before
import org.junit.Test

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings

class HasherServiceTest {

	HasherService service
	FileHasher hasher
    EventBus eventBus
	def listener = new ArrayBlockingQueue(100) {
		void onFileHashedEvent(FileHashedEvent evt) {
			offer evt
		}
	}
	
	@Before
	void before() {
        eventBus = new EventBus()
		hasher = new FileHasher()
		service = new HasherService(hasher, eventBus, new FileManager(eventBus, new MuWireSettings()))
        eventBus.register(FileHashedEvent.class, listener)
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
			def hashed = listener.poll(1000, TimeUnit.MILLISECONDS)
			if (hashed == null)
				break
			fileNames.add(hashed.sharedFile?.file?.getName())
		}
		assert fileNames.contains("build.gradle")
		assert fileNames.contains("HasherServiceTest.groovy")
	}
}
