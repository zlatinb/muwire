package com.muwire.core.files

import com.muwire.core.Event
import com.muwire.core.InfoHash

class InfoHashEvent extends Event {
    File file
    InfoHash infoHash
}
