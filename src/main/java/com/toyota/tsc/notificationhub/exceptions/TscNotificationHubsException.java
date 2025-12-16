package com.toyota.tsc.notificationhub.exceptions;

/**
 * NotificationHub関連の例外を丸める独自例外クラス
 */
public class TscNotificationHubsException extends RuntimeException {
    private final String resultCode;

    public TscNotificationHubsException(String resultCode) {
        super();
        this.resultCode = resultCode;
    }

    public String getResultCode() {
        return resultCode;
    }
}
