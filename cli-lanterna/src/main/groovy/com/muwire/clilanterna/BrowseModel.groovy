package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.table.TableModel
import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.search.BrowseStatus
import com.muwire.core.search.BrowseStatusEvent
import com.muwire.core.search.UIBrowseEvent
import com.muwire.core.search.UIResultEvent

import net.i2p.data.Base64
import net.i2p.data.DataHelper

class BrowseModel {
    private final Persona persona
    private final Core core
    private final TextGUIThread guiThread
    private final TableModel model = new TableModel("Name","Size","Hash","Comment","Certificates")
    private Map<String, UIResultEvent> rootToResult = new HashMap<>()
    
    private int totalResults
    
    private Label status
    private Label percentage
    
    BrowseModel(Persona persona, Core core, TextGUIThread guiThread) {
        this.persona = persona
        this.core = core
        this.guiThread = guiThread
        
        core.eventBus.register(BrowseStatusEvent.class, this)
        core.eventBus.register(UIResultEvent.class, this)
        core.eventBus.publish(new UIBrowseEvent(host : persona))
    }
    
    void unregister() {
        core.eventBus.unregister(BrowseStatusEvent.class, this)
        core.eventBus.unregister(UIResultEvent.class, this)
    }
    
    void onBrowseStatusEvent(BrowseStatusEvent e) {
        guiThread.invokeLater {
            status.setText(e.status.toString())
            if (e.status == BrowseStatus.FETCHING)
                totalResults = e.totalResults
        }
    }
    
    void onUIResultEvent(UIResultEvent e) {
        guiThread.invokeLater {
            String size = DataHelper.formatSize2Decimal(e.size, false) + "B"
            String infoHash = Base64.encode(e.infohash.getRoot())
            String comment = String.valueOf(e.comment != null)
            model.addRow(e.name, size, infoHash, comment, e.certificates)
            rootToResult.put(infoHash, e)
            
            String percentageString = ""
            if (totalResults != 0) {
                double percentage = Math.round( (model.getRowCount() * 100 / totalResults).toDouble() )
                percentageString = String.valueOf(percentage)+"%"
            }
            percentage.setText(percentageString)
        }
    }
    
    void setStatusLabel(Label status) {
        this.status = status
    }
    
    void setPercentageLabel(Label percentage) {
        this.percentage = percentage
    }
}
