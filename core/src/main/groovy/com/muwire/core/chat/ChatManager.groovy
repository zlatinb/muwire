package com.muwire.core.chat

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.connection.I2PConnector
import com.muwire.core.trust.TrustService

class ChatManager {
    private final EventBus eventBus
    private final Persona me
    private final I2PConnector connector
    private final TrustService trustService
    private final MuWireSettings settings
    
    private final Map<Persona, ChatClient> clients = new ConcurrentHashMap<>()
    
    ChatManager(EventBus eventBus, Persona me, I2PConnector connector, TrustService trustService, 
        MuWireSettings settings) {
        this.eventBus = eventBus
        this.me = me
        this.connector = connector
        this.trustService = trustService
        this.settings = settings
        
        Timer timer = new Timer("chat-connector", true)
        timer.schedule({connect()} as TimerTask, 1000, 1000)
    }
    
    void onUIConnectChatEvent(UIConnectChatEvent e) {
        if (e.host == me)
            return
        ChatClient client = new ChatClient(connector, eventBus, e.host, me, trustService, settings)
        clients.put(e.host, client)
    }
    
    void onUIDisconnectChatEvent(UIDisconnectChatEvent e) {
        if (e.host == me)
            return
        ChatClient client = clients.remove(e.host)
        client?.close()
    }
    
    void onChatMessageEvent(ChatMessageEvent e) {
        if (e.host == me)
            return
        if (e.sender != me)
            return
        clients[e.host]?.connection?.sendChat(e)
    }
    
    private void connect() {
        clients.each { k, v -> v.connectIfNeeded() }
    }
    
    void shutdown() {
        clients.each { k, v ->
            v.close() 
        }
    }
}
