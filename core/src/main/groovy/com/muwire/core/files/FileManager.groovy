package com.muwire.core.files

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.SharedFile
import com.muwire.core.UILoadedEvent
import com.muwire.core.search.ResultsEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.SearchIndex
import com.muwire.core.util.DataUtil

import groovy.util.logging.Log
import net.i2p.data.Base64

@Log
class FileManager {


    final EventBus eventBus
    final MuWireSettings settings
    final Map<InfoHash, Set<SharedFile>> rootToFiles = Collections.synchronizedMap(new HashMap<>())
    final Map<File, SharedFile> fileToSharedFile = Collections.synchronizedMap(new HashMap<>())
    final Map<String, Set<File>> nameToFiles = new HashMap<>()
    final Map<String, Set<File>> commentToFile = new HashMap<>()
    final SearchIndex index = new SearchIndex()

    FileManager(EventBus eventBus, MuWireSettings settings) {
        this.settings = settings
        this.eventBus = eventBus
    }

    void onFileHashedEvent(FileHashedEvent e) {
        if (e.sharedFile != null)
            addToIndex(e.sharedFile)
    }

    void onFileLoadedEvent(FileLoadedEvent e) {
        addToIndex(e.loadedFile)
    }

    void onFileDownloadedEvent(FileDownloadedEvent e) {
        if (settings.shareDownloadedFiles) {
            addToIndex(e.downloadedFile)
        }
    }

    private void addToIndex(SharedFile sf) {
        log.info("Adding shared file " + sf.getFile())
        InfoHash infoHash = sf.getInfoHash()
        Set<SharedFile> existing = rootToFiles.get(infoHash)
        if (existing == null) {
            log.info("adding new root")
            existing = new HashSet<>()
            rootToFiles.put(infoHash, existing);
        }
        existing.add(sf)
        fileToSharedFile.put(sf.file, sf)

        String name = sf.getFile().getName()
        Set<File> existingFiles = nameToFiles.get(name)
        if (existingFiles == null) {
            existingFiles = new HashSet<>()
            nameToFiles.put(name, existingFiles)
        }
        existingFiles.add(sf.getFile())

        String comment = sf.getComment()
        if (comment != null) {
            comment = DataUtil.readi18nString(Base64.decode(comment))
            index.add(comment)
            Set<File> existingComment = commentToFile.get(comment)
            if(existingComment == null) {
                existingComment = new HashSet<>()
                commentToFile.put(comment, existingComment)
            }
            existingComment.add(sf.getFile())
        }
        
        index.add(name)
    }

    void onFileUnsharedEvent(FileUnsharedEvent e) {
        SharedFile sf = e.unsharedFile
        InfoHash infoHash = sf.getInfoHash()
        Set<SharedFile> existing = rootToFiles.get(infoHash)
        if (existing != null) {
            existing.remove(sf)
            if (existing.isEmpty()) {
                rootToFiles.remove(infoHash)
            }
        }

        fileToSharedFile.remove(sf.file)

        String name = sf.getFile().getName()
        Set<File> existingFiles = nameToFiles.get(name)
        if (existingFiles != null) {
            existingFiles.remove(sf.file)
            if (existingFiles.isEmpty()) {
                nameToFiles.remove(name)
            }
        }
        
        String comment = sf.getComment()
        if (comment != null) {
            Set<File> existingComment = commentToFile.get(comment)
            if (existingComment != null) {
                existingComment.remove(sf.getFile())
                if (existingComment.isEmpty()) {
                    commentToFile.remove(comment)
                    index.remove(comment)
                }
            }
        }

        index.remove(name)
    }
    
    void onUICommentEvent(UICommentEvent e) {
        if (e.oldComment != null) {
            def comment = DataUtil.readi18nString(Base64.decode(e.oldComment))
            Set<File> existingFiles = commentToFile.get(comment) 
            existingFiles.remove(e.sharedFile.getFile())
            if (existingFiles.isEmpty()) {
                commentToFile.remove(comment)
                index.remove(comment)
            }
        }
        
        String comment = e.sharedFile.getComment()
        comment = DataUtil.readi18nString(Base64.decode(comment))
        if (comment != null) {
            index.add(comment)
            Set<File> existingComment = commentToFile.get(comment)
            if(existingComment == null) {
                existingComment = new HashSet<>()
                commentToFile.put(comment, existingComment)
            }
            existingComment.add(e.sharedFile.getFile())
        }        
    }

    Map<File, SharedFile> getSharedFiles() {
        synchronized(fileToSharedFile) {
            return new HashMap<>(fileToSharedFile)
        }
    }

    Set<SharedFile> getSharedFiles(byte []root) {
            return rootToFiles.get(new InfoHash(root))
    }

    void onSearchEvent(SearchEvent e) {
        // hash takes precedence
        ResultsEvent re = null
        if (e.searchHash != null) {
            Set<SharedFile> found
            found = rootToFiles.get new InfoHash(e.searchHash)
            found = filter(found, e.oobInfohash)
            if (found != null && !found.isEmpty())
                re = new ResultsEvent(results: found.asList(), uuid: e.uuid, searchEvent: e)
        } else {
            def names = index.search e.searchTerms
            Set<File> files = new HashSet<>()
            names.each { 
                files.addAll nameToFiles.getOrDefault(it, [])
                files.addAll commentToFile.getOrDefault(it, []) 
            }
            Set<SharedFile> sharedFiles = new HashSet<>()
            files.each { sharedFiles.add fileToSharedFile[it] }
            files = filter(sharedFiles, e.oobInfohash)
            
            if (!sharedFiles.isEmpty())
                re = new ResultsEvent(results: sharedFiles.asList(), uuid: e.uuid, searchEvent: e)

        }

        if (re != null)
            eventBus.publish(re)
    }

    private static Set<SharedFile> filter(Set<SharedFile> files, boolean oob) {
        if (!oob)
            return files
        Set<SharedFile> rv = new HashSet<>()
        files.each {
            if (it.getPieceSize() != 0)
                rv.add(it)
        }
        rv
    }

    void onDirectoryUnsharedEvent(DirectoryUnsharedEvent e) {
        e.directory.listFiles().each {
            if (it.isDirectory())
                eventBus.publish(new DirectoryUnsharedEvent(directory : it))
            else {
                SharedFile sf = fileToSharedFile.get(it)
                if (sf != null)
                    eventBus.publish(new FileUnsharedEvent(unsharedFile : sf))
            }
        }
    }
}
