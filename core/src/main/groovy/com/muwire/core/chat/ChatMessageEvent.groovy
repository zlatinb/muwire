package com.muwire.core.chat

import com.muwire.core.Event
import com.muwire.core.Persona

class ChatMessageEvent extends Event {
    UUID uuid
    String payload
    Persona sender, host
    ChatLink link
    String room
    long chatTime
    byte [] sig
}
