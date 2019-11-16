package com.muwire.core.chat

import com.muwire.core.Event
import com.muwire.core.Persona

class ChatConnectionEvent extends Event {
    ChatConnectionAttemptStatus status
    Persona persona
    ChatLink connection
    
    public String toString() {
        super.toString() + " " + persona.getHumanReadableName() + " " + status.toString()
    }
}
