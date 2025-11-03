package com.toyota.tsc.notificationhub.exceptions;

public class TscEMailException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;
    private final String address;

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getAddress() {
        return address;
    }

    public TscEMailException(int statusCode, String responseBody, String address) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.address = address;
    }

    public TscEMailException(String message, Throwable cause, String address) {
        this.statusCode = -1; // I/O等でHTTPコードが得られない場合
        this.responseBody = null;
        this.address = address;
    }
}
