package com.nd.jp.notification.service;

import com.nd.jp.notification.models.SendPrimaryContactRequestDto;
import com.nd.jp.notification.models.RequestHeaderDto;

public interface SendPrimaryContactServiceIF {
    /**
     * Primary contactメッセージ送信要求サービス
     * 
     * @param request Primary contactメッセージ送信要求リクエストDTO
     */
    String sendPrimaryContact(SendPrimaryContactRequestDto request, RequestHeaderDto header);
}
