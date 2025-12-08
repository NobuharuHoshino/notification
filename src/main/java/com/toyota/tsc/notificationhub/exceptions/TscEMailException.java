package com.toyota.tsc.notificationhub.exceptions;

/**
 * メール送信例外クラス
 */
public class TscEMailException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;
    private final String address;
    private final String title;

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getAddress() {
        return address;
    }

    public String getTitle() {
        return title;
    }

    public TscEMailException(String address) {
        this.statusCode = -1; // I/O等でHTTPコードが得られない場合
        this.responseBody = null;
        this.address = address;
        this.title = null;
    }

    public TscEMailException(String message, Throwable cause, String address) {
        super(message, cause);
        this.statusCode = -1; // I/O等でHTTPコードが得られない場合
        this.responseBody = null;
        this.address = address;
        this.title = null;
    }

    public TscEMailException(int statusCode, String address, String title) {
        super(null, null);
        this.statusCode = statusCode;
        this.responseBody = null;
        this.address = address;
        this.title = title;
    }
}
