package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageResponseDto {
    private String resultCode;
    private String resultMessage;
}