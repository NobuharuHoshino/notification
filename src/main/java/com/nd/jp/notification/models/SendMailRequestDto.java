package com.nd.jp.notification.models;

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
    private String body_text;
    private String body_html;
}
