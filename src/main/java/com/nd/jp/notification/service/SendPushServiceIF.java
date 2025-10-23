package com.nd.jp.notification.service;

import com.nd.jp.notification.models.SendPushRequestDto;
import com.nd.jp.notification.models.RequestHeaderDto;

public interface SendPushServiceIF {
    /**
     * Push通知要求サービス
     * 
     * @param request Push通知要求リクエストDTO
     */
    String sendPush(SendPushRequestDto request, RequestHeaderDto header);
}
