package com.nd.jp.notification.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistNotificationDeviceInfoRequestDto {
    private String internalUserId;
    private String platform;
    private String deviceToken;
    private String dvcId;
    private String brdCd;
}
