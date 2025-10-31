package com.toyota.tsc.notificationhub.commons;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CommonUtil {

    @Value("${personalinfo.api.url}")
    private static String personalInfoApiUrl;

    private static final String RESOURCE_LOG = "properties.LogMessages";
    private static final ResourceBundle bundleLog = ResourceBundle.getBundle(RESOURCE_LOG);
    private static final String RESOURCE_RESULT = "properties.ResultCode";
    private static final ResourceBundle bundleResult = ResourceBundle.getBundle(RESOURCE_RESULT);

    private CommonUtil() {
    }

    public static String toJson(Object dto) {
        if (dto == null)
            return "null";
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getMessage(String id, Object... params) {
        String pattern = bundleLog.getString(id);
        return MessageFormat.format(pattern, params);
    }

    public static String getResultCode(String code) {
        return bundleResult.getString(code);
    }

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
            // 通常文字列は先頭2文字＋********
            return text.length() <= 2 ? text + "********" : text.substring(0, 2) + "********";
        }
    }

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

    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null)
            return "";
        return phoneNumber.replaceAll("[^0-9]", "");
    }

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
