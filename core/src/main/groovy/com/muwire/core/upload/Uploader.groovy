package com.muwire.core.upload

import com.muwire.core.connection.Endpoint

class Uploader {
    private final Request request
    private final Endpoint endpoint
    
    Uploader(Request request, Endpoint endpoint) {
        this.request = request
        this.endpoint = endpoint
    }
    
    void respond() {
        
    }
}
