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
        model = new TableModel("Name","Size","Hash","Sources","Comment","Certificates")
        updateModel()
    }
    
    void sort(SortType type) {
        Comparator<UIResultEvent> chosen
        switch(type) {
            case SortType.NAME_ASC : chosen = ResultComparators.NAME_ASC; break
            case SortType.NAME_DESC : chosen = ResultComparators.NAME_DESC; break
            case SortType.SIZE_ASC : chosen = ResultComparators.SIZE_ASC; break
            case SortType.SIZE_DESC : chosen = ResultComparators.SIZE_DESC; break
        }

        Arrays.sort(results.results, chosen)
        updateModel()
    }
    
    private void updateModel() {
        int rowCount = model.getRowCount()
        rowCount.times { model.removeRow(0) }
        
        results.results.each {
            String size = DataHelper.formatSize2Decimal(it.size, false) + "B"
            String infoHash = Base64.encode(it.infohash.getRoot())
            String sources = String.valueOf(it.sources.size())
            String comment = String.valueOf(it.comment != null)
            model.addRow(it.name, size, infoHash, sources, comment, it.certificates)
            rootToResult.put(infoHash, it)
        }
    }
}
