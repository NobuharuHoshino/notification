package com.toyota.tsc.notificationhub.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * 共通ユーティリティクラス
 */
public class CommonUtil {

    @Value("${personalinfo.api.url}")
    private static String personalInfoApiUrl;

    private static final String RESOURCE_LOG = "properties.LogMessages";
    private static final ResourceBundle bundleLog = ResourceBundle.getBundle(RESOURCE_LOG);
    private static final String RESOURCE_RESULT = "properties.ResultCode";
    private static final ResourceBundle bundleResult = ResourceBundle.getBundle(RESOURCE_RESULT);

    private CommonUtil() {
    }

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
            throw new RuntimeException(e);
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
            String masked = "********";
            String domain = text.substring(atIdx);
            return head + masked + domain;
        } else {
            return text.length() <= 2 ? text + "********" : text.substring(0, 2) + "********";
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
            return "********";
        String normalized = normalizePhoneNumber(phoneNumber);
        int len = normalized.length();
        String last4 = normalized.substring(len - 4);
        String masked = "";
        for (int i = 0; i < len - 4; i++)
            masked += "*";
        return masked + last4;
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
        return phoneNumber.replaceAll("[^0-9]", "");
    }

    /**
     * 個人情報APIからレスポンスを取得します。
     * 
     * @param internalUserId ユーザーID
     * @return 個人情報レスポンスDTO
     */
    public static PersonalInfoResponseDto getPersonalInfoApiResponse(String internalUserId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", "検証");
        headers.set("号口", "固定キー");
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("connection", "keep-alive");
        headers.set("accept-encoding", "gzip");
        headers.set("x-correlation-id", internalUserId);
        ResponseEntity<String> response = restTemplate.getForEntity(personalInfoApiUrl, String.class, headers);
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(response.getBody(), PersonalInfoResponseDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse personal info response", e);
        }
    }

}
