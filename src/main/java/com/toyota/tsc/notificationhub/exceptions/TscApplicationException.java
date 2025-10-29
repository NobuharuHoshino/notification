package com.toyota.tsc.notificationhub.exceptions;

/**
 * 業務例外（アプリケーション例外）クラス
 */
public class TscApplicationException extends RuntimeException {
    private final String errorCode;

    public TscApplicationException() {
        super();
        this.errorCode = null;
    }

    public TscApplicationException(String message) {
        super(message);
        this.errorCode = null;
    }

    public TscApplicationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public TscApplicationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    public TscApplicationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
