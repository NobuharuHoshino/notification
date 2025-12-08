package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMailRequestDto {
    private String brdCd;
    private String emailAddress;
    private String title;
    private String bodyText;
    private String bodyHtml;
}
