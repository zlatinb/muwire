package com.muwire.core.trust

import com.muwire.core.Event
import com.muwire.core.Persona
import com.muwire.core.profile.MWProfileHeader

class TrustEvent extends Event {

    Persona persona
    TrustLevel level
    String reason
    MWProfileHeader profileHeader
}
