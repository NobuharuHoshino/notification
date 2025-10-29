package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.models.SendSmsRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;

public interface SendSmsServiceIF {
    /**
     * SMS送信要求サービス
     * 
     * @param request SMS送信要求リクエストDTO
     */
    String sendSms(SendSmsRequestDto request, RequestHeaderDto header);
}
