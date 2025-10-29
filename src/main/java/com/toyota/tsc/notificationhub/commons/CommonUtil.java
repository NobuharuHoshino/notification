package com.toyota.tsc.notificationhub.commons;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CommonUtil {
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
            String head = text.substring(0, Math.min(3, atIdx));
            String domain = text.substring(atIdx);
            return head + "***" + domain;
        } else {
            // 通常文字列は先頭3文字＋***
            return text.length() <= 3 ? text + "***" : text.substring(0, 3) + "***";
        }
    }
}
