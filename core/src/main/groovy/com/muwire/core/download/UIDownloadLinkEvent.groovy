package com.muwire.core.download

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

class UIDownloadLinkEvent extends Event {
    Persona host
    InfoHash infoHash
    String fileName
    long length
    int pieceSizePow2
}
