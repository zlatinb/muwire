package com.muwire.core.search

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.connection.I2PConnector
import com.muwire.core.profile.MWProfileHeader
import com.muwire.core.util.DataUtil

import groovy.json.JsonSlurper
import groovy.util.logging.Log
import net.i2p.data.Base64

import java.nio.charset.StandardCharsets
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.logging.Level
import java.util.zip.GZIPInputStream

@Log
class BrowseManager {
    
    private final I2PConnector connector
    private final EventBus eventBus
    private final Persona me
    
    private final Executor browserThread = Executors.newCachedThreadPool()
    
    BrowseManager(I2PConnector connector, EventBus eventBus, Persona me) {
        this.connector = connector
        this.eventBus = eventBus
        this.me = me
    }
    
    void onUIBrowseEvent(UIBrowseEvent e) {
        browserThread.execute(new BrowseSession(eventBus, connector, e, me))
    }
}
