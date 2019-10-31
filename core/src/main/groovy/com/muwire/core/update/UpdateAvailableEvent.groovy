package com.muwire.core.update

import com.muwire.core.Event
import com.muwire.core.InfoHash

class UpdateAvailableEvent extends Event {
    String version
    String signer
    String infoHash
    String text
}
