package com.muwire.core.chat;

import java.io.Closeable;

import com.muwire.core.Persona;

public interface ChatLink extends Closeable {
    public Persona getPersona();
    public boolean isUp();
    public void sendChat(ChatMessageEvent e);
    public void sendLeave(Persona p);
    public void sendPing();
    public Object nextEvent() throws InterruptedException;
}
