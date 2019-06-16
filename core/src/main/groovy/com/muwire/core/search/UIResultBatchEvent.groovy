package com.muwire.core.search

import com.muwire.core.Event

class UIResultBatchEvent extends Event {
    UUID uuid
    UIResultEvent[] results
}
