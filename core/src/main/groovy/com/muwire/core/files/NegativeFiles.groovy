package com.muwire.core.files

import com.muwire.core.MuWireSettings
import com.muwire.core.SharedFile
import com.muwire.core.files.directories.WatchedDirectoryManager
import com.muwire.core.util.DataUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Log
class NegativeFiles {
    final FileTree<Boolean> negativeTree = new FileTree<>()
    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor()
    private final File negativeTreeFile
    private final WatchedDirectoryManager watchedDirectoryManager
    
    NegativeFiles(File home, WatchedDirectoryManager watchedDirectoryManager, MuWireSettings settings) {
        this.negativeTreeFile = new File(home, "negativeTree.json")
        this.watchedDirectoryManager = watchedDirectoryManager
        if (!settings.negativeFileTree.isEmpty())
            convertOldNegativeTree(settings)
        loadNegativeTree()
    }
    
    void shutdown() {
        executorService.shutdownNow()
    }

    private void convertOldNegativeTree(MuWireSettings settings) {
        log.info("converting old negative tree")
        for (String negative : settings.negativeFileTree) {
            File file = new File(negative)
            if (!file.isFile())
                continue
            negativeTree.add(file, true)
        }
        settings.negativeFileTree.clear()
        saveNegativeTree()
    }

    private void loadNegativeTree() {
        if (!negativeTreeFile.exists())
            return
        JsonSlurper slurper = new JsonSlurper()
        negativeTreeFile.eachLine{line ->
            def json = slurper.parseText(line)
            if (json.name == null)
                return
            String name = DataUtil.readi18nString(Base64.decode(json.name))
            File file = new File(name)
            if (file.exists())
                negativeTree.add(file, true)
        }
    }

    private void saveNegativeTree() {
        List<File> negativeFiles = new ArrayList<>()
        negativeTree.fileToNode.keySet().each {
            log.fine("considering file $it")
            if (negativeTree.get(it))
                negativeFiles.add(it)
        }

        log.info("saving negative tree size ${negativeTree.fileToNode.size()}, files $negativeFiles")

        if (negativeFiles.isEmpty())
            return
        
        executorService.submit({
            negativeTreeFile.withPrintWriter { printer ->
                negativeFiles.each {
                    def json = [:]
                    json.name = Base64.encode(DataUtil.encodei18nString(it.getAbsolutePath()))
                    json = JsonOutput.toJson(json)
                    printer.println(json)
                }
            }
        } as Runnable)
    }
    
    void onDirectoryUnsharedEvent(DirectoryUnsharedEvent event) {
        boolean save = negativeTree.remove(event.directory)
        if (!event.deleted && watchedDirectoryManager.isWatched(event.directory.getParentFile())) {
            log.fine("adding to negative tree directory ${event.directory}")
            negativeTree.add(event.directory, true)
            save |= true
        }
        if (save)
            saveNegativeTree()
    }
    
    void onFileUnsharedEvent(FileUnsharedEvent event) {
        if (event.deleted)
            return
        boolean save = false
        for (SharedFile sharedFile : event.unsharedFiles) {
            if (watchedDirectoryManager.isWatched(sharedFile.file.getParentFile())) {
                log.fine("adding to negative tree file ${sharedFile.file}")
                negativeTree.add(sharedFile.file, true)
                save = true
            }
        }
        if (save)
            saveNegativeTree()
    }
}
