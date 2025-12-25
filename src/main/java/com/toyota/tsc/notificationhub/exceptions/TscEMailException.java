package com.toyota.tsc.notificationhub.exceptions;

/**
 * メール送信例外クラス
 */
public class TscEMailException extends RuntimeException {

    private final String resultCode;

    public TscEMailException(String resultCode) {
        super();
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }
}
