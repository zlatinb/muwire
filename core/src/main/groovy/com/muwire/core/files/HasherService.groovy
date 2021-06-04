package com.muwire.core.files

import com.muwire.core.util.DataUtil

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.Executors

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.SharedFile

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class HasherService {
    
    private static final ThreadLocal<FileHasher> HASHER_TL = new ThreadLocal<FileHasher>() {

        @Override
        protected FileHasher initialValue() {
            return new FileHasher()
        }
        
    } 

    final EventBus eventBus
    final FileManager fileManager
    final Set<File> hashed = new HashSet<>()
    final MuWireSettings settings
    
    private static final int TARGET_Q_SIZE = 1024 * 8
    private final BlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>()
    Executor throttlerExecutor
    Executor executor
    private int currentHashes 

    HasherService(EventBus eventBus, FileManager fileManager, MuWireSettings settings) {
        this.eventBus = eventBus
        this.fileManager = fileManager
        this.settings = settings
    }

    void start() {
        executor = Executors.newCachedThreadPool()
        throttlerExecutor = new ThreadPoolExecutor(1, 1, 0L, 
                TimeUnit.MILLISECONDS, runnables)
    }

    void onFileSharedEvent(FileSharedEvent evt) {
        File canonical = evt.file.getCanonicalFile()
        if (!settings.shareHiddenFiles && canonical.isHidden())
            return
        if (fileManager.fileToSharedFile.containsKey(canonical)) 
            return
        if (canonical.isFile() && fileManager.negativeTree.fileToNode.containsKey(canonical))
            return
        if (settings.ignoredFileTypes.contains(DataUtil.getFileExtension(canonical)))
            return
        if (canonical.getName().endsWith(".mwcollection"))
            return
        if (canonical.getName().endsWith(".mwcomment")) { 
            if (canonical.length() <= Constants.MAX_COMMENT_LENGTH)
                eventBus.publish(new SideCarFileEvent(file : canonical))
        } else if (hashed.add(canonical)) {
            if (canonical.isDirectory())
                executor.execute({processDirectory(canonical)} as Runnable)
            else
                throttlerExecutor.execute({ throttle(canonical) } as Runnable)
        }
    }
    
    void onFileUnsharedEvent(FileUnsharedEvent evt) {
        hashed.remove(evt.unsharedFile.file)
    }
    
    void onDirectoryUnsharedEvent(DirectoryUnsharedEvent evt) {
        hashed.remove(evt.directory)
    }

    private synchronized void throttle(File f) {
        while(currentHashes >= settings.hashingCores)
            wait(10)
        currentHashes++
        executor.execute({processFile(f)} as Runnable)
    }
    
    private void processDirectory(File f) {
        try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(f.toPath())) {
            for (Path p : directoryStream) {
                synchronized(this) {
                    while(runnables.size() >= TARGET_Q_SIZE)
                        wait(10)
                }
                eventBus.publish new FileSharedEvent(file : p.toFile())
            }
        }
    }
    
    private void processFile(File f) {
        try {
            if (f.length() == 0) {
                eventBus.publish new FileHashedEvent(error: "Not sharing empty file $f")
            } else if (f.length() > FileHasher.MAX_SIZE) {
                eventBus.publish new FileHashedEvent(error: "$f is too large to be shared ${f.length()}")
            } else if (!f.canRead()) {
                eventBus.publish(new FileHashedEvent(error: "$f cannot be read"))
            } else {
                eventBus.publish new FileHashingEvent(hashingFile: f)
                def hash = HASHER_TL.get().hashFile f
                eventBus.publish new FileHashedEvent(sharedFile: new SharedFile(f, hash.getRoot(), FileHasher.getPieceSize(f.length())),
                        infoHash: hash)
            }
        } finally {
            synchronized (this) {
                currentHashes--
                this.notifyAll()
            }
        }
    }
}
