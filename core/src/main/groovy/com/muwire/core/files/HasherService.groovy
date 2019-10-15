package com.muwire.core.files

import java.util.concurrent.Executor
import java.util.concurrent.Executors

import com.muwire.core.EventBus
import com.muwire.core.SharedFile

class HasherService {

    final FileHasher hasher
    final EventBus eventBus
    final FileManager fileManager
    final Set<File> hashed = new HashSet<>()
    Executor executor

    HasherService(FileHasher hasher, EventBus eventBus, FileManager fileManager) {
        this.hasher = hasher
        this.eventBus = eventBus
        this.fileManager = fileManager
    }

    void start() {
        executor = Executors.newSingleThreadExecutor()
    }

    void onFileSharedEvent(FileSharedEvent evt) {
        File canonical = evt.file.getCanonicalFile()
        if (fileManager.fileToSharedFile.containsKey(canonical)) 
            return
        if (hashed.add(canonical))
            executor.execute( { -> process(canonical) } as Runnable)
    }
    
    void onFileUnsharedEvent(FileUnsharedEvent evt) {
        hashed.remove(evt.unsharedFile.file)
    }
    
    void onDirectoryUnsharedEvent(DirectoryUnsharedEvent evt) {
        hashed.remove(evt.directory)
    }

    private void process(File f) {
        if (f.isDirectory()) {
            f.listFiles().each {eventBus.publish new FileSharedEvent(file: it) }
        } else {
            if (f.length() == 0) {
                eventBus.publish new FileHashedEvent(error: "Not sharing empty file $f")
            } else if (f.length() > FileHasher.MAX_SIZE) {
                eventBus.publish new FileHashedEvent(error: "$f is too large to be shared ${f.length()}")
            } else {
                eventBus.publish new FileHashingEvent(hashingFile: f)
                def hash = hasher.hashFile f
                eventBus.publish new FileHashedEvent(sharedFile: new SharedFile(f, hash, FileHasher.getPieceSize(f.length())))
            }
        }
    }
}
