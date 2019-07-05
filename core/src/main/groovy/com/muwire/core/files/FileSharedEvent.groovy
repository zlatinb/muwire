package com.muwire.core.files

import com.muwire.core.Event

class FileSharedEvent extends Event {

    File file

    @Override
    public String toString() {
        return super.toString() + " file: "+file.getAbsolutePath()
    }
}
