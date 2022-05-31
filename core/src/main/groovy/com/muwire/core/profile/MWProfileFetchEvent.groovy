package com.muwire.core.profile

import com.muwire.core.Event
import com.muwire.core.Persona

class MWProfileFetchEvent extends Event {
    MWProfileFetchStatus status
    Persona host
    UUID uuid
    MWProfile profile
}
