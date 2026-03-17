package com.toyota.tsc.notificationhub.commons;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.net.URLEncoder;
import java.time.Instant;
import org.springframework.stereotype.Component;

import com.windowsazure.messaging.NotificationHubsException;
import com.windowsazure.messaging.NotificationOutcome;

import com.windowsazure.messaging.NotificationHub;
import com.windowsazure.messaging.FcmV1Installation;
import com.windowsazure.messaging.FcmV1Notification;
import com.windowsazure.messaging.Notification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.windowsazure.messaging.AppleInstallation;
import com.windowsazure.messaging.AppleNotification;

/**
 * Azure Notification Hub連携ユーティリティクラス
 */
@Component
public class NotificationHubUtil {

    private static final String BRD_TOYOTA = "1";
    private static final String BRD_LEXUS = "2";
    private static final String PLATFORM_ANDROID = "1"; // FCM v1
    private static final String PLATFORM_IOS = "2"; // APNs
    private static final String[] TAGS = { "internalUserId" };
    private static final String PUSH_INFORMATION_LIST = "pushInformationList";
    private static final String LOC_KEY = "LocKey";
    private static final String LCS_SELECTED = "lcsSelected";
    private static final String PUSH_FLG = "pushFlg";
    private static final String PUSH_DEEP_LINK = "pushDeepLink";
    private static final String POPUP_BUTTON_DEEP_LINK = "popUpButtonDeepLink";
    private static final String POPUP_INFORMATION_LIST = "popupInformationList";
    private static final String NOTIFICATION_ID = "notificationId";
    private static final String ANDROID = "android";
    private static final String DATA = "data";
    private static final String MESSAGE = "message";
    private static final String APS = "aps";
    private static final String ALERT = "alert";
    private static final String MUTABLE_CONTENT = "mutable-content";
    private static final String AVAILABLE_CONTENT = "content-available";

    private PropertiesUtil propertiesUtil;

    public NotificationHubUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

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
                namespace = propertiesUtil.getNamespaceT();
                hubName = propertiesUtil.getHubNameT();
                keyName = propertiesUtil.getKeyNameT();
                key = propertiesUtil.getKeyT();
            } else if (BRD_LEXUS.equals(brdCd)) {
                namespace = propertiesUtil.getNamespaceL();
                hubName = propertiesUtil.getHubNameL();
                keyName = propertiesUtil.getKeyNameL();
                key = propertiesUtil.getKeyL();
            } else {
                return null; // サービスでバリデーションチェックしているため、対応ブランド以外は到達しない想定。
            }
            String uri = String.format(propertiesUtil.getRootUri(), namespace, hubName);
            String encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.name());
            long expiry = Instant.now().getEpochSecond() + propertiesUtil.getTtlSeconds();
            String toSign = encodedUri + "\n" + expiry;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getEncoder().encodeToString(mac.doFinal(toSign.getBytes(StandardCharsets.UTF_8)));
            String encodedSig = URLEncoder.encode(signature, StandardCharsets.UTF_8.name());
            return String.format(propertiesUtil.getSasTemplate(), encodedUri, encodedSig, expiry, keyName);
        } catch (Exception e) {
            throw new CustomException(e);
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
            namespace = propertiesUtil.getNamespaceT();
            hubName = propertiesUtil.getHubNameT();
            keyName = propertiesUtil.getKeyNameT();
            key = propertiesUtil.getKeyT();
        } else if (BRD_LEXUS.equals(brdCd)) {
            namespace = propertiesUtil.getNamespaceL();
            hubName = propertiesUtil.getHubNameL();
            keyName = propertiesUtil.getKeyNameL();
            key = propertiesUtil.getKeyL();
        } else {
            return; // サービスでバリデーションチェックしているため、対応ブランド以外は到達しない想定。
        }

        final String connectionString = String.format(propertiesUtil.getSdkConnectionStringTemplate(), namespace,
                keyName, key);
        NotificationHub hub = new NotificationHub(connectionString, hubName);

        switch (platformCode) {
            case PLATFORM_ANDROID: { // FCM v1
                FcmV1Installation installation = new FcmV1Installation(
                        installationId,
                        deviceToken,
                        TAGS);
                hub.createOrUpdateInstallation(installation);
                break;
            }
            case PLATFORM_IOS: { // APNs
                AppleInstallation installation = new AppleInstallation(
                        installationId,
                        deviceToken,
                        TAGS);
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
            namespace = propertiesUtil.getNamespaceT();
            hubName = propertiesUtil.getHubNameT();
            keyName = propertiesUtil.getKeyNameT();
            key = propertiesUtil.getKeyT();
        } else if (BRD_LEXUS.equals(brdCd)) {
            namespace = propertiesUtil.getNamespaceL();
            hubName = propertiesUtil.getHubNameL();
            keyName = propertiesUtil.getKeyNameL();
            key = propertiesUtil.getKeyL();
        } else {
            return; // サービスでバリデーションチェックしているため、対応ブランド以外は到達しない想定。
        }

        final String connectionString = String.format(propertiesUtil.getSdkConnectionStringTemplate(), namespace,
                keyName, key);
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
            namespace = propertiesUtil.getNamespaceT();
            hubName = propertiesUtil.getHubNameT();
            keyName = propertiesUtil.getKeyNameT();
            key = propertiesUtil.getKeyT();
        } else if (BRD_LEXUS.equals(brdCd)) {
            namespace = propertiesUtil.getNamespaceL();
            hubName = propertiesUtil.getHubNameL();
            keyName = propertiesUtil.getKeyNameL();
            key = propertiesUtil.getKeyL();
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
        final String connectionString = String.format(propertiesUtil.getSdkConnectionStringTemplate(), namespace,
                keyName, key);
        LogUtil.info(getClass(), payload);
        LogUtil.info(getClass(), connectionString);
        NotificationHub hub = new NotificationHub(connectionString, hubName);
        String tagExpr = String.format("$InstallationId:{%s}", installationId);

        NotificationOutcome outcome = hub.sendNotification(notification, tagExpr);

        String notificationId = outcome.getNotificationId(); // ← Java SDKのgetter名は実装に依存（概念は同じ）
        String trackingId = outcome.getTrackingId(); // ← これも同様

        LogUtil.info(getClass(), "notificationId=" + notificationId);
        LogUtil.info(getClass(), "trackingId=" + trackingId);
        LogUtil.info(getClass(), "installation=" + installationId);

        return outcome;
    }

    /**
     * ペイロードを組み立てます。
     *
     * @param bodyData ペイロードデータマップ
     * @return FCM v1ペイロード文字列
     */
    public String buildFcmV1Payload(String bodyData) {
        Map<String, Object> bodyDataMap = toMap(bodyData);
        ObjectMapper mapper = new ObjectMapper();
        try {
            ObjectNode dataNode = mapper.createObjectNode();
            Object pushInformationList = bodyDataMap.get(PUSH_INFORMATION_LIST);
            if (pushInformationList != null) {
                String json = mapper.writeValueAsString(pushInformationList);
                dataNode.put(PUSH_INFORMATION_LIST, json);
            }
            putAsStringIfPresent(dataNode, bodyDataMap, LOC_KEY);
            putAsStringIfPresent(dataNode, bodyDataMap, LCS_SELECTED);
            putAsStringIfPresent(dataNode, bodyDataMap, PUSH_FLG);
            putAsStringIfPresent(dataNode, bodyDataMap, PUSH_DEEP_LINK);
            putAsStringIfPresent(dataNode, bodyDataMap, POPUP_BUTTON_DEEP_LINK);
            Object popupInformationList = bodyDataMap.get(POPUP_INFORMATION_LIST);
            if (popupInformationList != null) {
                String json = mapper.writeValueAsString(popupInformationList);
                dataNode.put(POPUP_INFORMATION_LIST, json);
            }
            putAsStringIfPresent(dataNode, bodyDataMap, NOTIFICATION_ID);
            ObjectNode androidNode = mapper.createObjectNode();
            androidNode.put("priority", "high");
            androidNode.set(DATA, dataNode);
            ObjectNode messageNode = mapper.createObjectNode();
            messageNode.set(ANDROID, androidNode);
            ObjectNode root = mapper.createObjectNode();
            root.set(MESSAGE, messageNode);

            return mapper.writeValueAsString(root);

        } catch (JsonProcessingException e) {
            throw new CustomException(e);
        }
    }

    /**
     * bodyData の値を文字列化して dataNode に設定します。
     * 値が Number/Boolean でも String.valueOf(...) で文字列統一。
     */
    private void putAsStringIfPresent(ObjectNode dataNode, Map<String, Object> bodyData, String key) {
        Object val = bodyData.get(key);
        if (val != null) {
            dataNode.put(key, String.valueOf(val));
        }
    }

    public String buildApnsPayload(String bodyData) {
        Map<String, Object> bodyDataMap = toMap(bodyData);
        ObjectMapper mapper = new ObjectMapper();
        try {
            ObjectNode root = mapper.createObjectNode();
            ObjectNode aps = mapper.createObjectNode();
            // サイレントプッシュ用のデータ取得
            String locKey = extractLocKey(bodyDataMap);
            String silentLockKeys = propertiesUtil.getSilentPushLockeys();
            if (CommonUtil.containsData(CommonUtil.csvToList(silentLockKeys), locKey)) {
                aps.put(AVAILABLE_CONTENT, 1);
            } else {
                aps.put(ALERT, " ");
                aps.put(MUTABLE_CONTENT, 1);
            }
            root.set(APS, aps);
            Object pushInformationList = bodyDataMap.get(PUSH_INFORMATION_LIST);
            if (pushInformationList != null) {
                root.set(PUSH_INFORMATION_LIST, mapper.valueToTree(pushInformationList));
            }
            Object popupInformationList = bodyDataMap.get(POPUP_INFORMATION_LIST);
            if (popupInformationList != null) {
                root.set(POPUP_INFORMATION_LIST, mapper.valueToTree(popupInformationList));
            }
            putIfPresent(root, mapper, bodyDataMap, LOC_KEY);
            putIfPresent(root, mapper, bodyDataMap, LCS_SELECTED);
            putIfPresent(root, mapper, bodyDataMap, PUSH_FLG);
            putIfPresent(root, mapper, bodyDataMap, PUSH_DEEP_LINK);
            putIfPresent(root, mapper, bodyDataMap, POPUP_BUTTON_DEEP_LINK);
            putIfPresent(root, mapper, bodyDataMap, NOTIFICATION_ID);
            return mapper.writeValueAsString(root);

        } catch (JsonProcessingException e) {
            throw new CustomException(e);
        }
    }

    /**
     * bodyDataからロケーションキー（LokKey）を取得します。
     * 
     * @param bodyData ペイロードデータマップ
     * @return ロケーションキー（存在しない場合はnull）
     */
    public String extractLocKey(Map<String, Object> bodyDataMap) {
        Object locKeyObj = bodyDataMap.get(LOC_KEY);
        return locKeyObj != null ? String.valueOf(locKeyObj) : null;
    }

    /**
     * bodyData の値を「そのままの型」で JSON ノードに変換して root に設定します。
     * String/Number/Boolean/Map/List/POJO いずれでも valueToTree でツリー化できます。
     */
    private void putIfPresent(ObjectNode root, ObjectMapper mapper,
            Map<String, Object> bodyData, String key) {
        Object val = bodyData.get(key);
        if (val != null) {
            root.set(key, mapper.valueToTree(val));
        }
    }

    /**
     * JSON形式の文字列を Map<String, Object> に変換するだけ（null/空チェック等は一切しない）
     */
    public static Map<String, Object> toMap(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new CustomException(e);
        }
    }

    /**
     * platformに応じたパスのlcsSelectedを置換してJSON文字列を返す。
     * ANDROID: message.android.data.lcsSelected
     * IOS : (root).lcsSelected
     */
    public String replaceLcsSelected(String payload, String platform, String internalLicenseCode) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(payload);
            ObjectNode root = (ObjectNode) rootNode;
            switch (platform) {
                case PLATFORM_ANDROID:
                    ObjectNode dataNode = requireObject(root, MESSAGE, ANDROID, DATA);
                    dataNode.put(LCS_SELECTED, internalLicenseCode);
                    break;
                case PLATFORM_IOS:
                    root.put(LCS_SELECTED, internalLicenseCode);
                    break;
                default:
                    return null;
            }
            return mapper.writeValueAsString(root);

        } catch (JsonProcessingException e) {
            throw new CustomException(e);
        }
    }

    /**
     * 指定パスのノードが「存在するObject」であることを保証して返す（無い/型違いなら例外）
     */
    private ObjectNode requireObject(ObjectNode root, String... path) {
        JsonNode current = root;
        StringBuilder p = new StringBuilder("$");
        for (String key : path) {
            p.append(".").append(key);
            current = current.get(key);
            if (current == null || current.isNull()) {
                throw new CustomException();
            }
            if (!current.isObject()) {
                throw new CustomException();
            }
        }
        return (ObjectNode) current;
    }

}