package com.toyota.tsc.notificationhub.exceptions;

/**
 * DB操作系エラーを表すカスタム例外クラス（RuntimeException継承）
 */
public class CustomSqlException extends RuntimeException {
    public CustomSqlException() {
        super();
    }

    public CustomSqlException(String message) {
        super(message);
    }

    public CustomSqlException(String message, Throwable cause) {
        super(message, cause);
    }
}
