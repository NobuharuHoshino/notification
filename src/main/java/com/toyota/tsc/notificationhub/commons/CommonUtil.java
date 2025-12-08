package com.toyota.tsc.notificationhub.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 共通ユーティリティクラス
 */
@Component
public class CommonUtil {

    // 外部からのインスタンス化禁制
    private CommonUtil() {
    }

    private static final String RESOURCE_LOG = "properties.LogMessages";
    private static final ResourceBundle bundleLog = ResourceBundle.getBundle(RESOURCE_LOG);
    private static final String RESOURCE_RESULT = "properties.ResultCode";
    private static final ResourceBundle bundleResult = ResourceBundle.getBundle(RESOURCE_RESULT);
    private static final String MASKED_STRING = "********";

    /**
     * オブジェクトをJSON文字列に変換します。
     * 
     * @param dto 変換対象オブジェクト
     * @return JSON文字列
     */
    public static String toJson(Object dto) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    /**
     * ログメッセージを取得します。
     * 
     * @param id     メッセージID
     * @param params パラメータ
     * @return フォーマット済みメッセージ
     */
    public static String getMessage(String id, Object... params) {
        String pattern = bundleLog.getString(id);
        return MessageFormat.format(pattern, params);
    }

    /**
     * 結果コードを取得します。
     * 
     * @param code 結果コードキー
     * @return 結果コード値
     */
    public static String getResultCode(String code) {
        return bundleResult.getString(code);
    }

    /**
     * テキスト（メール等）をマスクします。
     * 
     * @param text マスク対象テキスト
     * @return マスク済みテキスト
     */
    public static String maskText(String text) {
        if (text == null || text.isEmpty())
            return "";
        int atIdx = text.indexOf('@');
        if (atIdx > 0) {
            String head = text.substring(0, Math.min(2, atIdx));
            String domain = text.substring(atIdx);
            return head + MASKED_STRING + domain;
        } else {
            return text.length() <= 2 ? text + MASKED_STRING : text.substring(0, 2) + MASKED_STRING;
        }
    }

    /**
     * 電話番号をマスクします。
     * 
     * @param phoneNumber マスク対象電話番号
     * @return マスク済み電話番号
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4)
            return MASKED_STRING;
        String normalized = normalizePhoneNumber(phoneNumber);
        int len = normalized.length();
        String last4 = normalized.substring(len - 4);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len - 4; i++)
            sb.append("*");
        return sb.toString() + last4;
    }

    /**
     * 電話番号を正規化します。
     * 
     * @param phoneNumber 正規化対象電話番号
     * @return 数字のみの電話番号文字列
     */
    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null)
            return "";
        return phoneNumber.replaceAll("\\D", "");
    }

    /**
     * 個人情報APIからレスポンスを取得します。
     * 
     * @param internalUserId ユーザーID
     * @return 個人情報レスポンスDTO
     */
    public static PersonalInfoResponseDto getPersonalInfoApiResponse(String internalUserId) {
        // TODO
        PersonalInfoResponseDto dto = new PersonalInfoResponseDto();
        dto.setResultCode("1");
        dto.setInternalUserId("TEST_USER");
        dto.setUserId("USER001"); // 任意
        dto.setFirstName("太郎"); // 任意
        dto.setLastName("山田"); // 任意
        dto.setBirthday("1990-01-01"); // 任意
        dto.setMemberStatus("ACTIVE"); // 任意
        List<PersonalInfoResponseDto.ContactDto> contactList = new ArrayList<>();
        PersonalInfoResponseDto.ContactDto contact1 = new PersonalInfoResponseDto.ContactDto();
        contact1.setDisplayOrder(1);
        contact1.setContactType("1");
        contact1.setContact("818067582835");
        contact1.setPrimaryContactFlag(true);
        contactList.add(contact1);
        PersonalInfoResponseDto.ContactDto contact2 = new PersonalInfoResponseDto.ContactDto();
        contact2.setDisplayOrder(2);
        contact2.setContactType("1");
        contact2.setContact("818067582835");
        contact2.setPrimaryContactFlag(false);
        contactList.add(contact2);
        PersonalInfoResponseDto.ContactDto contact3 = new PersonalInfoResponseDto.ContactDto();
        contact3.setDisplayOrder(3);
        contact3.setContactType("2");
        contact3.setContact("nobuharu.hoshino.bp@jp.nttdata.com");
        contact3.setPrimaryContactFlag(true);
        contactList.add(contact3);
        PersonalInfoResponseDto.ContactDto contact4 = new PersonalInfoResponseDto.ContactDto();
        contact4.setDisplayOrder(4);
        contact4.setContactType("2");
        contact4.setContact("nobuharu.hoshino.bp@jp.nttdata.com");
        contact4.setPrimaryContactFlag(false);
        contactList.add(contact4);

        dto.setContactList(contactList);
        return dto;
        // RestTemplate restTemplate = new RestTemplate();
        // HttpHeaders headers = new HttpHeaders();
        // headers.set("x-api-key", "検証");
        // headers.set("号口", "固定キー");
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // headers.set("connection", "keep-alive");
        // headers.set("accept-encoding", "gzip");
        // headers.set("x-correlation-id", internalUserId);
        // PropertiesUtil propertiesUtil = new PropertiesUtil();
        // ResponseEntity<String> response =
        // restTemplate.getForEntity(propertiesUtil.getPersonalInfoApiUrl(),
        // String.class, headers);
        // try {
        // ObjectMapper mapper = new ObjectMapper();
        // return mapper.readValue(response.getBody(), PersonalInfoResponseDto.class);
        // } catch (Exception e) {
        // throw new CustomException(e);
        // }
    }
}
