package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.SharedFile

class FileHashedEvent extends Event {

    SharedFile sharedFile
    String error

    /**
     * This will be non-null in case of a re-hash.
     * Enriched by FileManager
     */
    SharedFile duplicate

    @Override
    public String toString() {
        super.toString() + " sharedFile " + sharedFile?.file?.getAbsolutePath() + " error: $error"
    }

}
