package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestHeaderDto {
    private String apiKey; // x-api-key
    private String contentType; // content-type
    private String connection; // connection (keep-alive/close)
    private String acceptEncoding; // accept-encoding
    private String correlationId; // x-correlation-id
    private String userAccessKey; // user-access-key
    private String xSmartgbook; // x-smartgbook
}
