package com.muwire.core.chat

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

import com.muwire.core.Constants
import com.muwire.core.EventBus
import com.muwire.core.MuWireSettings
import com.muwire.core.Persona
import com.muwire.core.connection.Endpoint
import com.muwire.core.trust.TrustService
import com.muwire.core.util.DataUtil

import net.i2p.data.Base64
import net.i2p.data.Destination
import net.i2p.util.ConcurrentHashSet

class ChatServer {
    private final EventBus eventBus
    private final MuWireSettings settings
    private final TrustService trustService
    
    private final Map<Destination, ChatConnection> connections = new ConcurrentHashMap()
    
    ChatServer(EventBus eventBus, MuWireSettings settings, TrustService trustService) {
        this.eventBus = eventBus
        this.settings = settings
        this.trustService = trustService
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
        
        if (!settings.enableChat) {
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
        connection.start()
        eventBus.publish(new ChatConnectionEvent(connection : connection, status : ChatConnectionAttemptStatus.SUCCESSFUL, persona : client))
    }
}
