package com.muwire.core.trust

import com.muwire.core.Event
import com.muwire.core.Persona

class TrustSubscriptionEvent extends Event {
    Persona persona
    boolean subscribe
}
