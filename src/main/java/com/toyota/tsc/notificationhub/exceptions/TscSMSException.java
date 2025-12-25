package com.toyota.tsc.notificationhub.exceptions;

/**
 * SMS送信例外クラス
 */
public class TscSMSException extends RuntimeException {
    private final String resultCode;

    public TscSMSException(String resultCode) {
        super();
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }
}
