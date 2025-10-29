package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.models.SendMailRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;

public interface SendMailServiceIF {
    /**
     * メール送信要求サービス
     * 
     * @param request メール送信要求リクエストDTO
     */
    String sendMail(SendMailRequestDto request, RequestHeaderDto header);
}
