package com.muwire.gui

import com.muwire.core.messenger.MWMessage

class MWMessageStatus {
    private final MWMessage message
    private boolean status
    MWMessageStatus(MWMessage message, boolean status) {
        this.message = message
        this.status = status
    }

    int hashCode() {
        message.hashCode()
    }

    boolean equals(Object o) {
        MWMessageStatus other = (MWMessageStatus) o
        message.equals(other.message)
    }
}
