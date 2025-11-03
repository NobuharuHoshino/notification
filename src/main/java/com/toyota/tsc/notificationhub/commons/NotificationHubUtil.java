package com.toyota.tsc.notificationhub.commons;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URLEncoder;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.windowsazure.messaging.NotificationHubsException;
import com.windowsazure.messaging.NotificationOutcome;
import com.windowsazure.messaging.NotificationHub;
import com.windowsazure.messaging.FcmV1Installation;
import com.windowsazure.messaging.FcmV1Notification;
import com.windowsazure.messaging.Notification;
import com.windowsazure.messaging.AppleInstallation;
import com.windowsazure.messaging.AppleNotification;

@Component
public class NotificationHubUtil {

    private final String BRD_TOYOTA = "1";
    private final String BRD_LEXUS = "2";
    private static final String PLATFORM_ANDROID = "1"; // FCM v1
    private static final String PLATFORM_IOS = "2"; // APNs

    @Value("${azure.notification-hub.api-version}")
    private String apiVersion;
    @Value("${azure.notification-hub.root-uri}")
    private String rootUri;
    @Value("${azure.notification-hub.installation-uri}")
    private String installationUri;
    @Value("${azure.notification-hub.sas.ttlSeconds}")
    private int ttlSeconds;
    @Value("${azure.notification-hub.http.timeoutMillis}")
    private int timeoutMillis;
    @Value("${azure.notification-hub.namespace-t}")
    private String namespaceT;
    @Value("${azure.notification-hub.hub-name-t}")
    private String hubNameT;
    @Value("${azure.notification-hub.shared-access-key-name-t}")
    private String keyNameT;
    @Value("${azure.notification-hub.shared-access-key-t}")
    private String keyT;
    @Value("${azure.notification-hub.namespace-l}")
    private String namespaceL;
    @Value("${azure.notification-hub.hub-name-l}")
    private String hubNameL;
    @Value("${azure.notification-hub.shared-access-key-name-l}")
    private String keyNameL;
    @Value("${azure.notification-hub.shared-access-key-l}")
    private String keyL;
    @Value("${azure.notification-hub.sas.template}")
    private String sasTemplate;
    @Value("${azure.notification-hub.installation.payload.template}")
    private String installationPayloadTemplate;
    @Value("${azure.notification-hub.sdk.connection-string}")
    private String sdkConnectionStringTemplate;

    /**
     * SASトークン生成
     * 
     * @param brdCd
     * @return 指定ブランドのNotificationHub向けSASトークンを生成します。
     */
    public String generateSasToken(String brdCd) {
        final String namespace;
        final String hubName;
        final String keyName;
        final String key;
        try {
            if (BRD_TOYOTA.equals(brdCd)) {
                namespace = namespaceT;
                hubName = hubNameT;
                keyName = keyNameT;
                key = keyT;
            } else if (BRD_LEXUS.equals(brdCd)) {
                namespace = namespaceL;
                hubName = hubNameL;
                keyName = keyNameL;
                key = keyL;
            } else {
                return null; // サービスでバリデーションチェックしているため、対応ブランド以外は到達しない想定。
            }
            String uri = String.format(rootUri, namespace, hubName);
            String encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.name());
            long expiry = Instant.now().getEpochSecond() + ttlSeconds;
            String toSign = encodedUri + "\n" + expiry;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getEncoder().encodeToString(mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8)));
            String encodedSig = URLEncoder.encode(signature, StandardCharsets.UTF_8.name());
            return String.format(sasTemplate, encodedUri, encodedSig, expiry, keyName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Installation登録/更新APIリクエストを実行
     * 
     * @param installationId
     * @param brdCd
     * @param internalUserId
     * @param platformCode
     * @param deviceToken
     * @return
     */
    public void upsertInstallation(
            String installationId, String brdCd, String internalUserId,
            String platformCode, String deviceToken) throws NotificationHubsException {
        final String namespace;
        final String hubName;
        final String keyName;
        final String key;
        if (BRD_TOYOTA.equals(brdCd)) {
            namespace = namespaceT;
            hubName = hubNameT;
            keyName = keyNameT;
            key = keyT;
        } else if (BRD_LEXUS.equals(brdCd)) {
            namespace = namespaceL;
            hubName = hubNameL;
            keyName = keyNameL;
            key = keyL;
        } else {
            return; // サービスでバリデーションチェックしているため、対応ブランド以外は到達しない想定。
        }

        final String connectionString = String.format(sdkConnectionStringTemplate, namespace, keyName, key);
        NotificationHub hub = new NotificationHub(connectionString, hubName);

        switch (platformCode) {
            case PLATFORM_ANDROID: { // FCM v1
                FcmV1Installation installation = new FcmV1Installation(installationId);
                installation.setPushChannel(deviceToken);
                installation.getTags().add(internalUserId);
                hub.createOrUpdateInstallation(installation);
                break;
            }
            case PLATFORM_IOS: { // APNs
                AppleInstallation installation = new AppleInstallation(installationId);
                installation.setPushChannel(deviceToken);
                installation.getTags().add(internalUserId);
                hub.createOrUpdateInstallation(installation);
                break;
            }
            default:
                return;
        }
    }

    /**
     * Installation削除APIリクエストを実行
     * 
     * @param installationId
     * @param brdCd
     * @throws NotificationHubsException
     */
    public void deleteInstallation(String installationId, String brdCd) throws NotificationHubsException {
        final String namespace;
        final String hubName;
        final String keyName;
        final String key;
        if (BRD_TOYOTA.equals(brdCd)) {
            namespace = namespaceT;
            hubName = hubNameT;
            keyName = keyNameT;
            key = keyT;
        } else if (BRD_LEXUS.equals(brdCd)) {
            namespace = namespaceL;
            hubName = hubNameL;
            keyName = keyNameL;
            key = keyL;
        } else {
            return; // サービスでバリデーションチェックしているため、対応ブランド以外は到達しない想定。
        }

        final String connectionString = String.format(sdkConnectionStringTemplate, namespace, keyName, key);
        NotificationHub hub = new NotificationHub(connectionString, hubName);
        hub.deleteInstallation(installationId);
    }

    public NotificationOutcome postMessage(
            String installationId, String payload, String brdCd, String platform) throws NotificationHubsException {
        // ブランドでHub接続情報を切り替え
        final String namespace;
        final String hubName;
        final String keyName;
        final String key;
        if (BRD_TOYOTA.equals(brdCd)) {
            namespace = namespaceT;
            hubName = hubNameT;
            keyName = keyNameT;
            key = keyT;
        } else if (BRD_LEXUS.equals(brdCd)) {
            namespace = namespaceL;
            hubName = hubNameL;
            keyName = keyNameL;
            key = keyL;
        } else {
            return null; // サービスでバリデーションチェックしているため、対応ブランド以外は到達しない想定。
        }

        Notification notification;
        switch (platform) {
            case PLATFORM_ANDROID: { // FCM v1
                notification = new FcmV1Notification(payload);
                break;
            }
            case PLATFORM_IOS: { // APNs
                notification = new AppleNotification(payload);
                break;
            }
            default:
                return null;
        }

        // NotificationHub クライアント生成
        final String connectionString = String.format(sdkConnectionStringTemplate, namespace, keyName, key);
        NotificationHub hub = new NotificationHub(connectionString, hubName);
        // 端末（device handle）宛のダイレクト送信
        return hub.sendDirectNotification(notification, installationId);
    }

}