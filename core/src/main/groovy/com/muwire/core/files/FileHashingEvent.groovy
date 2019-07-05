package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.SharedFile

class FileHashingEvent extends Event {

    File hashingFile

    @Override
    public String toString() {
        super.toString() + " hashingFile " + hashingFile.getAbsolutePath()
    }

}
