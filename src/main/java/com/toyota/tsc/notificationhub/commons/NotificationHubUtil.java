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

/**
 * Azure Notification Hub連携ユーティリティクラス
 */
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
     * SASトークンを生成します。
     * 
     * @param brdCd ブランドコード
     * @return SASトークン文字列（失敗時はnull）
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
     * Installation情報を新規登録または更新します。
     * 
     * @param installationId Installation ID
     * @param brdCd          ブランドコード
     * @param internalUserId ユーザーID
     * @param platformCode   プラットフォームコード
     * @param deviceToken    デバイストークン
     * @return なし
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
                FcmV1Installation installation = new FcmV1Installation(
                        installationId,
                        deviceToken,
                        new String[] { internalUserId });
                hub.createOrUpdateInstallation(installation);
                break;
            }
            case PLATFORM_IOS: { // APNs
                AppleInstallation installation = new AppleInstallation(
                        installationId,
                        deviceToken,
                        new String[] { internalUserId });
                hub.createOrUpdateInstallation(installation);
                break;
            }
            default:
                return;
        }
    }

    /**
     * Installation情報を削除します。
     * 
     * @param installationId Installation ID
     * @param brdCd          ブランドコード
     * @return なし
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

    /**
     * メッセージを端末へ送信します。
     * 
     * @param installationId Installation ID
     * @param payload        メッセージペイロード
     * @param brdCd          ブランドコード
     * @param platform       プラットフォームコード
     * @return NotificationOutcome（送信結果）
     */
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