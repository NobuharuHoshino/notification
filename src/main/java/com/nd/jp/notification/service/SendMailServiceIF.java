package com.nd.jp.notification.service;

import com.nd.jp.notification.models.SendMailRequestDto;
import com.nd.jp.notification.models.RequestHeaderDto;

public interface SendMailServiceIF {
    /**
     * メール送信要求サービス
     * 
     * @param request メール送信要求リクエストDTO
     */
    String sendMail(SendMailRequestDto request, RequestHeaderDto header);
}
