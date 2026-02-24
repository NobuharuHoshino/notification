package com.toyota.tsc.notificationhub.commons;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.RegisterNotificationRequestDto;

@Component
public class BatApisUtil {

    private PropertiesUtil propertiesUtil;

    public BatApisUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    private static final String X_API_KEY_HEADER = "x-api-key";

    // TODO IT1用Mock
    // public ResponseEntity<String> executeRegisterNotification(
    // RegisterNotificationRequestDto request) {

    // try {
    // // テンプレート
    // RestTemplate restTemplate = new RestTemplate();
    // // ヘッダー設定
    // HttpHeaders headers = new HttpHeaders();
    // headers.set(X_API_KEY_HEADER,
    // propertiesUtil.getRegisterNotificationApiKey());
    // // エンティティセット
    // HttpEntity<RegisterNotificationRequestDto> entity = new HttpEntity<>(request,
    // headers);
    // // 実行
    // return restTemplate.exchange(
    // propertiesUtil.getRegisterNotificationUrl(),
    // HttpMethod.POST,
    // entity,
    // String.class);

    // } catch (Exception e) {
    // throw new CustomException(e);
    // }
    // }

    public ResponseEntity<String> executeRegisterNotification(RegisterNotificationRequestDto request) {
        String dummyJson = """
                {
                  "returnCode": "000000",
                  "notificationId": "TEST-NOTIF-001",
                  "message": "OK (dummy)"
                }
                """;

        return ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(dummyJson);

    }
}
