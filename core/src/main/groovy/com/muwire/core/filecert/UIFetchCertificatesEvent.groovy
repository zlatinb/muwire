package com.muwire.core.filecert

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

class UIFetchCertificatesEvent extends Event {
    Persona host
    InfoHash infoHash
}
