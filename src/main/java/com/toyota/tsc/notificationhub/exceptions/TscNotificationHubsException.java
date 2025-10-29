package com.toyota.tsc.notificationhub.exceptions;

/**
 * NotificationHub関連の例外を丸める独自例外クラス
 */
public class TscNotificationHubsException extends RuntimeException {
    public TscNotificationHubsException(String message) {
        super(message);
    }
    public TscNotificationHubsException(String message, Throwable cause) {
        super(message, cause);
    }
    public TscNotificationHubsException(Throwable cause) {
        super(cause);
    }
}
