package com.muwire.core.chat

import com.muwire.core.profile.MWProfile

import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.connection.I2PConnector
import com.muwire.core.trust.TrustService

import java.util.function.Supplier

class ChatManager {
    private final EventBus eventBus
    private final Persona me
    private final I2PConnector connector
    private final TrustService trustService
    private final MuWireSettings settings
    private final Supplier<MWProfile> profileSupplier
    
    private final Map<Persona, ChatClient> clients = new ConcurrentHashMap<>()
    
    ChatManager(EventBus eventBus, Persona me, Supplier<MWProfile> profileSupplier,
                I2PConnector connector, TrustService trustService, MuWireSettings settings) {
        this.eventBus = eventBus
        this.me = me
        this.profileSupplier = profileSupplier
        this.connector = connector
        this.trustService = trustService
        this.settings = settings
        
        Timer timer = new Timer("chat-connector", true)
        timer.schedule({connect()} as TimerTask, 1000, 1000)
    }
    
    boolean isConnected(Persona p) {
        clients.containsKey(p)
    }
    
    void onUIConnectChatEvent(UIConnectChatEvent e) {
        if (e.host == me) { 
            String defaultChatRoom = null
            if (settings.joinDefaultChatRoom && settings.defaultChatRoom.size() > 0)
                defaultChatRoom = settings.defaultChatRoom
            eventBus.publish(new ChatConnectionEvent(status : ChatConnectionAttemptStatus.SUCCESSFUL, 
                persona : me, connection : LocalChatLink.INSTANCE, defaultRoom: defaultChatRoom))
        } else {
            ChatClient client = new ChatClient(connector, eventBus, e.host, me, profileSupplier, 
                    trustService, settings)
            clients.put(e.host, client)
        }
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
        clients[e.host]?.sendChat(e)
    }
    
    void onChatDisconnectionEvent(ChatDisconnectionEvent e) {
        clients[e.persona]?.disconnected()
    }
    
    private void connect() {
        clients.each { k, v -> 
            v.connectIfNeeded()
            v.ping() 
        }
    }
    
    void shutdown() {
        clients.each { k, v ->
            v.close() 
        }
    }
}
