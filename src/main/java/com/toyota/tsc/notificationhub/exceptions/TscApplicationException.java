package com.toyota.tsc.notificationhub.exceptions;

/**
 * 業務例外（アプリケーション例外）クラス
 */
public class TscApplicationException extends RuntimeException {
    private final String resultCode;

    public TscApplicationException(String resultCode) {
        super();
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }
}
