package com.muwire.webui;

import com.muwire.core.connection.ConnectionAttemptStatus;
import com.muwire.core.connection.ConnectionEvent;
import com.muwire.core.connection.DisconnectionEvent;

public class ConnectionCounter {
    
    private volatile int connections;
    
    public int getConnections() {
        return connections;
    }
    
    public void onConnectionEvent(ConnectionEvent e) {
        if (e.getStatus() == ConnectionAttemptStatus.SUCCESSFUL)
            connections++;
    }
    
    public void onDisconnectionEvent(DisconnectionEvent e) {
        connections--;
    }

}
