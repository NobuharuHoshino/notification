package com.toyota.tsc.notificationhub.models;

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
    private String bodyText;
    private String bodyHtml;
    private String bodySms;
}
