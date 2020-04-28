package com.muwire.tracker

import com.muwire.core.Core
import com.muwire.core.UILoadedEvent

class TrackerService {
    
    private final TrackerStatus status = new TrackerStatus()
    private final Core core
    
    TrackerService(Core core) {
        this.core = core
        status.status = "Starting"
    }
    
    public TrackerStatus status() {
        status.connections = core.getConnectionManager().getConnections().size()
        status
    }
    
    
    void onUILoadedEvent(UILoadedEvent e) {
        status.status = "Running"
    }
}
