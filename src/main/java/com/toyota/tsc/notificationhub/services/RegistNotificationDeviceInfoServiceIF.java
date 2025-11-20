package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;

public interface RegistNotificationDeviceInfoServiceIF {
    /**
     * 通知端末情報登録サービス
     * 
     * @param request 通知端末情報登録リクエストDTO
     * @return 登録結果（必要に応じてレスポンスDTO等を返却）
     */
    ResponseDto registDeviceInfo(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header);
}
