package com.muwire.tracker

import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcMethod
import com.github.arteam.simplejsonrpc.core.annotation.JsonRpcService
import com.muwire.core.Core
import com.muwire.core.UILoadedEvent

@JsonRpcService
class TrackerService {
    
    private final TrackerStatus status = new TrackerStatus()
    private final Core core
    
    TrackerService(Core core) {
        this.core = core
        status.status = "Starting"
    }
    
    @JsonRpcMethod
    public TrackerStatus status() {
        status.connections = core.getConnectionManager().getConnections().size()
        status
    }
    
    
    void onUILoadedEvent(UILoadedEvent e) {
        status.status = "Running"
    }
}
