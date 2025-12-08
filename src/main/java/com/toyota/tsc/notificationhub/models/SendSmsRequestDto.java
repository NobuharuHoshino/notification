package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendSmsRequestDto {
    private String brdCd;
    private String mobileNumber;
    private String bodySms;
}
