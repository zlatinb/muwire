package com.muwire.clilanterna

import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.search.UIResultEvent

import net.i2p.data.Base64
import net.i2p.data.DataHelper

import com.googlecode.lanterna.gui2.table.TableModel

class ResultsModel {
    private final UIResultBatchEvent results
    final TableModel model
    final Map<String, UIResultEvent> rootToResult = new HashMap<>()
    
    ResultsModel(UIResultBatchEvent results) {
        this.results = results
        model = new TableModel("Name","Size","Hash","Sources","Comment")
        results.results.each { 
            String size = DataHelper.formatSize2Decimal(it.size, false) + "B"
            String infoHash = Base64.encode(it.infohash.getRoot())
            String sources = String.valueOf(it.sources.size())
            String comment = String.valueOf(it.comment != null)
            model.addRow(it.name, size, infoHash, sources, comment)
            rootToResult.put(infoHash, it)
        }
    }
}
