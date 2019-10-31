package com.muwire.core.update

import com.muwire.core.Event

class UpdateDownloadedEvent extends Event {
    String version
    String signer
    String text
}
