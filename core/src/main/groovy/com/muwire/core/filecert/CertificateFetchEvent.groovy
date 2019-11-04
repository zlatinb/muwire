package com.muwire.core.filecert

import com.muwire.core.Event

class CertificateFetchEvent extends Event {
    CertificateFetchStatus status
    int count
}
