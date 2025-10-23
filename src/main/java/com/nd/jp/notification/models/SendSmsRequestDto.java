package com.nd.jp.notification.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendSmsRequestDto {
    private String brdCd;
    private String mobileNumber;
    private String body_sms;
}
