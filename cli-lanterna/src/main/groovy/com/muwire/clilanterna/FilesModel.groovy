package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.table.TableModel
import com.muwire.core.Core
import com.muwire.core.SharedFile
import com.muwire.core.files.AllFilesLoadedEvent
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
    private final TableModel model = new TableModel("Name","Size","Comment")
    
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
                eventBus.publish(new FileSharedEvent(file : new File(it)))
            }
            // TODO: move this to the trust model class
            core.muOptions.trustSubscriptions.each { 
                eventBus.publish(new TrustSubscriptionEvent(persona : it, subscribe : true))
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
            model.addRow(new SharedFileWrapper(it), DataHelper.formatSize2(size, false)+"B", comment)
        }
    }
}
