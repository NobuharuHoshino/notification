package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;

public interface RegistNotificationDeviceInfoServiceIF {
    /**
     * 通知端末情報登録サービス
     * 
     * @param request 通知端末情報登録リクエストDTO
     * @return 登録結果（必要に応じてレスポンスDTO等を返却）
     */
    String registDeviceInfo(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header);
}
