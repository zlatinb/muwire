package com.muwire.clilanterna

import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.TextGUIThread
import com.googlecode.lanterna.gui2.table.TableModel
import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.search.BrowseStatus
import com.muwire.core.search.BrowseStatusEvent
import com.muwire.core.search.UIBrowseEvent
import com.muwire.core.search.UIResultBatchEvent
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
    private UUID uuid
    
    private Label status
    private Label percentage
    
    BrowseModel(Persona persona, Core core, TextGUIThread guiThread) {
        this.persona = persona
        this.core = core
        this.guiThread = guiThread
        
        core.eventBus.register(BrowseStatusEvent.class, this)
        core.eventBus.register(UIResultBatchEvent.class, this)
        core.eventBus.publish(new UIBrowseEvent(host : persona))
    }
    
    void unregister() {
        core.eventBus.unregister(BrowseStatusEvent.class, this)
        core.eventBus.unregister(UIResultBatchEvent.class, this)
    }
    
    void onBrowseStatusEvent(BrowseStatusEvent e) {
        guiThread.invokeLater {
            status.setText(e.status.toString())
            if (e.status == BrowseStatus.FETCHING) {
                totalResults = e.totalResults
                uuid = e.uuid
            }
        }
    }
    
    void onUIResultBatchEvent(UIResultBatchEvent eBatch) {
        if (uuid != eBatch.uuid)
            return
        guiThread.invokeLater {
            for (UIResultEvent e : eBatch.results) {
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
    }
    
    void setStatusLabel(Label status) {
        this.status = status
    }
    
    void setPercentageLabel(Label percentage) {
        this.percentage = percentage
    }
    
    void sort(SortType type) {
        Comparator<UIResultEvent> chosen
        switch(type) {
            case SortType.NAME_ASC : chosen = ResultComparators.NAME_ASC; break
            case SortType.NAME_DESC : chosen = ResultComparators.NAME_DESC; break
            case SortType.SIZE_ASC : chosen = ResultComparators.SIZE_ASC; break
            case SortType.SIZE_DESC : chosen = ResultComparators.SIZE_DESC; break
        }
        
        List<UIResultEvent> l = new ArrayList<>(rootToResult.values())
        Collections.sort(l, chosen)
        
        int rowCount = model.getRowCount()
        rowCount.times { model.removeRow(0) }
        
        l.each { e ->
            String size = DataHelper.formatSize2Decimal(e.size, false) + "B"
            String infoHash = Base64.encode(e.infohash.getRoot())
            String comment = String.valueOf(e.comment != null)
            model.addRow(e.name, size, infoHash, comment, e.certificates)
        }
    }
}
