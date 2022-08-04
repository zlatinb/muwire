package com.muwire.gui.mulinks

class InvalidMuLinkException extends Exception {
    InvalidMuLinkException(String message) {
        super(message)
    }

    InvalidMuLinkException(String message, Throwable cause) {
        super(message, cause)
    }

    InvalidMuLinkException(Throwable cause) {
        super(cause)
    }
}
