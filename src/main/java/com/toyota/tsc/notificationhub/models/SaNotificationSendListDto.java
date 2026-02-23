package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaNotificationSendListDto {
    private String vin;
    private String internalUserId;
    private String licenseCode;
    private String brdCd;
    private String userId;
}