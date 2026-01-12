package com.toyota.tsc.notificationhub.commons;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.DvcLinkageResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserIdResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserInfoResponseDto;
import com.toyota.tsc.notificationhub.models.MailContextDto;
import com.toyota.tsc.notificationhub.models.PushRequestResponseDto;
import com.toyota.tsc.notificationhub.models.SendMessageResponseDto;
import com.toyota.tsc.notificationhub.models.SmsContextDto;

@Component
public class JsapUtil {

    private PropertiesUtil propertiesUtil;

    public JsapUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    private static final String PLATFORM_ANDROID = "1"; // FCM v1
    private static final String PLATFORM_IOS = "2"; // APNs
    private static final String X_API_KEY_HEADER = "x-api-key";
    private static final String USER_ID_BODY = "userId";
    private static final String AUTH_TOKEN = "authorization";

    public ResponseEntity<String> executeGetToken() {
        try {
            // テンプレート
            RestTemplate restTemplate = new RestTemplate();
            // ヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
            // ボディ設定
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials"); // TODO プロパティ化
            body.add("client_id", "client_id");
            body.add("client_secret", "client_secret");

            // エンティティセット
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            // 実行
            return restTemplate.exchange(
                    "tokenUrl",
                    HttpMethod.POST,
                    entity,
                    String.class);

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    public ResponseEntity<String> executeGetUserId(String internalUserId, String colId) {
        // TODO

        GetUserIdResponseDto dto = new GetUserIdResponseDto();
        dto.setResultCode("00001548B123");
        dto.setUserId("DEV_USER_ID");
        dto.setInternalUserId("DEV_INTUSER_ID");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String body = mapper.writeValueAsString(dto);
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);
        } catch (Exception e) {
            throw new CustomException(e);
        }

        // try {
        // GetUserIdResponseDto dto = new GetUserIdResponseDto();
        // dto.setResultCode("00001548B123ERROR");
        // dto.setInternalUserId(internalUserId);
        // dto.setUserId("任意");
        // ObjectMapper mapper = new ObjectMapper();
        // String json = mapper.writeValueAsString(dto);

        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);

        // return new ResponseEntity<>(json, headers, 400); // ★非2xxで返す
        // } catch (Exception e) {
        // throw new RuntimeException("failed to build GetUserId error response", e);
        // }

        // try {
        // // テンプレート
        // RestTemplate restTemplate = new RestTemplate();
        // // ヘッダー設定
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapGetUserIdApiKey());
        // headers.set("x-correlation-id", colId);
        // // ボディ設定
        // Map<String, Object> body = new HashMap<>();
        // body.put("internalUserId", internalUserId);
        // // エンティティセット
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // // 実行
        // return restTemplate.exchange(
        // propertiesUtil.getJsapGetUserIdApiUrl(),
        // HttpMethod.POST,
        // entity,
        // String.class);

        // } catch (Exception e) {
        // throw new CustomException(e);
        // }
    }

    public ResponseEntity<String> executeDvcLink(String userId, String dvcToken, String platform, String token) {

        // TODO

        DvcLinkageResponseDto dto = new DvcLinkageResponseDto();
        dto.setResultCode("000000");
        dto.setResultMessage("TEST");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String body = mapper.writeValueAsString(dto);

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

        } catch (Exception e) {
            // とりあえず異常系も返す（必要ならログ出し）
            return ResponseEntity
                    .internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"resultCode\":\"999999\",\"resultMessage\":\"JSON_SERIALIZE_ERROR\"}");
        }

        // DvcLinkageResponseDto dto = new DvcLinkageResponseDto();
        // dto.setResultCode("00001548B123");
        // dto.setResultMessage("Mockです");

        // try {
        // ObjectMapper mapper = new ObjectMapper();
        // String body = mapper.writeValueAsString(dto);
        // return ResponseEntity
        // .ok()
        // .contentType(MediaType.APPLICATION_JSON)
        // .body(body);
        // } catch (Exception e) {
        // throw new CustomException(e);
        // }

        // try {
        // // プラットフォーム変換
        // if (PLATFORM_ANDROID.equals(platform)) {
        // platform = "android";
        // } else if (PLATFORM_IOS.equals(platform)) {
        // platform = "ios";
        // } else {
        // return null; // サービスでバリデーションチェックしているため、対応ブランド以外は到達しない想定。
        // }
        // // テンプレート
        // RestTemplate restTemplate = new RestTemplate();
        // // ヘッダー設定
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapDvcLinkApiKey());
        // headers.set(AUTH_TOKEN, "bearer " + token);
        // // ボディ設定
        // Map<String, Object> body = new HashMap<>();
        // body.put(USER_ID_BODY, userId);
        // body.put("dvcToken", dvcToken);
        // body.put("platform", platform);
        // // エンティティセット
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // // 実行
        // return restTemplate.exchange(
        // propertiesUtil.getJsapDvcLinkApiUrl(),
        // HttpMethod.POST,
        // entity,
        // String.class);

        // } catch (Exception e) {
        // throw new CustomException(e);
        // }
    }

    public ResponseEntity<String> executePushRequest(String userId, String payload, String token) {
        // メソッドはPush通知("0")固定
        // String method = "0";
        // try {
        // // テンプレート
        // RestTemplate restTemplate = new RestTemplate();
        // // ヘッダー設定
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapNotificationApiKey());
        // headers.set(AUTH_TOKEN, token);
        // // ボディ設定
        // Map<String, Object> body = new HashMap<>();
        // body.put(USER_ID_BODY, userId);
        // body.put("noticeMethod", method);
        // body.put("payload", payload);
        // // エンティティセット
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // // 実行
        // return restTemplate.exchange(
        // propertiesUtil.getJsapNotificationApiUrl(),
        // HttpMethod.POST,
        // entity,
        // String.class);

        // } catch (Exception e) {
        // throw new CustomException(e);
        // }

        // TODO
        PushRequestResponseDto dto = new PushRequestResponseDto();
        dto.setResultCode("000000eeee");
        dto.setResultMessage("TEST");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String body = mapper.writeValueAsString(dto);

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

        } catch (Exception e) {
            // とりあえず異常系も返す（必要ならログ出し）
            return ResponseEntity
                    .internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"resultCode\":\"999999\",\"resultMessage\":\"JSON_SERIALIZE_ERROR\"}");
        }
    }

    public ResponseEntity<String> executeSendMessage(
            String proccessId, String userId, String contactType, String title, Object context,
            String token) {
        // try {
        // // テンプレート
        // RestTemplate restTemplate = new RestTemplate();
        // // ヘッダー設定
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapNotificationApiKey());
        // headers.set(AUTH_TOKEN, token);
        // // ボディ設定
        // Map<String, Object> body = new HashMap<>();
        // body.put("processID", proccessId);
        // body.put(USER_ID_BODY, userId);
        // body.put("noticeMethod ", contactType);
        // body.put("title", title);
        // body.put("context", context);
        // // エンティティセット
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // // 実行
        // return restTemplate.exchange(
        // propertiesUtil.getJsapNotificationApiUrl(),
        // HttpMethod.POST,
        // entity,
        // String.class);

        // } catch (Exception e) {
        // throw new CustomException(e);
        // }

        SendMessageResponseDto dto = new SendMessageResponseDto();
        dto.setResultCode("000000");
        dto.setResultMessage("TEST");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String body = mapper.writeValueAsString(dto);

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

        } catch (Exception e) {
            // とりあえず異常系も返す（必要ならログ出し）
            return ResponseEntity
                    .internalServerError()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"resultCode\":\"999999\",\"resultMessage\":\"JSON_SERIALIZE_ERROR\"}");
        }
    }

    public ResponseEntity<String> executeGetUserInfo(String userId) {
        // try {
        // // テンプレート
        // RestTemplate restTemplate = new RestTemplate();
        // // ヘッダー設定
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set(X_API_KEY_HEADER, propertiesUtil.getJsapGetUserInfoApiKey());
        // // ボディ設定
        // Map<String, Object> body = new HashMap<>();
        // body.put(USER_ID_BODY, userId);
        // // エンティティセット
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // // 実行
        // return restTemplate.exchange(
        // propertiesUtil.getJsapGetUserInfoApiUrl(),
        // HttpMethod.POST,
        // entity,
        // String.class);

        // } catch (Exception e) {
        // throw new CustomException(e);
        // }

        // 1) DTOを作って任意の値を詰める
        GetUserInfoResponseDto dto = new GetUserInfoResponseDto();
        dto.setUserId(userId); // 引数をそのまま使うとテストで便利
        dto.setCountry("JP");
        dto.setFirstName("Taro");
        dto.setLastName("Yamada");
        dto.setDateOfBirth("1990-01-01"); // 文字列仕様に合わせる

        GetUserInfoResponseDto.ContactDto contact = new GetUserInfoResponseDto.ContactDto();
        contact.setContactType("1");
        contact.setContact("taro.yamada@example.com");
        contact.setPrimaryContactFlag(true);

        GetUserInfoResponseDto.ContactDto contact2 = new GetUserInfoResponseDto.ContactDto();
        contact2.setContactType("0");
        contact2.setContact("818067582835");
        contact2.setPrimaryContactFlag(true);

        List<GetUserInfoResponseDto.ContactDto> contactList = new ArrayList<>();
        contactList.add(contact); // 1件入ってればOKの条件を満たす
        contactList.add(contact2);
        dto.setContactList(contactList);

        // 2) JSON文字列化して200 OKで返す
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(dto);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            // モック用途なら RuntimeException で落としてもOKだが、
            // もし安全側に倒すなら500返却などに変えてもよい
            throw new IllegalStateException("Failed to serialize GetUserInfoResponseDto to JSON", e);
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
