package com.muwire.core.search

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

class SearchEvent extends Event {

    List<String> searchTerms
    byte [] searchHash
    UUID uuid
    boolean oobInfohash
    boolean searchComments
    boolean compressedResults
    Persona persona

    String toString() {
        def infoHash = null
        if (searchHash != null)
            infoHash = new InfoHash(searchHash)
        "searchTerms: $searchTerms searchHash:$infoHash, uuid:$uuid oobInfohash:$oobInfohash searchComments:$searchComments compressedResults:$compressedResults"
    }
}
