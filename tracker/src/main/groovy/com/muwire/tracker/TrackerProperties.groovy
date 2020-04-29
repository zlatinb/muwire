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
    
    final SwarmParameters swarmParameters = new SwarmParameters()
    
    public static class SwarmParameters {
        /** how often to kick of queries on the MW net, in hours */
        int queryInterval = 1
        /** how many hosts to ping in parallel */
        int pingParallel = 5
        /** interval of time between pinging the same host, in minutes */
        int pingInterval = 15
        /** how long to wait before declaring a host is dead, in minutes */
        int expiry = 60
    }
}
