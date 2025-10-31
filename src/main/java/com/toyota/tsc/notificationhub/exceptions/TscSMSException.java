package com.toyota.tsc.notificationhub.exceptions;

public class TscSMSException extends RuntimeException {
    public TscSMSException(String message) {
        super(message);
    }

    public TscSMSException(String message, Throwable cause) {
        super(message, cause);
    }
}
