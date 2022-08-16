package com.muwire.core.files

import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

import static java.nio.file.StandardWatchEventKinds.*

import java.nio.file.ClosedWatchServiceException
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.SharedFile
import com.muwire.core.files.directories.WatchedDirectoryConfigurationEvent
import com.muwire.core.files.directories.WatchedDirectoryConvertedEvent
import com.muwire.core.files.directories.WatchedDirectoryManager

import groovy.util.logging.Log
import net.i2p.util.SystemVersion

@Log
class DirectoryWatcher {

    private static final long WAIT_TIME = 3000

    private static final WatchEvent.Kind[] kinds
    static {
        if (SystemVersion.isMac())
            kinds = [ENTRY_MODIFY, ENTRY_DELETE]
        else
            kinds = [ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE]
    }

    private final File home
    private final EventBus eventBus
    private final MuWireSettings settings
    private final FileManager fileManager
    private final WatchedDirectoryManager watchedDirectoryManager
    private final NegativeFiles negativeFiles
    private final Thread watcherThread, publisherThread
    private final Map<File, WaitingEntry> waitingFiles = new ConcurrentHashMap<>()
    private final Map<File, WatchKey> watchedDirectories = new ConcurrentHashMap<>()
    private WatchService watchService
    private volatile boolean shutdown

    DirectoryWatcher(EventBus eventBus, FileManager fileManager, File home, 
                     WatchedDirectoryManager watchedDirectoryManager, NegativeFiles negativeFiles,
                     MuWireSettings settings) {
        this.home = home
        this.eventBus = eventBus
        this.settings = settings
        this.fileManager = fileManager
        this.watchedDirectoryManager = watchedDirectoryManager
        this.negativeFiles = negativeFiles
        this.watcherThread = new Thread({watch() } as Runnable, "directory-watcher")
        watcherThread.setDaemon(true)
        this.publisherThread = new Thread({publish()} as Runnable, "watched-files-publisher")
        publisherThread.setDaemon(true)
    }

    void onWatchedDirectoryConvertedEvent(WatchedDirectoryConvertedEvent e) {
        watchService = FileSystems.getDefault().newWatchService()
        watcherThread.start()
        publisherThread.start()
    }

    void stop() {
        shutdown = true
        watcherThread?.interrupt()
        publisherThread?.interrupt()
        watchService?.close()
    }

    void onDirectoryWatchedEvent(DirectoryWatchedEvent e) {
        log.fine("DW: onDirectoryWatchedEvent $e")
        File canonical = e.directory.getCanonicalFile()
        if (e.watch) {
            watchedDirectories.computeIfAbsent(canonical, {
                Path path = canonical.toPath()
                path.register(watchService, kinds)
            })
        } else {
            watchedDirectories.remove(canonical)?.cancel()
        }
    }

    void onWatchedDirectoryConfigurationEvent(WatchedDirectoryConfigurationEvent e) {
        if (watchService == null)
            return // still converting
        e.toApply.each {
            if (!e.autoWatch) {
                WatchKey wk = watchedDirectories.remove(it)
                wk?.cancel()
            } else if (!watchedDirectories.containsKey(it)) {
                Path path = it.toPath()
                def wk = path.register(watchService, kinds)
                watchedDirectories.put(it, wk)
            } // else it was already watched
        }
    }
    
    private void watch() {
        try {
            while(!shutdown) {
                WatchKey key = watchService.take()
                key.pollEvents().each {
                    switch(it.kind()) {
                        case ENTRY_CREATE: processCreated(key.watchable(), it.context()); break
                        case ENTRY_MODIFY: processModified(key.watchable(), it.context()); break
                        case ENTRY_DELETE: processDeleted(key.watchable(), it.context()); break
                    }
                }
                key.reset()
            }
        } catch (InterruptedException|ClosedWatchServiceException e) {
            if (!shutdown)
                throw e
        }
    }


    private void processCreated(Path parent, Path path) {
        File f = join(parent, path)
        if (!settings.shareHiddenFiles && f.isHidden())
            return
        log.fine("created entry $f")
        if (f.isDirectory())
            eventBus.publish(new FileSharedEvent(file : f, fromWatch : true))
        else
            waitingFiles.put(f, new WaitingEntry(System.currentTimeMillis(), false, f.length())) 
    }

    private void processModified(Path parent, Path path) {
        File f = join(parent, path)
        log.fine("modified entry $f")
        if (!settings.shareHiddenFiles && f.isHidden())
            return
        if (!negativeFiles.negativeTree.get(f))
            waitingFiles.put(f, new WaitingEntry(System.currentTimeMillis(), true, f.length()))
    }

    private void processDeleted(Path parent, Path path) {
        File f = join(parent, path)
        log.fine("deleted entry $f => ${f.getCanonicalFile()}")
        SharedFile sf = fileManager.fileToSharedFile.get(f)
        if (sf != null)
            eventBus.publish(new FileUnsharedEvent(unsharedFiles : new SharedFile[]{sf}, deleted : true))
        else if (watchedDirectoryManager.isWatched(f)) 
            eventBus.publish(new DirectoryUnsharedEvent(directories : new File[]{f}, deleted : true))
        else
            log.fine("Entry was not relevant");
    }

    private static File join(Path parent, Path path) {
        File parentFile = parent.toFile()
        new File(parentFile, path.toFile().getName())
    }

    private void publish() {
        try {
            while(!shutdown) {
                Thread.sleep(WAIT_TIME)
                long now = System.currentTimeMillis()
                def published = []
                waitingFiles.each { file, waitingEntry ->
                    if (now - waitingEntry.timestamp > WAIT_TIME) {
                        final long length = file.length()
                        if (length != waitingEntry.length) {
                            log.fine("${file} length changed during wait period")
                            waitingEntry.length = length
                            return
                        }
                        try (FileChannel fc = Files.newByteChannel(file.toPath(), StandardOpenOption.READ)) {
                            try {
                                def lock = fc.tryLock(0, Long.MAX_VALUE, true)
                                if (lock == null) {
                                    log.fine("Couldn't acquire read lock on $file will try again")
                                    return
                                }
                                lock.release()
                            } catch (OverlappingFileLockException ofle) {
                                log.fine("file $file has already started hashing")
                                return
                            }
                            published << file
                            if (waitingEntry.modified) {
                                SharedFile sf = fileManager.fileToSharedFile.get(file)
                                if (sf != null) {
                                    log.fine ("modified shared $file")
                                    eventBus.publish(new FileModifiedEvent(sharedFile: sf))
                                    return 
                                }
                            }
                            log.fine("publishing file $file")
                            eventBus.publish new FileSharedEvent(file: file, fromWatch: true)
                        } catch (IOException cantOpen) {
                            log.fine("couldn't open file $file for reading, will try again")
                        }
                    }
                }
                published.each {
                    waitingFiles.remove(it)
                }
            }
        } catch (InterruptedException e) {
            if (!shutdown)
                throw e
        }
    }
    
    private static class WaitingEntry {
        private final long timestamp
        private final boolean modified
        private volatile long length
        WaitingEntry(long timestamp, boolean modified, long length) {
            this.timestamp = timestamp
            this.modified = modified
            this.length = length
        }
    }
}
