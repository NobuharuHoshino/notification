package com.toyota.tsc.notificationhub.exceptions;

/**
 * 業務例外（アプリケーション例外）クラス
 */
public class TscPrimaryContactException extends RuntimeException {
    private final String resultCode;

    public TscPrimaryContactException(String resultCode) {
        super();
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }
}
