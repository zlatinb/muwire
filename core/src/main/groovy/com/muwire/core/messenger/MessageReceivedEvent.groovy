package com.muwire.core.messenger

import com.muwire.core.Event
import com.muwire.core.profile.MWProfileHeader

class MessageReceivedEvent extends Event {
    MWMessage message
    MWProfileHeader profileHeader
}
