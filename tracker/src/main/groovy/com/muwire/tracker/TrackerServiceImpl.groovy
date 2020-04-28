package com.muwire.tracker

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import com.muwire.core.Core
import com.muwire.core.InfoHash
import com.muwire.core.UILoadedEvent

import net.i2p.data.Base64

@Component
class TrackerServiceImpl implements TrackerService {
    
    private final TrackerStatus status = new TrackerStatus()
    private final Core core
    
    @Autowired
    private SwarmManager swarmManager
    
    TrackerServiceImpl(Core core) {
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
    
    @Override
    public void track(String infoHash) {
        InfoHash ih = new InfoHash(Base64.decode(infoHash))
        swarmManager.track(ih)
    }
}
