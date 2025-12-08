package com.toyota.tsc.notificationhub.commons;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;

/**
 * SMS送信（SmsCountry）ユーティリティクラス
 */
@Component
public class SmsCountryUtil {

    private static final String BRD_INVALID = "0";
    private static final String BRD_TOYOTA = "1";
    private static final String BRD_LEXUS = "2";

    private PropertiesUtil propertiesUtil;

    public SmsCountryUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    /**
     * SMS送信リクエストを生成します。
     * 
     * @param to      送信先電話番号
     * @param message メッセージ本文
     * @param brdCd   ブランドコード
     * @return HttpEntity（送信リクエスト）
     */
    public HttpEntity<String> createRequest(String to, String message, String brdCd) {

        String senderId;
        try {
            if (BRD_TOYOTA.equals(brdCd) || brdCd.equals(BRD_INVALID)) {
                senderId = propertiesUtil.getSenderIdToyota();
            } else if (BRD_LEXUS.equals(brdCd)) {
                senderId = propertiesUtil.getSenderIdLexus();
            } else {
                return null;
            }

            String encodedUser = URLEncoder.encode(propertiesUtil.getSmsCountryUser(), "UTF-8");
            String hexMessage = toHexUtf16BE(message);
            String payload = "User=" + encodedUser +
                    "&passwd=" + propertiesUtil.getSmsCountryPass() +
                    "&mobilenumber=" + to +
                    "&message=" + hexMessage +
                    "&sid=" + senderId +
                    "&Mtype=OL&DR=N";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            return new HttpEntity<>(payload, headers);

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    /**
     * SmsCountry APIでSMS送信を実行します。
     * 
     * @param entity  送信リクエスト
     * @param phoneNo 送信先電話番号
     * @return なし
     */
    public void sendSmsCountry(HttpEntity<String> entity, String phoneNo) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(propertiesUtil.getSmsCountryApiUrl(), entity,
                    String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new TscSMSException(response.getStatusCode().value(),
                        response.getBody(), phoneNo);
            }
        } catch (TscSMSException scEx) {
            throw new TscSMSException(scEx.getStatusCode(), scEx.getResponseBody(),
                    scEx.getPhoneNo());
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    /**
     * UTF-16BEでエンコードし16進文字列へ変換します。
     * 
     * @param text 変換対象文字列
     * @return 16進数文字列
     */
    private static String toHexUtf16BE(String text) {
        byte[] utf16 = text.getBytes(StandardCharsets.UTF_16BE);
        StringBuilder sb = new StringBuilder(utf16.length * 2);
        for (byte b : utf16) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}