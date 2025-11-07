package com.toyota.tsc.notificationhub.commons;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.toyota.tsc.notificationhub.exceptions.TscSMSException;

import org.springframework.beans.factory.annotation.Value;

@Component
public class SmsCountryUtil {

    @Value("${sms-country.user}")
    private String smsCountryUser;
    @Value("${sms-country.pass}")
    private String smsCountryPass;
    @Value("${sms-country.sender-id-t}")
    private String senderIdToyota;
    @Value("${sms-country.sender-id-l}")
    private String senderIdLexus;
    @Value("${sms-country.api-url}")
    private String smsCountryApiUrl;

    private final String BRD_TOYOTA = "1";
    private final String BRD_LEXUS = "2";

    public HttpEntity<String> createRequest(String to, String message, String brdCd) {

        String senderId;
        try {
            if (BRD_TOYOTA.equals(brdCd)) {
                senderId = senderIdToyota;
            } else if (BRD_LEXUS.equals(brdCd)) {
                senderId = senderIdLexus;
            } else {
                return null;
            }

            String encodedUser = URLEncoder.encode(smsCountryUser, "UTF-8");
            String hexMessage = toHexUtf16BE(message);
            String payload = "User=" + encodedUser +
                    "&passwd=" + smsCountryPass +
                    "&mobilenumber=" + to +
                    "&message=" + hexMessage +
                    "&SenderID=" + senderId +
                    "&Mtype=OL&DR=N";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            return entity;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendSmsCountry(HttpEntity<String> entity, String phoneNo) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.postForEntity(smsCountryApiUrl, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new TscSMSException(response.getStatusCode().value(), response.getBody(), phoneNo);
            }
        } catch (TscSMSException scEx) {
            throw new TscSMSException(scEx.getStatusCode(), scEx.getResponseBody(), scEx.getPhoneNo());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHexUtf16BE(String text) {
        byte[] utf16 = text.getBytes(StandardCharsets.UTF_16BE);
        StringBuilder sb = new StringBuilder(utf16.length * 2);
        for (byte b : utf16) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

}
