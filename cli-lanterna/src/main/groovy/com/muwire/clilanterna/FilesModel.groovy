package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.table.TableModel
import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile
import com.muwire.core.files.AllFilesLoadedEvent
import com.muwire.core.files.DirectoryWatchedEvent
import com.muwire.core.files.FileHashedEvent
import com.muwire.core.files.FileLoadedEvent
import com.muwire.core.files.FileSharedEvent
import com.muwire.core.files.FileUnsharedEvent
import com.muwire.core.trust.TrustSubscriptionEvent

import net.i2p.data.DataHelper

class FilesModel {
    private final TextGUIThread guiThread
    private final Core core
    private final List<SharedFile> sharedFiles = new ArrayList<>()
    private final TableModel model = new TableModel("Name","Size","Comment","Certified","Search Hits","Downloaders")
    
    FilesModel(TextGUIThread guiThread, Core core) {
        this.guiThread = guiThread
        this.core = core
        
        core.eventBus.register(FileLoadedEvent.class, this)
        core.eventBus.register(FileUnsharedEvent.class, this)
        core.eventBus.register(FileHashedEvent.class, this)
        core.eventBus.register(AllFilesLoadedEvent.class, this)
        
        Runnable refreshModel = {refreshModel()}
        Timer timer = new Timer(true)
        timer.schedule({
            guiThread.invokeLater(refreshModel)
        } as TimerTask, 1000,1000)
        
    }
    
    void onAllFilesLoadedEvent(AllFilesLoadedEvent e) {
        def eventBus = core.eventBus
        guiThread.invokeLater {
            core.muOptions.watchedDirectories.each {
                eventBus.publish(new FileSharedEvent(file: new File(it)))
            }
        }
    }
    
    void onFileLoadedEvent(FileLoadedEvent e) {
        guiThread.invokeLater {
            sharedFiles.add(e.loadedFile)
        }
    }
    
    void onFileHashedEvent(FileHashedEvent e) {
        guiThread.invokeLater {
            if (e.sharedFile != null)
                sharedFiles.add(e.sharedFile)
        }
    }
    
    void onFileUnsharedEvent(FileUnsharedEvent e) {
        guiThread.invokeLater {
            sharedFiles.remove(e.unsharedFile)
        }
    }
    
    private void refreshModel() {
        int rowCount = model.getRowCount()
        rowCount.times { model.removeRow(0) }
        
        sharedFiles.each { 
            long size = it.getCachedLength()
            boolean comment = it.comment != null
            boolean certified = core.certificateManager.hasLocalCertificate(new InfoHash(it.getRoot()))
            String hits = String.valueOf(it.getHits())
            String downloaders = String.valueOf(it.getDownloaders().size())
            model.addRow(new SharedFileWrapper(it), DataHelper.formatSize2(size, false)+"B", comment, certified, hits, downloaders)
        }
    }
    
    private void sort(SortType type) {
        Comparator<SharedFile> chosen
        switch(type) {
            case SortType.NAME_ASC : chosen = NAME_ASC; break
            case SortType.NAME_DESC : chosen = NAME_DESC; break
            case SortType.SIZE_ASC : chosen = SIZE_ASC; break
            case SortType.SIZE_DESC : chosen = SIZE_DESC; break
        }
        
        Collections.sort(sharedFiles, chosen)
    }
    
    private static final Comparator<SharedFile> NAME_ASC = new Comparator<SharedFile>() {
        public int compare(SharedFile a, SharedFile b) {
            a.getFile().getName().compareTo(b.getFile().getName())
        }
    }
    
    private static final Comparator<SharedFile> NAME_DESC = NAME_ASC.reversed()
    
    private static final Comparator<SharedFile> SIZE_ASC = new Comparator<SharedFile>() {
        public int compare(SharedFile a, SharedFile b) {
            Long.compare(a.getCachedLength(), b.getCachedLength())
        }
    }
    
    private static final Comparator<SharedFile> SIZE_DESC = SIZE_ASC.reversed()
}
