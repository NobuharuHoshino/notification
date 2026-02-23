package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageNotificationRequestDto {
    private Integer registrationSerialNumber;
    private List<NotificationContent> notificationContents;
    private String notificationType;
    private String isPushNotificationRequired;
    private String title;
    private String bodyText;
    private String bodyHtml;
    private String bodySms;
    private String payload;
    private String scheduledSendData;
    private String expirationDate;

    @Data
    public static class NotificationContent {
        private String languageCode;
        private String title;
        private String dlrMsgDatFmt;
        private String detail;
        private String dlrSetUri;
        private String dlrCntUrl;
    }
}
