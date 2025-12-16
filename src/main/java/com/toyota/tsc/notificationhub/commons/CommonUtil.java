package com.toyota.tsc.notificationhub.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;
import org.springframework.stereotype.Component;

/**
 * 共通ユーティリティクラス
 */
@Component
public class CommonUtil {

    // 外部からのインスタンス化禁制
    private CommonUtil() {
    }

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
        String profile = System.getProperty("spring.profiles.active", "");
        String[] actProfile = Arrays.stream(profile.split("\\s*,\\s*"))
                .filter(s -> !s.isBlank()).map(String::toLowerCase).toArray(String[]::new);
        String useLogResource = Arrays.stream(actProfile).anyMatch("sa"::equals)
                ? "properties.SaLogMessages"
                : "properties.LogMessages";
        ResourceBundle bundleLog = ResourceBundle.getBundle(useLogResource);
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
}
