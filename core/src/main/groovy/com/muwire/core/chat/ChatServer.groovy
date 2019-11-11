package com.muwire.core.chat

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.trust.TrustEvent
import com.muwire.core.trust.TrustLevel
import com.muwire.core.trust.TrustService
import com.muwire.core.util.DataUtil

import groovy.util.logging.Log
import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

@Log
class ChatServer {
    public static final String CONSOLE = "__CONSOLE__"
    private final EventBus eventBus
    private final MuWireSettings settings
    private final TrustService trustService
    private final Persona me
    
    private final Map<Destination, ChatConnection> connections = new ConcurrentHashMap()
    private final Map<String, Set<Persona>> rooms = new ConcurrentHashMap<>()
    private final Map<Persona, Set<String>> memberships = new ConcurrentHashMap<>()
    
    private final AtomicBoolean running = new AtomicBoolean()
    
    ChatServer(EventBus eventBus, MuWireSettings settings, TrustService trustService, Persona me) {
        this.eventBus = eventBus
        this.settings = settings
        this.trustService = trustService
        this.me = me
        
        Timer timer = new Timer("chat-server-pinger", true)
        timer.schedule({sendPings()} as TimerTask, 1000, 1000)
    }
    
    public void start() {
        running.set(true)
    }
    
    private void sendPings() {
        connections.each { k,v -> 
            v.sendPing()
        }
    }
    
    public void handle(Endpoint endpoint) {
        InputStream is = endpoint.getInputStream()
        OutputStream os = endpoint.getOutputStream()
        
        Map<String, String> headers = DataUtil.readAllHeaders(is)
        
        if (!headers.containsKey("Version"))
            throw new Exception("Version header missing")
        
        int version = Integer.parseInt(headers['Version'])
        if (version != Constants.CHAT_VERSION)
            throw new Exception("Unknown chat version $version")
        
        if (!headers.containsKey('Persona'))
            throw new Exception("Persona header missing")
        
        Persona client = new Persona(new ByteArrayInputStream(Base64.decode(headers['Persona'])))
        if (client.destination != endpoint.destination)
            throw new Exception("Client destination mismatch")
        
        if (!running.get()) {
            os.write("400 Chat Not Enabled\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
            os.close()
            endpoint.close()
            return
        }
        
        if (connections.containsKey(client.destination) || connections.size() == settings.maxChatConnections) {
            os.write("429 Rejected\r\n\r\n".getBytes(StandardCharsets.US_ASCII))
            os.close()
            endpoint.close()
            return
        }
        
        os.with { 
            write("200 OK\r\n".getBytes(StandardCharsets.US_ASCII))
            write("Version:${Constants.CHAT_VERSION}\r\n".getBytes(StandardCharsets.US_ASCII))
            write("\r\n".getBytes(StandardCharsets.US_ASCII))
            flush()
        }
        
        ChatConnection connection = new ChatConnection(eventBus, endpoint, client, true, trustService, settings)
        connections.put(endpoint.destination, connection)
        joinRoom(client, CONSOLE)
        connection.start()
        eventBus.publish(new ChatConnectionEvent(connection : connection, status : ChatConnectionAttemptStatus.SUCCESSFUL, persona : client))
    }
    
    void onChatDisconnectionEvent(ChatDisconnectionEvent e) {
        ChatConnection con = connections.remove(e.persona.destination)
        if (con == null)
            return
            
        Set<String> rooms = memberships.get(e.persona)
        if (rooms != null) {
            rooms.each { 
                leaveRoom(e.persona, it)
            }
        }
        connections.each { k, v ->
            v.sendLeave(e.persona)
        }
    }
    
    void onTrustEvent(TrustEvent e) {
        if (e.level == TrustLevel.TRUSTED)
            return
        if (settings.allowUntrusted && e.level == TrustLevel.NEUTRAL)
            return
        
        ChatConnection connection = connections.remove(e.persona.destination)
        connection?.close()
    }
    
    private void joinRoom(Persona p, String room) {
        Set<Persona> existing = rooms.get(room)
        if (existing == null) {
            existing = new ConcurrentHashSet<>()
            rooms.put(room, existing)
        }
        existing.add(p)
        
        Set<String> membership = memberships.get(p)
        if (membership == null) {
            membership = new ConcurrentHashSet<>()
            memberships.put(p, membership)
        }
        membership.add(room)
    }
    
    private void leaveRoom(Persona p, String room) {
        Set<Persona> existing = rooms.get(room)
        if (existing == null) {
            log.warning(p.getHumanReadableName() + " leaving room they hadn't joined")
            return
        }
        existing.remove(p)
        if (existing.isEmpty())
            rooms.remove(room)
            
        Set<String> membership = memberships.get(p)
        if (membership == null) {
            log.warning(p.getHumanReadableName() + " didn't have any memberships")
            return
        }
        membership.remove(room)
        if (membership.isEmpty())
            memberships.remove(p)
    }
    
    void onChatMessageEvent(ChatMessageEvent e) {
        if (e.host != me)
            return
        
        ChatCommand command
        try {
            command = new ChatCommand(e.payload)
        } catch (Exception badCommand) {
            log.log(Level.WARNING, "bad chat command",badCommand)
            return
        }
        
        switch(command.action) {
            case ChatAction.JOIN : processJoin(command.payload, e); break
            case ChatAction.LEAVE : processLeave(e); break
            case ChatAction.SAY : processSay(e); break
        }
    }
    
    private void processJoin(String room, ChatMessageEvent e) {
        joinRoom(room, e.sender)
        rooms[room].each { 
            if (it == e.sender)
                return
            connections[it.destination].sendChat(e)
        }
    }
    
    private void processLeave(ChatMessageEvent e) {
        leaveRoom(e.room)
        rooms.getOrDefault(e.room, []).each { 
            if (it == e.sender)
                return
            connections[it.destination].sendChat(e)
        }
    }
    
    private void processSay(ChatMessageEvent e) {
        if (rooms.containsKey(e.room)) {
            // not a private message
            rooms[e.room].each { 
                if (it == e.sender)
                    return
                connections[it.destination].sendChat(e)
            }
        } else {
            Persona target = new Persona(new ByteArrayInputStream(Base64.decode(e.room)))
            connections[target.destination]?.sendChat(e)
        }
    }
    
    void stop() {
        if (running.compareAndSet(true, false)) {
            connections.each { k, v ->
                v.close()
            }
        }
    }
}
