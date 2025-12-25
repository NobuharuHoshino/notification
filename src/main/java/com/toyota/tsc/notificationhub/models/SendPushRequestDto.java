package com.toyota.tsc.notificationhub.models;

import lombok.Data;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendPushRequestDto {
    private String processId;
    private String internalUserId;
    private Map<String, Object> body;
}
