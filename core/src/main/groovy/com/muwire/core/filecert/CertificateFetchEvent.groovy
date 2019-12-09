package com.muwire.core.filecert

import com.muwire.core.Event
import com.muwire.core.InfoHash
import com.muwire.core.Persona

class CertificateFetchEvent extends Event {
    CertificateFetchStatus status
    int count
    Persona user
    InfoHash infoHash
}
