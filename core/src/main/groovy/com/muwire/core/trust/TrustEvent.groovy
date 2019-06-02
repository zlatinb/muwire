package com.muwire.core.trust

import com.muwire.core.Event
import com.muwire.core.Persona

class TrustEvent extends Event {

	Persona persona
	TrustLevel level
}
