package com.muwire.core.files

import com.muwire.core.InfoHash
import com.muwire.core.util.DataUtil
import groovy.util.logging.Log

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
import java.util.function.Function

@Log
class HasherService {
    
    private static final ThreadLocal<FileHasher> HASHER_TL = new ThreadLocal<FileHasher>() {

        @Override
        protected FileHasher initialValue() {
            return new FileHasher()
        }
        
    } 

    final EventBus eventBus
    final FileManager fileManager
    private final NegativeFiles negativeFiles
    private final Function<File, InfoHash> hashListFunction
    final Set<File> hashed = new HashSet<>()
    final MuWireSettings settings
    
    private static final int TARGET_Q_SIZE = 1024 * 8
    private final BlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>()
    Executor throttlerExecutor
    Executor executor
    private int currentHashes
    private long totalHashes

    HasherService(EventBus eventBus, FileManager fileManager, NegativeFiles negativeFiles,
                  Function<File,InfoHash> hashListFunction, MuWireSettings settings) {
        this.eventBus = eventBus
        this.fileManager = fileManager
        this.negativeFiles = negativeFiles
        this.hashListFunction = hashListFunction
        this.settings = settings
    }

    void start() {
        executor = Executors.newCachedThreadPool()
        throttlerExecutor = new ThreadPoolExecutor(1, 1, 0L, 
                TimeUnit.MILLISECONDS, runnables)
    }

    void onFileSharedEvent(FileSharedEvent evt) {
        if (!settings.shareHiddenFiles && evt.file.isHidden())
            return
        if (fileManager.fileToSharedFile.containsKey(evt.file)) 
            return
        if (negativeFiles.negativeTree.get(evt.file))
            return
        String extension = DataUtil.getFileExtension(evt.file)
        if (extension != "" && settings.ignoredFileTypes.contains(extension))
            return
        if (evt.file.getName().endsWith(".mwcollection"))
            return
        if (evt.file.getName().endsWith(".mwcomment")) { 
            if (evt.file.length() <= Constants.MAX_COMMENT_LENGTH)
                eventBus.publish(new SideCarFileEvent(file : evt.file))
        } else if (hashed.add(evt.file)) {
            File canonical = evt.file.getCanonicalFile()
            if (canonical.isDirectory())
                executor.execute({processDirectory(canonical)} as Runnable)
            else
                throttlerExecutor.execute({ throttle(evt.file, canonical) } as Runnable)
        }
    }
    
    void onFileUnsharedEvent(FileUnsharedEvent evt) {
        for (SharedFile sharedFile : evt.unsharedFiles)
            hashed.remove(sharedFile.file)
    }
    
    void onDirectoryUnsharedEvent(DirectoryUnsharedEvent evt) {
        for(File dir : evt.directories)
            hashed.remove(dir)
    }

    private synchronized void throttle(File f, File canonical) {
        while(currentHashes >= settings.hashingCores)
            wait(10)
        currentHashes++
        if (++totalHashes % TARGET_Q_SIZE == 0)
            System.gc()
        executor.execute({processFile(f, canonical)} as Runnable)
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
    
    private void processFile(File f, File canonical) {
        try {
            if (f.length() == 0) {
                eventBus.publish new FileHashedEvent(error: "Not sharing empty file $f")
            } else if (f.length() > FileHasher.MAX_SIZE) {
                eventBus.publish new FileHashedEvent(error: "$f is too large to be shared ${f.length()}")
            } else if (!f.canRead()) {
                eventBus.publish(new FileHashedEvent(error: "$f cannot be read"))
            } else {
                eventBus.publish new FileHashingEvent(hashingFile: f)
                def hash = hashListFunction.apply(canonical)
                if (hash == null) {
                    log.fine("did not find a hash list for $f => $canonical")
                    hash = HASHER_TL.get().hashFile canonical
                    eventBus.publish new InfoHashEvent(file: canonical, infoHash: hash)
                } else
                    log.fine("found an existing hash list for $f => $canonical")
                def sf = new SharedFile(f, hash.getRoot(), FileHasher.getPieceSize(f.length()))
                eventBus.publish new FileHashedEvent(sharedFile: sf)
            }
        } finally {
            synchronized (this) {
                currentHashes--
                this.notifyAll()
            }
        }
    }
}
