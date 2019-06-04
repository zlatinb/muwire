package com.muwire.core.download

import com.muwire.core.Event
import com.muwire.core.search.UIResultEvent

class UIDownloadEvent extends Event {
    
    UIResultEvent[] result
    File target
}
