package com.muwire.core.filecert

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

class CertificateFetchedEvent extends Event {
    Certificate certificate
    Persona user
    InfoHash infoHash
}
