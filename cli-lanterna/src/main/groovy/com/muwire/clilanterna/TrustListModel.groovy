package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.table.TableModel
import com.muwire.core.Core
import com.muwire.core.trust.RemoteTrustList
import com.muwire.core.trust.TrustEvent

class TrustListModel {
    private final TextGUIThread guiThread
    private final RemoteTrustList trustList
    private final Core core
    private final TableModel trustedTableModel, distrustedTableModel
    
    TrustListModel(RemoteTrustList trustList, Core core) {
        this.trustList = trustList
        this.core = core
        
        trustedTableModel = new TableModel("Trusted User","Your Trust")
        distrustedTableModel = new TableModel("Distrusted User", "Your Trust")
        refreshModels()
        
        core.eventBus.register(TrustEvent.class, this)
    }
    
    void onTrustEvent(TrustEvent e) {
        guiThread.invokeLater {
            refreshModels()
        }
    }
    
    private void refreshModels() {
        int trustRows = trustedTableModel.getRowCount()
        trustRows.times { trustedTableModel.removeRow(0) }
        int distrustRows = distrustedTableModel.getRowCount()
        distrustRows.times { distrustedTableModel.removeRow(0) }
        
        trustList.good.each { 
            trustedTableModel.addRow(new PersonaWrapper(it), core.trustService.getLevel(it.destination))
        }
        trustList.bad.each { 
            distrustedTableModel.addRow(new PersonaWrapper(it), core.trustService.getLevel(it.destination))
        }
    }
    
    void unregister() {
        core.eventBus.unregister(TrustEvent.class, this)
    }
}
