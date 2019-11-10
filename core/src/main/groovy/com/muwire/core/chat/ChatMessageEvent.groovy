package com.muwire.core.chat

import com.muwire.core.Event
import com.muwire.core.Persona

class ChatMessageEvent extends Event {
    String payload
    Persona sender
    String room
    long chatTime
    byte [] sig
}
