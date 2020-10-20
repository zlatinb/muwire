package com.muwire.core.search

import com.muwire.core.Event
import com.muwire.core.Persona

class UIBrowseEvent extends Event {
    UUID uuid
    Persona host
}
