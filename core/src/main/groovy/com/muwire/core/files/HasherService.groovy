package com.muwire.core.files

import java.util.concurrent.Executor
import java.util.concurrent.Executors

import com.muwire.core.SharedFile

class HasherService {

	final FileHasher hasher
	final def listener
	Executor executor
	
	HasherService(FileHasher hasher, def listener) {
		this.hasher = hasher
		this.listener = listener
	}
	
	void start() {
		executor = Executors.newSingleThreadExecutor()
	}
	
	void onFileSharedEvent(FileSharedEvent evt) {
		executor.execute( { -> process(evt.file) } as Runnable)
	}
	
	private void process(File f) {
		f = f.getCanonicalFile()
		if (f.isDirectory()) {
			f.listFiles().each {onFileSharedEvent new FileSharedEvent(file: it) }
		} else {
			if (f.length() == 0) {
				listener.publish new FileHashedEvent(error: "Not sharing empty file $f")
			} else if (f.length() > FileHasher.MAX_SIZE) {
				listener.publish new FileHashedEvent(error: "$f is too large to be shared ${f.length()}")
			} else {
				def hash = hasher.hashFile f
				listener.publish new FileHashedEvent(sharedFile: new SharedFile(f, hash))
			}
		}
	}
}
