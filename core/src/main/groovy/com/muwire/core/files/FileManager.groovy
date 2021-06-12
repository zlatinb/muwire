package com.muwire.core.files

import java.util.stream.Collectors
import java.util.stream.Stream

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
    final Map<InfoHash, SharedFile[]> rootToFiles = Collections.synchronizedMap(new HashMap<>())
    final Map<File, SharedFile> fileToSharedFile = Collections.synchronizedMap(new HashMap<>())
    final Map<String, File[]> nameToFiles = new HashMap<>()
    final Map<String, Set<File>> commentToFile = new HashMap<>()
    final SearchIndex index
    final FileTree<Void> negativeTree = new FileTree<>()
    final FileTree<SharedFile> positiveTree = new FileTree<>()
    final Set<File> sideCarFiles = new HashSet<>()

    FileManager(EventBus eventBus, MuWireSettings settings) {
        this.settings = settings
        this.eventBus = eventBus
        this.index = new SearchIndex("fileManager")
        for (String negative : settings.negativeFileTree) {
            negativeTree.add(new File(negative), null)
        }
    }
    
    void onFileHashedEvent(FileHashedEvent e) {
        if (e.sharedFile == null)
            return
        File f = e.sharedFile.getFile()
        if (sideCarFiles.remove(f)) {
            File sideCar = new File(f.getParentFile(), f.getName() + ".mwcomment")
            if (sideCar.exists()) 
                e.sharedFile.setComment(Base64.encode(DataUtil.encodei18nString(sideCar.text)))
        }
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
    
    void onSideCarFileEvent(SideCarFileEvent e) {
        String name = e.file.getName()
        name = name.substring(0, name.length() - ".mwcomment".length())
        File target = new File(e.file.getParentFile(), name)
        SharedFile existing = fileToSharedFile.get(target)
        if (existing == null) {
            sideCarFiles.add(target)
            return
        }
        String comment = Base64.encode(DataUtil.encodei18nString(e.file.text))
        String oldComment = existing.getComment()
        existing.setComment(comment)
        eventBus.publish(new UICommentEvent(oldComment : oldComment, sharedFile : existing))
    }

    private void addToIndex(SharedFile sf) {
        log.info("Adding shared file " + sf.getFile())
        InfoHash infoHash = new InfoHash(sf.getRoot())
        SharedFile[] existing = rootToFiles.get(infoHash)
        if (existing == null) {
            log.info("adding new root")
            existing = new SharedFile[1]
            existing[0] = sf
        } else {
            Set<SharedFile> unique = new HashSet<>()
            existing.each {unique.add(it)}
            unique.add(sf)
            existing = unique.toArray(existing)
        }
        rootToFiles.put(infoHash, existing);
            
        fileToSharedFile.put(sf.file, sf)
        positiveTree.add(sf.file, sf);
        
        negativeTree.remove(sf.file)
        String parent = sf.getFile().getParent()
        if (parent != null && settings.watchedDirectories.contains(parent)) {
            negativeTree.add(sf.file.getParentFile(),null)
        }
        saveNegativeTree()

        String name = sf.getFile().getName()
        File[] existingFiles = nameToFiles.get(name)
        if (existingFiles == null) {
            existingFiles = new File[1]
            existingFiles[0] = sf.getFile()
        } else {
            Set<File> unique = new HashSet<>()
            existingFiles.each {unique.add(it)}
            unique.add(sf.getFile())
            existingFiles = unique.toArray(existingFiles)
        }
        nameToFiles.put(name, existingFiles)

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
        for(SharedFile sharedFile : e.unsharedFiles)
            unshareFile(sharedFile, e.deleted)
    }
    
    private void unshareFile(SharedFile sf, boolean deleted) {
        InfoHash infoHash = new InfoHash(sf.getRoot())
        SharedFile[] existing = rootToFiles.get(infoHash)
        if (existing != null) {
            Set<SharedFile> unique = new HashSet<>()
            existing.each {unique.add(it)}
            unique.remove(sf)
            if (unique.isEmpty()) {
                rootToFiles.remove(infoHash)
            } else {
                existing = unique.toArray(new SharedFile[0])
                rootToFiles.put(infoHash, existing)
            }
        }

        fileToSharedFile.remove(sf.file)
        positiveTree.remove(sf.file)
        if (!deleted && negativeTree.fileToNode.containsKey(sf.file.getParentFile())) {
            negativeTree.add(sf.file,null)
            saveNegativeTree()
        }

        String name = sf.getFile().getName()
        File[] existingFiles = nameToFiles.get(name)
        if (existingFiles != null) {
            Set<File> unique = new HashSet<>()
            unique.addAll(existingFiles)
            unique.remove(sf.file)
            if (unique.isEmpty()) {
                nameToFiles.remove(name)
            } else {
                existingFiles = unique.toArray(new File[0])
                nameToFiles.put(name, existingFiles)
            }
        }
        
        String comment = sf.getComment()
        if (comment != null) {
            comment = DataUtil.readi18nString(Base64.decode(comment))
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
    
    boolean isShared(InfoHash infoHash) {
        rootToFiles.containsKey(infoHash)
    }

    void onSearchEvent(SearchEvent e) {
        // hash takes precedence
        ResultsEvent re = null
        if (e.searchHash != null) {
            SharedFile[] found
            found = rootToFiles.get new InfoHash(e.searchHash)
            if (found != null && found.length > 0) {
                found.each { it.hit(e.persona, e.timestamp, "Hash Search") }
                re = new ResultsEvent(results: found, uuid: e.uuid, searchEvent: e)
            }
        } else {
            def names = index.search e.searchTerms
            Set<File> files = new HashSet<>()
            names.each { 
                files.addAll nameToFiles.getOrDefault(it, [])
                if (e.searchComments)
                    files.addAll commentToFile.getOrDefault(it, [])
            }
            Set<SharedFile> sharedFiles = new HashSet<>()
            files.each { sharedFiles.add fileToSharedFile[it] }
            
            if (!sharedFiles.isEmpty()) {
                sharedFiles.each { it.hit(e.persona, e.timestamp, String.join(" ", e.searchTerms)) }
                re = new ResultsEvent(results: sharedFiles.asList(), uuid: e.uuid, searchEvent: e)
            }

        }

        if (re != null)
            eventBus.publish(re)
    }
    
    void onDirectoryUnsharedEvent(DirectoryUnsharedEvent e) {
        negativeTree.remove(e.directory)
        saveNegativeTree()
        if (!e.deleted) {
            List<SharedFile> unsharedFiles = new ArrayList<>()
            e.directory.listFiles().each {
                if (it.isDirectory())
                    eventBus.publish(new DirectoryUnsharedEvent(directory : it))
                else {
                    SharedFile sf = fileToSharedFile.get(it)
                    if (sf != null)
                        unsharedFiles.add(sf)
                }
            }
            eventBus.publish(new FileUnsharedEvent(unsharedFiles : unsharedFiles.toArray(new SharedFile[0])))
        } else {
             def cb = new DirDeletionCallback()
             positiveTree.traverse(e.directory, cb)
             positiveTree.remove(e.directory)
             eventBus.publish(new FileUnsharedEvent()unsharedFiles: cb.unsharedFiles.toArray(new SharedFile[0]), 
                deleted: true)
             cb.subDirs.each {
                 eventBus.publish(new DirectoryUnsharedEvent(directory : it, deleted : true))
             }
        }
    }
    
    private void saveNegativeTree() {
        settings.negativeFileTree.clear()
        settings.negativeFileTree.addAll(negativeTree.fileToNode.keySet().collect { it.getAbsolutePath() })
    }
    
    public List<SharedFile> getPublishedSince(long timestamp) {
        synchronized(fileToSharedFile) {
            fileToSharedFile.values().stream().
                    filter({sf -> sf.isPublished()}).
                    filter({sf -> sf.getPublishedTimestamp() >= timestamp}).
                    collect(Collectors.toList())
        }
    }
    
    private static class DirDeletionCallback implements FileTreeCallback<SharedFile> {
        
        final List<File> subDirs = new ArrayList<>()
        final List<SharedFile> unsharedFiles = new ArrayList<>()

        @Override
        public void onDirectoryEnter(File file) {
            subDirs.add(file)
        }

        @Override
        public void onDirectoryLeave() {
        }

        @Override
        public void onFile(File file, SharedFile value) {
            unsharedFiles << value
        }
        
    }
}
