package com.muwire.core.chat;

import java.io.Closeable;

import com.muwire.core.Persona;
import com.muwire.core.profile.MWProfileHeader;

public interface ChatLink extends Closeable {
    public Persona getPersona();
    public MWProfileHeader getProfileHeader();
    public boolean isUp();
    public void sendChat(ChatMessageEvent e);
    public void sendLeave(Persona p);
    public void sendPing();
    public Object nextEvent() throws InterruptedException;
}
