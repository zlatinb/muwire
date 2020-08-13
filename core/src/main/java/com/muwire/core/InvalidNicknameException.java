package com.muwire.core;

public class InvalidNicknameException extends Exception {

    public InvalidNicknameException() {
    }

    public InvalidNicknameException(String message) {
        super(message);
    }

    public InvalidNicknameException(Throwable cause) {
        super(cause);
    }

    public InvalidNicknameException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidNicknameException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
