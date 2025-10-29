package com.toyota.tsc.notificationhub.exceptions;

public class TscEMailException extends RuntimeException {
    public TscEMailException(String message) {
        super(message);
    }

    public TscEMailException(String message, Throwable cause) {
        super(message, cause);
    }
}
