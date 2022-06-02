package com.muwire.core.chat

import com.muwire.core.profile.MWProfileHeader

import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

import com.muwire.core.Persona

import groovy.util.logging.Log

@Log
class LocalChatLink implements ChatLink {
    
    public static final LocalChatLink INSTANCE = new LocalChatLink()
    
    private final BlockingQueue messages = new LinkedBlockingQueue()

    private LocalChatLink() {}
    
    @Override
    public void close() throws IOException {
    }

    @Override
    public void sendChat(ChatMessageEvent e) {
        messages.put(e)
    }

    @Override
    public void sendLeave(Persona p) {
        messages.put(p)
    }

    @Override
    public void sendPing() {}
    
    @Override
    public Object nextEvent() {
        messages.take()
    }

    @Override
    public boolean isUp() {
        true
    }
    
    public Persona getPersona() {
        null
    }
    
    public MWProfileHeader getProfileHeader() {
        null
    }
}
