package com.muwire.core.download

class BadHashException extends Exception {

    public BadHashException() {
        super();
    }

    public BadHashException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public BadHashException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadHashException(String message) {
        super(message);
    }

    public BadHashException(Throwable cause) {
        super(cause);
    }

}
