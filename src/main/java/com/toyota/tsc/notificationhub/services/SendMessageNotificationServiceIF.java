package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendMessageNotificationRequestDto;

public interface SendMessageNotificationServiceIF {
    /**
     * お知らせ通知送信サービス
     * 
     * @param request お知らせ通知要求リクエストDTO
     */
    ResponseDto sendMessageNotification(SendMessageNotificationRequestDto request, RequestHeaderDto header);
}
