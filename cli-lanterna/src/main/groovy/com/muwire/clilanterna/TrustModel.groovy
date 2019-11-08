package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.table.TableModel
import com.muwire.core.Core
import com.muwire.core.files.AllFilesLoadedEvent
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustSubscriptionEvent
import com.muwire.core.trust.TrustSubscriptionUpdatedEvent

class TrustModel {
    private final TextGUIThread guiThread
    private final Core core
    private final TableModel modelTrusted, modelDistrusted, modelSubscriptions
    
    TrustModel(TextGUIThread guiThread, Core core) {
        this.guiThread = guiThread
        this.core = core
        
        modelTrusted = new TableModel("Trusted Users","Reason")
        modelDistrusted = new TableModel("Distrusted Users","Reason")
        modelSubscriptions = new TableModel("Name","Trusted","Distrusted","Status","Last Updated")
        
        core.eventBus.register(TrustEvent.class, this)
        core.eventBus.register(AllFilesLoadedEvent.class, this)
        core.eventBus.register(TrustSubscriptionUpdatedEvent.class, this)
        
    }
    
    void onTrustEvent(TrustEvent e) {
        guiThread.invokeLater {
            refreshModels()
        }
    }
    
    void onTrustSubscriptionUpdatedEvent(TrustSubscriptionUpdatedEvent e) {
        guiThread.invokeLater {
            refreshModels()
        }
    }
    
    void onAllFilesLoadedEvent(AllFilesLoadedEvent e) {
        guiThread.invokeLater {
            refreshModels()
        }
        core.muOptions.trustSubscriptions.each {
            core.eventBus.publish(new TrustSubscriptionEvent(persona : it, subscribe : true))
        }
    }
    
    private void refreshModels() {
        int trustedRows = modelTrusted.getRowCount()
        trustedRows.times { modelTrusted.removeRow(0) }
        int distrustedRows = modelDistrusted.getRowCount()
        distrustedRows.times { modelDistrusted.removeRow(0) }
        int subsRows = modelSubscriptions.getRowCount()
        subsRows.times { modelSubscriptions.removeRow(0) }
        
        core.trustService.good.values().each { 
            modelTrusted.addRow(new PersonaWrapper(it.persona),it.reason)
        }
        
        core.trustService.bad.values().each { 
            modelDistrusted.addRow(new PersonaWrapper(it.persona),it.reason)
        }
        
        core.trustSubscriber.remoteTrustLists.values().each { 
            def name = new TrustListWrapper(it)
            String trusted = String.valueOf(it.good.size())
            String distrusted = String.valueOf(it.bad.size())
            String status = it.status
            String lastUpdated = it.timestamp == 0 ? "Never" : new Date(it.timestamp)
            
            modelSubscriptions.addRow(name, trusted, distrusted, status, lastUpdated)
        }
        
    }
}
