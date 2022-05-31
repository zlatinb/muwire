package com.muwire.core.profile

import com.muwire.core.Event
import com.muwire.core.Persona

class UIProfileFetchEvent extends Event {
    UUID uuid
    Persona host
}
