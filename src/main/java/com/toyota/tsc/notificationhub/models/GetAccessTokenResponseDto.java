package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAccessTokenResponseDto {
    private String access_token;
    private String token_type;
    private String expires_in;
    private String scope;
    private String jti;
}