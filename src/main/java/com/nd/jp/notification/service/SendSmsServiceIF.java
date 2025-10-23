package com.nd.jp.notification.service;

import com.nd.jp.notification.models.SendSmsRequestDto;
import com.nd.jp.notification.models.RequestHeaderDto;

public interface SendSmsServiceIF {
    /**
     * SMS送信要求サービス
     * 
     * @param request SMS送信要求リクエストDTO
     */
    String sendSms(SendSmsRequestDto request, RequestHeaderDto header);
}
