package com.muwire.core.files.directories

import com.muwire.core.MuWireSettings
import com.muwire.core.files.AllFilesLoadedEvent

import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.stream.Stream

import com.muwire.core.EventBus
import com.muwire.core.SharedFile
import com.muwire.core.files.DirectoryUnsharedEvent
import com.muwire.core.files.DirectoryWatchedEvent
import com.muwire.core.files.FileListCallback
import com.muwire.core.files.FileManager
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.files.FileUnsharedEvent

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log

@Log
class WatchedDirectoryManager {
    
    private final File home
    private final MuWireSettings settings
    private final EventBus eventBus
    private final FileManager fileManager
    
    private final Map<File, WatchedDirectory> watchedDirs = new HashMap<>()
    private final Map<File, WatchedDirectory> aliasesMap = new HashMap<>()
    
    private final ExecutorService diskIO = Executors.newSingleThreadExecutor({r -> 
        Thread t = new Thread(r, "disk-io")
        t.setDaemon(true)
        t
    } as ThreadFactory)
    
    private final Timer timer = new Timer("directory-timer", true)
    
    private boolean converting = true
    
    WatchedDirectoryManager(File home, EventBus eventBus, FileManager fileManager, MuWireSettings settings) {
        this.home = new File(home, "directories")
        this.home.mkdir()
        this.eventBus = eventBus
        this.fileManager = fileManager
        this.settings = settings
    }
    /**
     * Used by the plugin only
     * TODO: fix to work with symlinks and locking
     */
    synchronized Stream<WatchedDirectory> getWatchedDirsStream() {
        watchedDirs.values().stream()
    }
    
    synchronized boolean isWatched(File f) {
        watchedDirs.containsKey(f) || aliasesMap.containsKey(f)
    }
    
    synchronized WatchedDirectory getDirectory(File file) {
        if (watchedDirs.containsKey(file))
            return watchedDirs[file]
        return aliasesMap[file]
    }
    
    public void shutdown() {
        diskIO.shutdown()
        timer.cancel()
    }
    
    synchronized void onUISyncDirectoryEvent(UISyncDirectoryEvent e) {
        def wd = watchedDirs.get(e.directory)
        if (wd == null) {
            log.warning("Got a sync event for non-watched dir ${e.directory}")
            return
        }
        diskIO.submit({sync(wd, System.currentTimeMillis())} as Runnable)
    }
    
    void onWatchedDirectoryConfigurationEvent(WatchedDirectoryConfigurationEvent e) {
        if (converting) {
            def newDir = new WatchedDirectory(e.directory)
            // conversion is always autowatch really
            newDir.autoWatch = e.autoWatch
            persist(newDir)
        } else {
            e.toApply.each {
                def wd
                synchronized (this) {
                    wd = watchedDirs.get(it)
                }
                if (wd == null) {
                    log.severe("got a configuration event for a non-watched directory ${it}")
                    return
                }
                wd.autoWatch = e.autoWatch
                wd.syncInterval = e.syncInterval
                persist(wd)
            }
        }
    }
    
    void onWatchedDirectoryConvertedEvent(WatchedDirectoryConvertedEvent e) {
        converting = false
        diskIO.submit({
            def slurper = new JsonSlurper()
            Files.walk(home.toPath()).filter({
                it.getFileName().toString().endsWith(".json")
            }).
            parallel().
            forEach {
                def parsed = slurper.parse(it.toFile())
                WatchedDirectory wd = WatchedDirectory.fromJson(parsed)
                if (wd.directory.exists() && wd.directory.isDirectory() && // check if directory disappeared or hidden
                        (settings.shareHiddenFiles || !wd.directory.isHidden())) {
                    synchronized (this) {
                        watchedDirs.put(wd.canonical, wd)
                    }
                }
                else
                    it.toFile().delete()
            }
            eventBus.publish(new WatchedDirectoriesLoadedEvent())
        } as Runnable)
    }
    
    synchronized void onAllFilesLoadedEvent(AllFilesLoadedEvent event) {
        watchedDirs.values().stream().filter({it.autoWatch}).
                flatMap({it.aliases.stream()}).
                forEach {
                    eventBus.publish(new DirectoryWatchedEvent(directory : it, watch: true))
                    eventBus.publish(new FileSharedEvent(file : it))
                }
        timer.schedule({sync()} as TimerTask, 1000, 1000)
    }
    
    private void persist(WatchedDirectory dir) {
        diskIO.submit({doPersist(dir)} as Runnable)
    }
    
    private void doPersist(WatchedDirectory dir) {
        def json = JsonOutput.toJson(dir.toJson())
        def targetFile = new File(home, dir.getEncodedName() + ".json")
        targetFile.text = json
    }
    
    void onFileSharedEvent(FileSharedEvent e) {
        if (e.file.isFile())
            return
        if (!settings.shareHiddenFiles && e.file.isHidden())
            return
        
        
        def wd = new WatchedDirectory(e.file) // canonicalize outside of lock
        log.fine("WDM: watching directory ${e.file} => ${wd.canonical}")
        
        synchronized (this) {
            if (watchedDirs.containsKey(wd.canonical)) {
                log.fine("WDM: ${wd.canonical} was already watched, adding alias ${e.file}")
                watchedDirs[wd.canonical].aliases.add(e.file)
                aliasesMap.put(e.file, watchedDirs[wd.canonical])
                return
            }
            if (e.fromWatch) {
                // parent should be already watched, copy settings
                def parent = watchedDirs.get(e.file.getParentFile())
                if (parent == null) {
                    log.severe("watching found a directory without a watched parent? ${e.file}")
                    return
                }
                wd.autoWatch = parent.autoWatch
                wd.syncInterval = parent.syncInterval
            } else
                wd.autoWatch = true
            
            watchedDirs.put(wd.canonical, wd)
            aliasesMap.put(wd.directory, wd)
        }
        
        persist(wd)
        if (wd.autoWatch) 
            eventBus.publish(new DirectoryWatchedEvent(directory: wd.directory, watch: true))
    }
    
    void onDirectoryUnsharedEvent(DirectoryUnsharedEvent e) {
        List<WatchedDirectory> toRemove = new ArrayList<>()
        synchronized (this) {
            for (File dir : e.directories) {
                if (!isWatched(dir)) {
                    log.warning("unshared a directory that wasn't watched? ${dir}")
                    continue
                }
                
                // 1. see if the canonical contains it
                def wd = watchedDirs.remove(dir)
                if (wd != null) {
                    log.fine("WDM: will remove watched directory ${dir} and ${wd.aliases.size()} aliases")
                    // remove all aliases
                    aliasesMap.keySet().removeAll(wd.aliases)
                    toRemove << wd
                } else {
                    wd = aliasesMap.remove(dir)
                    log.fine("WDM: removing $dir from aliases of ${wd.canonical}")
                    
                    wd.aliases.remove(dir)
                    if (!wd.aliases.isEmpty())
                        continue
                    
                    log.fine("WDM: no more aliases for ${wd.canonical}")
                    watchedDirs.remove(wd.canonical)
                    toRemove << wd
                }
            }
        }
        log.fine "will un-watch ${toRemove.size()} directories"
        if (!toRemove.isEmpty()) {
            for (WatchedDirectory wd : toRemove) {
                eventBus.publish new DirectoryWatchedEvent(directory: wd.canonical, watch: false)
            }
            diskIO.submit({
                for (WatchedDirectory wd : toRemove) {
                    File persistFile = new File(home, wd.getEncodedName() + ".json")
                    persistFile.delete()
                }
            } as Runnable)
        }
    }
    
    private synchronized void sync() {
        long now = System.currentTimeMillis()
        watchedDirs.values().stream().
            filter({!it.autoWatch}).
            filter({it.syncInterval > 0}).
            filter({it.lastSync + it.syncInterval * 1000 < now}).
            forEach({wd -> diskIO.submit({sync(wd, now)} as Runnable )})
    }
    
    private void sync(WatchedDirectory wd, long now) {
        log.fine("syncing ${wd.directory}")
        wd.lastSync = now
        doPersist(wd)
        eventBus.publish(new WatchedDirectorySyncEvent(directory: wd.directory, when: now))
        
        def cb = new DirSyncCallback()
        fileManager.positiveTree.list(wd.directory, cb)
        
        Set<File> filesOnFS = new HashSet<>()
        Set<File> dirsOnFS = new HashSet<>()
        wd.directory.listFiles().each {
            if (it.isFile())
                filesOnFS.add(it)
            else
                dirsOnFS.add(it)
        }
        
        Set<File> addedFiles = new HashSet<>(filesOnFS)
        addedFiles.removeAll(cb.files)
        addedFiles.each { 
            eventBus.publish(new FileSharedEvent(file : it, fromWatch : true))
        }
        Set<File> addedDirs = new HashSet<>(dirsOnFS)
        addedDirs.removeAll(cb.dirs)
        addedDirs.each { 
            eventBus.publish(new FileSharedEvent(file : it, fromWatch : true))
        }
        
        Set<File> deletedFiles = new HashSet<>(cb.files)
        deletedFiles.removeAll(filesOnFS)
        eventBus.publish(new FileUnsharedEvent(unsharedFiles: deletedFiles.toArray(new SharedFile[0]), deleted: true))
        Set<File> deletedDirs = new HashSet<>(cb.dirs)
        deletedDirs.removeAll(dirsOnFS)
        deletedDirs.each {
            eventBus.publish(new DirectoryUnsharedEvent(directory : it, deleted: true))
        }
    }
    
    private static class DirSyncCallback implements FileListCallback<SharedFile> {
        
        private final Set<File> files = new HashSet<>()
        private final Set<File> dirs = new HashSet<>()

        @Override
        public void onFile(File f, SharedFile value) {
            files.add(f)
        }

        @Override
        public void onDirectory(File f) {
            dirs.add(f)
        }
        
    }
}
