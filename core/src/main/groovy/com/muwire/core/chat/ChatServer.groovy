package com.muwire.core.chat

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.stream.Collectors

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
import net.i2p.data.SigningPrivateKey
import net.i2p.util.ConcurrentHashSet

@Log
class ChatServer {
    public static final String CONSOLE = "__CONSOLE__"
    private static final String DEFAULT_WELCOME = "Welcome to my chat server!  Type /HELP for list of available commands"
    
    private final EventBus eventBus
    private final MuWireSettings settings
    private final TrustService trustService
    private final Persona me
    private final SigningPrivateKey spk
    
    private final Map<Destination, ChatLink> connections = new ConcurrentHashMap()
    private final Map<String, Set<Persona>> rooms = new ConcurrentHashMap<>()
    private final Map<Persona, Set<String>> memberships = new ConcurrentHashMap<>()
    private final Map<String, Persona> shortNames = new ConcurrentHashMap<>()
    
    private final AtomicBoolean running = new AtomicBoolean()
    
    ChatServer(EventBus eventBus, MuWireSettings settings, TrustService trustService, Persona me, SigningPrivateKey spk) {
        this.eventBus = eventBus
        this.settings = settings
        this.trustService = trustService
        this.me = me
        this.spk = spk
        
        Timer timer = new Timer("chat-server-pinger", true)
        timer.schedule({sendPings()} as TimerTask, 1000, 1000)
    }
    
    public void start() {
        if (!running.compareAndSet(false, true))
            return
        connections.put(me.destination, LocalChatLink.INSTANCE)
        joinRoom(me, CONSOLE)
        shortNames.put(me.getHumanReadableName(), me)
        echo(getWelcome(),me.destination)
    }
    
    public boolean isRunning() {
        running.get()
    }
    
    private String getWelcome() {
        String welcome = DEFAULT_WELCOME
        if (settings.chatWelcomeFile != null)
            welcome = settings.chatWelcomeFile.text
        "/SAY $welcome"
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
            if (settings.defaultChatRoom.size() > 0)
                write("DefaultRoom:${settings.defaultChatRoom}\r\n".getBytes(StandardCharsets.US_ASCII))
            write("\r\n".getBytes(StandardCharsets.US_ASCII))
            flush()
        }
        
        ChatConnection connection = new ChatConnection(eventBus, endpoint, client, true, trustService, settings)
        connections.put(endpoint.destination, connection)
        joinRoom(client, CONSOLE)
        shortNames.put(client.getHumanReadableName(), client)
        connection.start()
        echo(getWelcome(),connection.endpoint.destination)
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
        shortNames.remove(e.persona.getHumanReadableName())
        connections.each { k, v ->
            v.sendLeave(e.persona)
        }
    }
    
    void onTrustEvent(TrustEvent e) {
        if (e.level == TrustLevel.TRUSTED)
            return
        if (settings.allowUntrusted && e.level == TrustLevel.NEUTRAL)
            return
        
        ChatConnection connection = connections.get(e.persona.destination)
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

        if (command.action.console && e.room != CONSOLE) {
            echo("/SAY ERROR: You can only execute that command in the chat console, not in a chat room.",
                    e.sender.destination, e.room)
            return
        }
        
        if (!command.action.console && e.room == CONSOLE) {
            echo("/SAY ERROR: You need to be in a chat room.  Type /LIST for list of rooms or /JOIN to join or create a room.",
                    e.sender.destination)
            return
        }
        
        if (!command.action.user)
            return
        
        if (command.action.local && e.sender != me)
            return
            
        switch(command.action) {
            case ChatAction.JOIN : processJoin(command.payload, e); break
            case ChatAction.LEAVE : processLeave(e); break
            case ChatAction.SAY : processSay(e); break
            case ChatAction.LIST : processList(e.sender.destination); break
            case ChatAction.INFO : processInfo(e.sender.destination); break
            case ChatAction.HELP : processHelp(e.sender.destination); break
            case ChatAction.TRUST : processTrust(command.payload, TrustLevel.TRUSTED); break
            case ChatAction.DISTRUST : processTrust(command.payload, TrustLevel.DISTRUSTED); break
        }
    }
    
    private void processJoin(String room, ChatMessageEvent e) {
        joinRoom(e.sender, room)
        rooms[room].each { 
            if (it == e.sender)
                return
            connections[it.destination].sendChat(e)
        }
        String payload = rooms[room].stream().filter({it != e.sender}).map({it.toBase64()})
            .collect(Collectors.joining(","))
        if (payload.length() == 0) {
            return
        }
        payload = "/JOINED $payload"
        long now = System.currentTimeMillis()
        UUID uuid = UUID.randomUUID()
        byte [] sig = ChatConnection.sign(uuid, now, room, payload, me, me, spk)
        ChatMessageEvent echo = new ChatMessageEvent(
            uuid : uuid,
            payload : payload,
            sender : me,
            host : me,
            room : room,
            chatTime : now,
            sig : sig
            )
        connections[e.sender.destination].sendChat(echo)
    }
    
    private void processLeave(ChatMessageEvent e) {
        leaveRoom(e.sender, e.room)
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
    
    private void processList(Destination d) {
        String roomList = rooms.keySet().stream().filter({it != CONSOLE}).collect(Collectors.joining("\n"))
        roomList = "/SAY \nRoom List:\n"+roomList
        echo(roomList, d)
    }
    
    private void processInfo(Destination d) {
        String info = "/SAY \nThe address of this server is\n========\n${me.toBase64()}\n========\nCopy/paste the above and share it\n"
        String connectedUsers = memberships.keySet().stream().map({it.getHumanReadableName()}).collect(Collectors.joining("\n"))
        info = "${info}\nConnected Users:\n$connectedUsers\n======="
        echo(info, d)
    }
    
    private void processHelp(Destination d) {
        String help = """/SAY 
            Available commands: /JOIN /LEAVE /SAY /LIST /INFO /TRUST /DISTRUST /HELP
            /JOIN <room name>  - joins a room, or creates one if it does not exist.  You must type this in the console
            /LEAVE             - leaves a room.  You must type this in the room you want to leave
            /SAY               - optional, says something in the room you're in
            /LIST              - lists the existing rooms on this server.  You must type this in the console
            /INFO              - shows information about this server.  You must type this in the console
            /TRUST <user>      - marks user as trusted.  This is only available to the server owner
            /DISTRUST <user>   - marks user as distrusted.  This is only available to the server owner
            /HELP              - prints this help message
            """
        echo(help, d)
    }
    
    private void echo(String payload, Destination d, String room = CONSOLE) {
        log.info "echoing $payload"
        UUID uuid = UUID.randomUUID()
        long now = System.currentTimeMillis()
        byte [] sig = ChatConnection.sign(uuid, now, room, payload, me, me, spk)
        ChatMessageEvent echo = new ChatMessageEvent(
            uuid : uuid,
            payload : payload,
            sender : me,
            host : me,
            room : room,
            chatTime : now,
            sig : sig
            )
        connections[d]?.sendChat(echo)
    }
    
    private void processTrust(String shortName, TrustLevel level) {
        Persona p = shortNames.get(shortName)
        if (p == null)
            return
        eventBus.publish(new TrustEvent(persona : p, level : level))
    }
    
    void stop() {
        if (running.compareAndSet(true, false)) {
            connections.each { k, v ->
                v.close()
            }
        }
    }
}
