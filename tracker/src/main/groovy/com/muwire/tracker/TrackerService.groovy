package com.muwire.tracker

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService

@JsonRpcService
class TrackerService {
    
    private final TrackerStatus status = new TrackerStatus()
    
    TrackerService() {
        status.status = "Starting"
    }
    
    @JsonRpcMethod
    public TrackerStatus status() {
        status
    }
}
