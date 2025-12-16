package com.toyota.tsc.notificationhub.commons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.MailContextDto;
import com.toyota.tsc.notificationhub.models.SmsContextDto;

public class JsapUtil {

    private PropertiesUtil propertiesUtil;

    public JsapUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    private static final String PLATFORM_ANDROID = "1"; // FCM v1
    private static final String PLATFORM_IOS = "2"; // APNs
    private static final String X_API_KEY_HEADER = "x-api-key";
    private static final String USER_ID_BODY = "userId";

    public ResponseEntity<String> executeGetUserId(String internalUserId, String colId) {
        try {
            // テンプレート
            RestTemplate restTemplate = new RestTemplate();
            // ヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapGetUserIdApiKey());
            headers.set("x-correlation-id", colId);
            // ボディ設定
            Map<String, Object> body = new HashMap<>();
            body.put("internalUserId", internalUserId);
            // エンティティセット
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 実行
            return restTemplate.exchange(
                    propertiesUtil.getJsapGetUserIdApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public ResponseEntity<String> executeDvcLink(String userId, String dvcToken, String platform) {
        try {
            // プラットフォーム変換
            if (PLATFORM_ANDROID.equals(platform)) {
                platform = "android";
            } else if (PLATFORM_IOS.equals(platform)) {
                platform = "ios";
            } else {
                return null; // サービスでバリデーションチェックしているため、対応ブランド以外は到達しない想定。
            }
            // テンプレート
            RestTemplate restTemplate = new RestTemplate();
            // ヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapDvcLinkApiKey());
            // ボディ設定
            Map<String, Object> body = new HashMap<>();
            body.put(USER_ID_BODY, userId);
            body.put("dvcToken", dvcToken);
            body.put("platform", platform);
            // エンティティセット
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 実行
            return restTemplate.exchange(
                    propertiesUtil.getJsapDvcLinkApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public ResponseEntity<String> executePushRequest(String userId, String payload, String token) {
        // メソッドはPush通知("0")固定
        String method = "0";
        try {
            // テンプレート
            RestTemplate restTemplate = new RestTemplate();
            // ヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapNotificationApiKey());
            headers.set("auth-token", token);
            // ボディ設定
            Map<String, Object> body = new HashMap<>();
            body.put(USER_ID_BODY, userId);
            body.put("noticeMethod", method);
            body.put("payload", payload);
            // エンティティセット
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 実行
            return restTemplate.exchange(
                    propertiesUtil.getJsapNotificationApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public ResponseEntity<String> executeSendMessage(
            String proccessId, String userId, String contactType, String title, Object context,
            String token) {
        try {
            // テンプレート
            RestTemplate restTemplate = new RestTemplate();
            // ヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapNotificationApiKey());
            headers.set("auth-token", token);
            // ボディ設定
            Map<String, Object> body = new HashMap<>();
            body.put("processID", proccessId);
            body.put(USER_ID_BODY, userId);
            body.put("noticeMethod ", contactType);
            body.put("title", title);
            body.put("context", context);
            // エンティティセット
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 実行
            return restTemplate.exchange(
                    propertiesUtil.getJsapNotificationApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public ResponseEntity<String> executeGetUserInfo(String userId) {
        try {
            // テンプレート
            RestTemplate restTemplate = new RestTemplate();
            // ヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapGetUserInfoApiKey());
            // ボディ設定
            Map<String, Object> body = new HashMap<>();
            body.put(USER_ID_BODY, userId);
            // エンティティセット
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 実行
            return restTemplate.exchange(
                    propertiesUtil.getJsapGetUserInfoApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public SmsContextDto createSmsContext(String bodySms) {
        return new SmsContextDto(bodySms);
    }

    public List<MailContextDto> createMailContext(String bodyText, String bodyHtml) {
        MailContextDto text = new MailContextDto("text/plain", bodyText);
        MailContextDto html = new MailContextDto("text/html", bodyHtml);
        List<MailContextDto> mailContext = new ArrayList<>();
        mailContext.add(text);
        mailContext.add(html);
        return mailContext;
    }
}
