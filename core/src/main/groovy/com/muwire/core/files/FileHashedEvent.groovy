package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.SharedFile

class FileHashedEvent extends Event {

    SharedFile sharedFile
    String error

    @Override
    public String toString() {
        super.toString() + " sharedFile " + sharedFile?.file.getAbsolutePath() + " error: $error"
    }

}
