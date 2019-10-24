package com.muwire.clilanterna

import com.muwire.core.SharedFile

class SharedFileWrapper {
    private final SharedFile sharedFile
    
    SharedFileWrapper(SharedFile sharedFile) {
        this.sharedFile = sharedFile
    }
    
    @Override
    public String toString() {
        sharedFile.getCachedPath()
    }
}
