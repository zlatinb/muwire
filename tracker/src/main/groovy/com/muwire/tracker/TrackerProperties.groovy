package com.muwire.tracker

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("tracker")
class TrackerProperties {
    
    final JsonRpc jsonRpc = new JsonRpc()
    
    public static class JsonRpc {
        InetAddress iface
        int port
    }
}
