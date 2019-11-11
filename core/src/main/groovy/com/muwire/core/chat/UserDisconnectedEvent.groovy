package com.muwire.core.chat

import com.muwire.core.Event
import com.muwire.core.Persona

class UserDisconnectedEvent extends Event {
    Persona user
    Persona host
}
