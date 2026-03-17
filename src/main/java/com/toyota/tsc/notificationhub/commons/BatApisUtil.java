package com.toyota.tsc.notificationhub.commons;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.RegisterNotificationRequestDto;

// import org.springframework.http.MediaType;

@Component
public class BatApisUtil {

    private PropertiesUtil propertiesUtil;

    public BatApisUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    /**
     * APIを呼び出す
     * 
     * @param request
     * @return
     */
    public ResponseEntity<String> executeRegisterNotification(
            RegisterNotificationRequestDto request) {
        try {
            // テンプレート
            RestTemplate restTemplate = new RestTemplate();
            // エンティティセット
            HttpEntity<RegisterNotificationRequestDto> entity = new HttpEntity<>(request);
            // 実行
            return restTemplate.exchange(
                    propertiesUtil.getRegisterNotificationUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);
        } catch (HttpStatusCodeException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    // @@@@@@@@@@@@@@@@@@@@@@ IT1用Mock @@@@@@@@@@@@@@@@@@@@@@
    // public ResponseEntity<String>
    // executeRegisterNotification(RegisterNotificationRequestDto request) {
    // String dummyJson = """
    // {
    // "returnCode": "000000",
    // "notificationId": "TEST-NOTIF-001",
    // "message": "OK (dummy)"
    // }
    // """;
    // return ResponseEntity
    // .ok()
    // .contentType(MediaType.APPLICATION_JSON)
    // .body(dummyJson);
    // }
}
