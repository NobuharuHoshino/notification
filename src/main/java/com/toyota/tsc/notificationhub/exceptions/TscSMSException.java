package com.toyota.tsc.notificationhub.exceptions;

/**
 * SMS送信例外クラス
 */
public class TscSMSException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;
    private final String phoneNo;

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public TscSMSException(int statusCode, String responseBody, String phoneNo) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.phoneNo = phoneNo;
    }

    public TscSMSException(String message, Throwable cause, String phoneNo) {
        this.statusCode = -1; // I/O等でHTTPコードが得られない場合
        this.responseBody = null;
        this.phoneNo = phoneNo;
    }
}
