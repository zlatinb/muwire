package com.muwire.core.search

import com.muwire.core.Event
import com.muwire.core.InfoHash

class SearchEvent extends Event {

	List<String> searchTerms
	byte [] searchHash
	UUID uuid
    boolean oobInfohash
    
    String toString() {
        def infoHash = null
        if (searchHash != null)
            infoHash = new InfoHash(searchHash)
        "searchTerms: $searchTerms searchHash:$infoHash, uuid:$uuid oobInfohash:$oobInfohash"
    }
}
