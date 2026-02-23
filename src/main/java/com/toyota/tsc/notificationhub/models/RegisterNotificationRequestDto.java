package com.toyota.tsc.notificationhub.models;

import lombok.Data;

import java.util.List;

import com.toyota.tsc.notificationhub.models.SendMessageNotificationRequestDto.NotificationContent;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterNotificationRequestDto {
    private String notificationTarget;
    private String countryCode;
    private String internalUserIdLocal;
    private List<NotificationContent> notificationContents;
    private String notificationType;
}