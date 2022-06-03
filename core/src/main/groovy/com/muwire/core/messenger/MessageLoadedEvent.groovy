package com.muwire.core.messenger

import com.muwire.core.Event
import com.muwire.core.profile.MWProfileHeader

class MessageLoadedEvent extends Event {
    MWMessage message
    String folder
    boolean unread
    MWProfileHeader profileHeader
}
