package com.muwire.tracker

import org.springframework.boot.web.server.ConfigurableWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.stereotype.Component

@Component
class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    
    private final TrackerProperties trackerProperties
    
    WebServerConfiguration(TrackerProperties trackerProperties) {
        this.trackerProperties = trackerProperties;
    }
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        factory.setAddress(trackerProperties.jsonRpc.getIface())
        factory.setPort(trackerProperties.jsonRpc.port)
    }
}
