package com.muwire.core.search

class UnexpectedResultsException extends Exception {

    public UnexpectedResultsException() {
        super();
    }

    public UnexpectedResultsException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UnexpectedResultsException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnexpectedResultsException(String message) {
        super(message);
    }

}
