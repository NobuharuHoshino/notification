package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterNotificationRequestDto {
    private NotificationTarget notificationTarget;
    private List<NotificationContent> notificationContents;
    private String notificationType;

    @Data
    @AllArgsConstructor
    public static class NotificationTarget {
        private String vin;
        private String userType;
        private String country;
        private String internalUserIdLocal;
    }

    @Data
    @AllArgsConstructor
    public static class NotificationContent {
        private String languageCode;
        private String title;
        private String dlrMsgDatFmt;
        private String detail;
        private String dlrSetUri;
        private String dlrCntUrl;
    }
}