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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import org.springframework.web.client.HttpStatusCodeException;
import com.toyota.tsc.notificationhub.models.MailContextDto;
import com.toyota.tsc.notificationhub.models.SmsContextDto;
import jp.toyota.res.common.auth.GetALJTokenResultDto;
import jp.toyota.res.common.utils.ALJToken;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.models.DvcLinkageResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserIdResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserInfoResponseDto;
import com.toyota.tsc.notificationhub.models.PushRequestResponseDto;
import com.toyota.tsc.notificationhub.models.SendMessageResponseDto;

@Component
public class JsapUtil {

    private PropertiesUtil propertiesUtil;
    private ALJToken aljToken;

    public JsapUtil(PropertiesUtil propertiesUtil, ALJToken aljToken) {
        this.propertiesUtil = propertiesUtil;
        this.aljToken = aljToken;
    }

    private static final String PLATFORM_ANDROID = "1"; // FCM v1
    private static final String PLATFORM_IOS = "2"; // APNs
    private static final String USER_ID_BODY = "userId";
    private static final String AUTH_TOKEN = "authorization";

    public GetALJTokenResultDto executeGetToken() {

        // try {
        // GetALJTokenResultDto tokenResult = aljToken.getALJToken();
        // boolean result = tokenResult.getResult();
        // if (!result) {
        // aljToken.requestUpdateALJToken();
        // }
        // return tokenResult;
        // } catch (Exception e) {
        // throw new CustomException(e);
        // }
        // @@@@@@@@@@@@@@@@@@@@@@@@@@ TEST MOCK @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        GetALJTokenResultDto dto = new GetALJTokenResultDto();
        dto.setResult(true);
        dto.setAljToken("TEST_ALJ_TOKEN");
        return dto;
    }

    public ResponseEntity<String> executeGetUserId(String internalUserId, String colId) {

        // try {
        // // テンプレート
        // RestTemplate restTemplate = new RestTemplate();
        // // ヘッダー設定
        // HttpHeaders headers = new HttpHeaders();
        // headers.set("x-correlation-id", colId);
        // // ボディ設定
        // Map<String, Object> body = new HashMap<>();
        // body.put("internaluserId", internalUserId);
        // // エンティティセット
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        // // 実行
        // return restTemplate.exchange(
        // propertiesUtil.getJsapGetUserIdApiUrl(),
        // HttpMethod.POST,
        // entity,
        // String.class);

        // } catch (HttpStatusCodeException e) {
        // throw e;
        // } catch (Exception e) {
        // throw new CustomException(e);
        // }
        // @@@@@@@@@@@@@@@@@@@@@@@@@@ TEST MOCK @@@@@@@@@@@@@@@@@@@@@@@@@@@@
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
    }

    public ResponseEntity<String> executeDvcLink(String userId, String dvcToken, String platform, String token) {

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
        // headers.set(AUTH_TOKEN, "bearer " + token);
        // // ボディ設定
        // Map<String, Object> body = new HashMap<>();
        // body.put(USER_ID_BODY, userId);
        // body.put("deviceToken", dvcToken);
        // body.put("osType", platform);
        // // エンティティセット
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // // 実行
        // return restTemplate.exchange(
        // propertiesUtil.getJsapDvcLinkApiUrl(),
        // HttpMethod.POST,
        // entity,
        // String.class);

        // } catch (HttpStatusCodeException e) {
        // throw e;
        // } catch (Exception e) {
        // throw new CustomException(e);
        // }

        // @@@@@@@@@@@@@@@@@@@@@@@@@@ TEST MOCK @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        DvcLinkageResponseDto dto = new DvcLinkageResponseDto();
        dto.setResultCode("000000");
        dto.setResultMessage("Mockです");

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
    }

    public ResponseEntity<String> executePushRequest(String userId, String payload, String token) {

        // // メソッドはPush通知("0")固定
        // String method = "0";
        // try {
        // // テンプレート
        // RestTemplate restTemplate = new RestTemplate();
        // // ヘッダー設定
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set(AUTH_TOKEN, "bearer " + token);
        // // ボディ設定
        // Map<String, Object> body = new HashMap<>();
        // body.put(USER_ID_BODY, userId);
        // body.put("sendMethod", method);
        // body.put("payload", payload);
        // // エンティティセット
        // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        // // 実行
        // return restTemplate.exchange(
        // propertiesUtil.getJsapNotificationApiUrl(),
        // HttpMethod.POST,
        // entity,
        // String.class);

        // } catch (HttpStatusCodeException e) {
        // throw e;
        // } catch (Exception e) {
        // throw new CustomException(e);
        // }
        // @@@@@@@@@@@@@@@@@@@@@@@@@@ TEST MOCK @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        PushRequestResponseDto dto = new PushRequestResponseDto();
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

    public ResponseEntity<String> executeSendMessage(
            String proccessId, String userId, String contactType, String title, Object context, String token) {

        // try {
        // // テンプレート
        // RestTemplate restTemplate = new RestTemplate();
        // // ヘッダー設定
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set(AUTH_TOKEN, "bearer " + token);
        // // ボディ設定
        // Map<String, Object> body = new HashMap<>();
        // body.put("processID", proccessId);
        // body.put(USER_ID_BODY, userId);
        // body.put("sendMethod ", contactType);
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

        // } catch (HttpStatusCodeException e) {
        // throw e;
        // } catch (Exception e) {
        // throw new CustomException(e);
        // }

        // @@@@@@@@@@@@@@@@@@@@@@@@@@ TEST MOCK @@@@@@@@@@@@@@@@@@@@@@@@@@@@
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

    public ResponseEntity<String> executeGetUserInfo(String userId, String token) {

        // try {
        // // テンプレート
        // RestTemplate restTemplate = new RestTemplate();
        // // ヘッダー設定
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set(AUTH_TOKEN, "bearer " + token);
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

        // } catch (HttpStatusCodeException e) {
        // throw e;
        // } catch (Exception e) {
        // throw new CustomException(e);
        // }

        // @@@@@@@@@@@@@@@@@@@@@@@@@@ TEST MOCK @@@@@@@@@@@@@@@@@@@@@@@@@@@@
        GetUserInfoResponseDto dto = new GetUserInfoResponseDto();
        dto.setUserId(userId); // 引数をそのまま使うとテストで便利
        dto.setCountry("JP");
        dto.setFirstName("Taro");
        dto.setLastName("Yamada");
        dto.setDateOfBirth("1990-01-01"); // 文字列仕様に合わせる
        GetUserInfoResponseDto.ContactDto contact = new GetUserInfoResponseDto.ContactDto();
        contact.setContactType("2");
        contact.setContact("taro.yamada@example.com");
        contact.setPrimaryContactFlag(true);
        GetUserInfoResponseDto.ContactDto contact2 = new GetUserInfoResponseDto.ContactDto();
        contact2.setContactType("1");
        contact2.setContact("818067582835");
        contact2.setPrimaryContactFlag(false);
        List<GetUserInfoResponseDto.ContactDto> contactList = new ArrayList<>();
        contactList.add(contact);
        contactList.add(contact2);
        dto.setContactList(contactList);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(dto);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
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
