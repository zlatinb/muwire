package com.muwire.core.files

import com.muwire.core.Persona
import com.muwire.core.files.directories.WatchedDirectoryConfigurationEvent

import java.nio.file.Path
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.function.BiPredicate
import java.util.function.Predicate
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import java.util.stream.Collectors

import com.muwire.core.EventBus
import com.muwire.core.InfoHash
import com.muwire.core.MuWireSettings
import com.muwire.core.SharedFile
import com.muwire.core.search.ResultsEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.SearchIndex
import com.muwire.core.util.DataUtil

import groovy.util.logging.Log
import net.i2p.data.Base64

@Log
class FileManager {


    private final File home
    final EventBus eventBus
    final MuWireSettings settings
    final Map<InfoHash, SharedFile[]> rootToFiles = Collections.synchronizedMap(new HashMap<>())
    final Map<File, SharedFile> fileToSharedFile = Collections.synchronizedMap(new HashMap<>())
    final Map<String, File[]> nameToFiles = new HashMap<>()
    final Map<String, File[]> pathToFiles = new HashMap<>()
    final Map<String, Set<File>> commentToFile = new HashMap<>()
    final SearchIndex index, pathIndex
    final FileTree<SharedFile> positiveTree = new FileTree<>()
    final Set<File> sideCarFiles = new HashSet<>()
    private Predicate<File> isWatched
    private BiPredicate<File, Persona> isVisible
    private final Executor INDEX_IO = Executors.newSingleThreadExecutor()

    FileManager(File home, EventBus eventBus, MuWireSettings settings) {
        this.home = home
        this.settings = settings
        this.eventBus = eventBus
        File tmp = new File(home, "tmp")
        if (!tmp.exists())
            tmp.mkdirs()
        this.index = new SearchIndex(tmp,"fileManager") 
        this.pathIndex = new SearchIndex(tmp,"fileManagerPaths")
    }
    
    void setIsWatched(Predicate<File> isWatched) {
        this.isWatched = isWatched
    }
    
    void setIsVisible(BiPredicate<File, Persona> isVisible) {
        this.isVisible = isVisible
    }
    
    private void updateIndex(SearchIndex index, String string, boolean add) {
        log.fine("submitting update to index add $add string $string")
        INDEX_IO.submit ({
            log.fine("updating index add $add string $string")
            if (add)
                index.add(string)
            else
                index.remove(string)
        } as Runnable)
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
        if (e.original != null)
            e.sharedFile.setComment(e.original.getComment())
        addToIndex(e.sharedFile)
    }

    void onFileLoadedEvent(FileLoadedEvent e) {
        addToIndex(e.loadedFile)
    }

    void onFileDownloadedEvent(FileDownloadedEvent e) {
        if (settings.shareDownloadedFiles && !e.confidential) {
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
            updateIndex(index, comment, true)
            Set<File> existingComment = commentToFile.get(comment)
            if(existingComment == null) {
                existingComment = new HashSet<>()
                commentToFile.put(comment, existingComment)
            }
            existingComment.add(sf.getFile())
        }
        
        updateIndex(index, name, true)
        
        String path = getVisiblePath(sf.getFile())
        if (path == null)
            return
        
        log.fine("will index path $path")
        updateIndex(pathIndex, path, true)
        existingFiles = pathToFiles.get(path)
        if (existingFiles == null) {
            existingFiles = new File[] {sf.getFile()}
        } else {
            Set<File> unique = new HashSet<>()
            existingFiles.each {unique.add(it)}
            unique.add(sf.getFile())
            existingFiles = unique.toArray(existingFiles)
        }
        pathToFiles.put(path, existingFiles)
    }
    
    private String getVisiblePath(File file) {
        File parent = file.getParentFile()
        if (parent == null)
            return null
        
        File root = file
        while(true) {
            File parent2 = root.getParentFile()
            if (parent2 == null)
                break
            if (!isWatched.test(parent2))
                break
            root = parent2
        }
        
        if (root == file)
            return null
        
        Path path = root.toPath().relativize(file.toPath())
        Path visible = Path.of(root.getName(), path.toString())
        visible.toString()
    }

    void onFileUnsharedEvent(FileUnsharedEvent e) {
        for(SharedFile sharedFile : e.unsharedFiles)
            unshareFile(sharedFile, e.deleted)
    }
    
    void onFileModifiedEvent(FileModifiedEvent event) {
        for(SharedFile sf : event.sharedFiles)
            unshareFile(sf, false)
    }
    
    private void unshareFile(SharedFile sf, boolean deleted) {
        log.fine("unsharing ${sf.getFile()} deleted:$deleted")
        
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
                    updateIndex(index, comment, false)
                }
            }
        }

        updateIndex(index, name, false)
        
        String path = getVisiblePath(sf.file)
        if (path == null)
            return
        log.fine("un-indexing path $path")
        existingFiles = pathToFiles.get(path)
        if (existingFiles != null) {
            Set<File> unique = new HashSet<>()
            unique.addAll(existingFiles)
            unique.remove(sf.file)
            if (unique.isEmpty())
                pathToFiles.remove(path)
            else {
                existingFiles = unique.toArray(new File[0])
                pathToFiles.put(path, existingFiles)
            }
        }
    }
    
    void onUICommentEvent(UICommentEvent e) {
        if (e.oldComment != null) {
            def comment = DataUtil.readi18nString(Base64.decode(e.oldComment))
            Set<File> existingFiles = commentToFile.get(comment) 
            existingFiles.remove(e.sharedFile.getFile())
            if (existingFiles.isEmpty()) {
                commentToFile.remove(comment)
                updateIndex(index, comment, false)
            }
        }
        
        String comment = e.sharedFile.getComment()
        comment = DataUtil.readi18nString(Base64.decode(comment))
        if (comment != null) {
            updateIndex(index, comment, true)
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
            List<SharedFile> found
            found = rootToFiles.getOrDefault(new InfoHash(e.searchHash), new SharedFile[0]).toList()
            if (!found.isEmpty()) 
                found.retainAll { isVisible.test(it.file.getParentFile(), e.persona) }
            if (!found.isEmpty()) {
                found.each { it.hit(e.persona, e.timestamp, "Hash Search") }
                re = new ResultsEvent(results: found, uuid: e.uuid, searchEvent: e)
            }
        } else if (e.regex) {
            // check if valid regex
            try {
                Pattern pattern = Pattern.compile(e.searchTerms[0])
                Set<SharedFile> results
                synchronized (fileToSharedFile) {
                    results = fileToSharedFile.values().stream().filter{
                        Matcher matcher = pattern.matcher(it.getFile().getName())
                        if (matcher.matches())
                            return true
                        if (e.searchComments && it.getComment() != null) {
                            matcher = pattern.matcher(DataUtil.readi18nString(Base64.decode(it.getComment())))
                            if (matcher.matches())
                                return true
                        }
                        if (e.searchPaths && settings.showPaths) {
                            matcher = pattern.matcher(it.getCachedVisiblePath())
                            if (matcher.matches())
                                return true
                        }
                        return false
                    }.collect(Collectors.toSet())
                }
                if (!results.isEmpty())
                    results.retainAll{isVisible.test(it.file.getParentFile(), e.persona)}
                if (!results.isEmpty()) {
                    results.each { it.hit(e.persona, e.timestamp, "/${e.searchTerms[0]}/") }
                    re = new ResultsEvent(results: results.asList(), uuid: e.uuid, searchEvent: e)
                }
            } catch (PatternSyntaxException bad) {
                log.info("invalid regex received $e")
            }
        } else {
            def names = index.search e.searchTerms
            Set<File> files = new HashSet<>()
            names.each { 
                files.addAll nameToFiles.getOrDefault(it, [])
                if (e.searchComments)
                    files.addAll commentToFile.getOrDefault(it, [])
            }
        
            if (e.searchPaths && settings.showPaths) {
                def paths = pathIndex.search e.searchTerms
                paths.each {
                    files.addAll pathToFiles.getOrDefault(it, [])
                }
            }
            
            Set<SharedFile> sharedFiles = new HashSet<>()
            files.each { sharedFiles.add fileToSharedFile[it] }
            
            if (!sharedFiles.isEmpty())
                sharedFiles.retainAll { isVisible.test(it.file.getParentFile(), e.persona)}
            if (!sharedFiles.isEmpty()) {
                sharedFiles.each { it.hit(e.persona, e.timestamp, String.join(" ", e.searchTerms)) }
                re = new ResultsEvent(results: sharedFiles.asList(), uuid: e.uuid, searchEvent: e)
            }

        }

        if (re != null)
            eventBus.publish(re)
    }
    
    void onDirectoryUnsharedEvent(DirectoryUnsharedEvent e) {
        def cb = new DirDeletionCallback()
        for (File dir : e.directories) {
            log.fine("FM: traversing from $dir")
            positiveTree.traverse(dir, cb)
        }
        cb.subDirs.each {log.fine("FM: will remove dir $it")}
        if (!cb.subDirs.isEmpty())
            eventBus.publish(new DirectoryUnsharedEvent(directories: cb.subDirs.toArray(new File[0]), deleted: e.deleted))
        if (!cb.unsharedFiles.isEmpty()) {
            eventBus.publish(new FileUnsharedEvent(unsharedFiles: cb.unsharedFiles.toArray(new SharedFile[0]),
                    deleted: e.deleted, implicit: true))
        }
        for (File dir : e.directories)
            positiveTree.remove(dir)
    }
    
    void onWatchedDirectoryConfigurationEvent(WatchedDirectoryConfigurationEvent e) {
        final long start = System.currentTimeMillis() - e.timestamp
        // just enriches the event with subdirectories.
        if (!e.subfolders) {
            e.toApply = new File[] {e.directory}
        } else {
            def cb = new SubDirCallback()
            positiveTree.traverse(e.directory, cb)
            e.toApply = cb.subDirs.toArray(new File[0])
        }
        final long end = System.currentTimeMillis() - e.timestamp
        log.fine("WatchedDirectoryConfiguration enrichment \"${e.directory}\" ${start} ${end}")
    }
    
    public List<SharedFile> getPublishedSince(long timestamp) {
        synchronized(fileToSharedFile) {
            fileToSharedFile.values().stream().
                    filter({sf -> sf.isPublished()}).
                    filter({sf -> sf.getPublishedTimestamp() >= timestamp}).
                    collect(Collectors.toList())
        }
    }
    
    public void close() {
        INDEX_IO.shutdownNow()
        pathIndex.close()
        index.close()
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
    
    private static class SubDirCallback implements FileTreeCallback<SharedFile> {
        final List<File> subDirs = new ArrayList<>()
        
        @Override
        public void onDirectoryEnter(File file) {
            subDirs << file
        }
        
        @Override
        public void onDirectoryLeave(){}
        @Override
        public void onFile(File file, SharedFile ignored){}
    }
}
