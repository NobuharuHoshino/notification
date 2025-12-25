package com.toyota.tsc.notificationhub.commons;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;

@Component
public class PersonalInfoUtil {

    private PropertiesUtil propertiesUtil;

    public PersonalInfoUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    /**
     * 個人情報APIからレスポンスを取得します。
     * 
     * @param internalUserId ユーザーID
     * @return 個人情報レスポンスDTO
     */
    public PersonalInfoResponseDto getPersonalInfoApiResponse(String internalUserId, String colId) {
        try {
            PersonalInfoResponseDto dto = new PersonalInfoResponseDto();
            dto.setResultCode("0000");
            PersonalInfoResponseDto.ContactDto con = new PersonalInfoResponseDto.ContactDto();
            con.setDisplayOrder(1);
            con.setContactType("1");
            con.setContact("XXX");
            con.setPrimaryContactFlag(true);
            dto.setContactList(new java.util.ArrayList<PersonalInfoResponseDto.ContactDto>() {
                {
                    add(con);
                }
            });
            return dto;

            // // テンプレート
            // RestTemplate restTemplate = new RestTemplate();
            // // ヘッダー設定
            // HttpHeaders headers = new HttpHeaders();
            // headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
            // headers.set(HttpHeaders.CONNECTION, "keep-alive");
            // headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip");
            // headers.set("x-api-key", propertiesUtil.getPersonalInfoApiKey());
            // headers.set("x-correlation-id", colId);
            // // ボディ設定
            // Map<String, Object> body = new HashMap<>();
            // body.put("internalUserId", internalUserId);
            // // エンティティセット
            // HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // // 実行
            // ResponseEntity<String> response = restTemplate.exchange(
            // propertiesUtil.getPersonalInfoApiUrl(),
            // HttpMethod.POST,
            // entity,
            // String.class);
            // if (!response.getStatusCode().is2xxSuccessful()) {
            // throw new CustomException("An error occurred
            // whileretrievingpersonalinformation. Status:"
            // + response.getStatusCode().value());
            // }

            // // レスポンスマッピング
            // ObjectMapper mapper = new ObjectMapper();
            // return mapper.readValue(response.getBody(), PersonalInfoResponseDto.class);
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }
}
