package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;

public interface SendPrimaryContactServiceIF {
    /**
     * Primary contactメッセージ送信要求サービス
     * 
     * @param request Primary contactメッセージ送信要求リクエストDTO
     */
    ResponseDto sendPrimaryContact(SendPrimaryContactRequestDto request, RequestHeaderDto header);
}
