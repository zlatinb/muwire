package com.muwire.core.update

import com.muwire.core.Event

class UpdateAvailableEvent extends Event {
    String version
    String signer
}
