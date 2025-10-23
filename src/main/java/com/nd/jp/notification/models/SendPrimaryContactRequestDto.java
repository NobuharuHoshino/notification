package com.nd.jp.notification.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendPrimaryContactRequestDto {
    private String processId;
    private String internalUserId;
    private String brdCd;
    private String title;
    private String body_text;
    private String body_html;
    private String body_sms;
}
