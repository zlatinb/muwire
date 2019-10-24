package com.muwire.clilanterna

import com.muwire.core.Core
import com.muwire.core.Persona
import com.muwire.core.SplitPattern
import com.muwire.core.search.QueryEvent
import com.muwire.core.search.SearchEvent
import com.muwire.core.search.UIResultBatchEvent
import com.muwire.core.search.UIResultEvent


import com.googlecode.lanterna.gui2.table.TableModel
class SearchModel {
    private final String query
    private final Core core
    final TableModel model
    
    SearchModel(String query, Core core) {
        this.query = query
        this.core = core
        this.model = new TableModel("Sender","Results","Browse","Trust")
        core.eventBus.register(UIResultBatchEvent.class, this)
        
        
        def replaced = query.toLowerCase().trim().replaceAll(SplitPattern.SPLIT_PATTERN, " ")
        def terms = replaced.split(" ")
        def nonEmpty = []
        terms.each { if (it.length() > 0) nonEmpty << it }
        def searchEvent = new SearchEvent(searchTerms : nonEmpty, uuid : UUID.randomUUID(), oobInfohash: true,
            searchComments : core.muOptions.searchComments, compressedResults : true)
        boolean firstHop = core.muOptions.allowUntrusted || core.muOptions.searchExtraHop
        core.eventBus.publish(new QueryEvent(searchEvent : searchEvent, firstHop : firstHop,
            replyTo: core.me.destination, receivedOn: core.me.destination,
            originator : core.me))
    }
    
    void unregister() {
        core.eventBus.unregister(UIResultBatchEvent.class, this)
    }
    
    void onUIResultBatchEvent(UIResultBatchEvent e) {
        Persona sender = e.results[0].sender
        String browse = String.valueOf(e.results[0].browse)
        String results = String.valueOf(e.results.length)
        String trust = core.trustService.getLevel(sender.destination).toString()
        model.addRow([sender.getHumanReadableName(), results, browse, trust])
    }
}
