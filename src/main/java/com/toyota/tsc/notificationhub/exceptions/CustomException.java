package com.toyota.tsc.notificationhub.exceptions;

/**
 * 汎用カスタム例外クラス（SonarQube指摘対応用）
 */
public class CustomException extends RuntimeException {
    public CustomException() {
        super();
    }

    public CustomException(String message) {
        super(message);
    }

    public CustomException(String message, Throwable cause) {
        super(message, cause);
    }

    public CustomException(Throwable cause) {
        super(cause);
    }
}
