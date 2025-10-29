package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;

public interface SendPushServiceIF {
    /**
     * Push通知要求サービス
     * 
     * @param request Push通知要求リクエストDTO
     */
    String sendPush(SendPushRequestDto request, RequestHeaderDto header);
}
