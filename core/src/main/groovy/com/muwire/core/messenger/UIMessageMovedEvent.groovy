package com.muwire.core.messenger

import com.muwire.core.Event

class UIMessageMovedEvent extends Event {
    MWMessage message
    String from, to
}
