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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.PersonalInfoListRequestDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto;
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
            // テンプレート
            RestTemplate restTemplate = new RestTemplate();
            // ヘッダー設定
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
            headers.set(HttpHeaders.CONNECTION, "keep-alive");
            headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip");
            headers.set("x-api-key", propertiesUtil.getPersonalInfoApiKey());
            headers.set("x-correlation-id", colId);
            // ボディ設定
            Map<String, Object> body = new HashMap<>();
            body.put("internalUserId", internalUserId);
            // エンティティセット
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            // 実行
            ResponseEntity<String> response = restTemplate.exchange(
                    propertiesUtil.getPersonalInfoApiUrl(),
                    HttpMethod.POST,
                    entity,
                    String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new CustomException("An error occurred while retrieving personal information. Status:"
                        + response.getStatusCode().value());
            }

            // レスポンスマッピング
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getBody(), PersonalInfoResponseDto.class);
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    /**
     * 販売店申し込み個人情報検索APIからレスポンスを取得します。
     */
    // TODO IT1用Mock
    // public ResponseEntity<String>
    // getPersonalInfoListApiResponse(PersonalInfoListRequestDto internalUserIdList,
    // String colId) {
    // try {
    // // テンプレート
    // RestTemplate restTemplate = new RestTemplate();
    // // ヘッダー設定
    // HttpHeaders headers = new HttpHeaders();
    // headers.set(HttpHeaders.CONTENT_TYPE, "application/json");
    // headers.set(HttpHeaders.CONNECTION, "keep-alive");
    // headers.set(HttpHeaders.ACCEPT_ENCODING, "gzip");
    // headers.set("x-api-key", propertiesUtil.getPersonalInfoListApiKey());
    // headers.set("x-correlation-id", colId);
    // // エンティティセット
    // HttpEntity<PersonalInfoListRequestDto> entity = new
    // HttpEntity<>(internalUserIdList, headers);

    // // 実行
    // return restTemplate.exchange(
    // propertiesUtil.getPersonalInfoListApiUrl(),
    // HttpMethod.POST,
    // entity,
    // String.class);

    // } catch (Exception e) {
    // throw new CustomException(e);
    // }
    // }

    public ResponseEntity<String> getPersonalInfoListApiResponse(
            PersonalInfoListRequestDto internalUserIdList,
            String colId) {

        try {
            // ★テスト用固定レスポンスにしたい場合はここで return
            ObjectMapper mapper = new ObjectMapper();

            PersonalInfoListResponseDto dummy = createDummyPersonalInfoListResponse(
                    internalUserIdList.getInternalUserIdList());
            String body = mapper.writeValueAsString(dummy);

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    private PersonalInfoListResponseDto createDummyPersonalInfoListResponse(
            List<String> internalUserIdList) {
        PersonalInfoListResponseDto res = new PersonalInfoListResponseDto();
        res.setResultCode("00001581U000");

        List<PersonalInfoListResponseDto.PersonalInfoList> list = new ArrayList<>();

        for (int i = 0; i < internalUserIdList.size(); i++) {
            PersonalInfoListResponseDto.PersonalInfoList pi = new PersonalInfoListResponseDto.PersonalInfoList();
            pi.setInternalUserId(internalUserIdList.get(i));
            pi.setUserId(String.format("user-%03d", i + 1));
            pi.setFirstName("太郎" + (i + 1));
            pi.setLastName("山田");
            pi.setBirthday("1990-01-01"); // 適当でOK
            pi.setMemberStatus("ACTIVE"); // 適当でOK

            // contactList 4件（1件だけ primaryContactFlag=true）
            List<PersonalInfoListResponseDto.ContactDto> contacts = new ArrayList<>();
            for (int j = 1; j <= 4; j++) {
                PersonalInfoListResponseDto.ContactDto c = new PersonalInfoListResponseDto.ContactDto();
                c.setDisplayOrder(j);

                // 例：種別を変える（全部同じでもOK）
                if (j == 1) {
                    c.setContactType("2");
                    c.setContact(String.format("user%03d@example.com", i + 1));
                } else if (j == 2) {
                    c.setContactType("1");
                    c.setContact(String.format("090-0000-%04d", i + 1));
                } else if (j == 3) {
                    c.setContactType("2");
                    c.setContact(String.format("line_id_%03d", i + 1));
                } else {
                    c.setContactType("3");
                    c.setContact(String.format("other_%03d", i + 1));
                }

                c.setPrimaryContactFlag(j == 2); // ★2件目だけtrue
                contacts.add(c);
            }

            pi.setContactList(contacts);
            list.add(pi);
        }

        res.setPersonalInfoList(list);
        return res;
    }
}
