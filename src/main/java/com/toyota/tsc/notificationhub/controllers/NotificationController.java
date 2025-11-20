package com.toyota.tsc.notificationhub.controllers;

import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.services.RegistNotificationDeviceInfoServiceIF;
import com.toyota.tsc.notificationhub.services.SendPrimaryContactServiceIF;
import com.toyota.tsc.notificationhub.services.SendPushServiceIF;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 通知サービスAPIコントローラー
 */
@RestController
@RequestMapping("/v1/dcm/service-list")
@RequiredArgsConstructor
public class NotificationController {
        private final RegistNotificationDeviceInfoServiceIF registService;
        private final SendPushServiceIF pushService;
        private final SendPrimaryContactServiceIF primaryContactService;

        /**
         * デバイス情報登録API
         * 
         * @param apiKey         APIキー
         * @param contentType    Content-Type
         * @param connection     Connection
         * @param acceptEncoding Accept-Encoding
         * @param correlationId  コリレーションID
         * @param userAccessKey  ユーザーアクセスキー
         * @param xSmartgbook    x-smartgbook
         * @param body           リクエストボディ
         * @return 登録結果コード
         */
        @PostMapping("/registNotificationDeviceInfo")
        public ResponseEntity<ResponseDto> registNotificationDeviceInfo(
                        @RequestHeader(value = "x-api-key", required = false) String apiKey,
                        @RequestHeader(value = "content-type", required = true) String contentType,
                        @RequestHeader(value = "connection", required = false) String connection,
                        @RequestHeader(value = "accept-encoding", required = false) String acceptEncoding,
                        @RequestHeader(value = "x-correlation-id", required = true) String correlationId,
                        @RequestHeader(value = "user-access-key", required = false) String userAccessKey,
                        @RequestHeader(value = "x-smartgbook", required = false) String xSmartgbook,
                        @RequestBody RegistNotificationDeviceInfoRequestDto body) {
                RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding,
                                correlationId, userAccessKey, xSmartgbook);
                ResponseDto resultCode = registService.registDeviceInfo(body, header);
                return ResponseEntity.ok(resultCode);
        }

        /**
         * プッシュ通知送信API
         * 
         * @param apiKey         APIキー
         * @param contentType    Content-Type
         * @param connection     Connection
         * @param acceptEncoding Accept-Encoding
         * @param correlationId  コリレーションID
         * @param userAccessKey  ユーザーアクセスキー
         * @param xSmartgbook    x-smartgbook
         * @param body           リクエストボディ
         * @return 送信結果コード
         */
        @PostMapping("/sendPush")
        public ResponseEntity<ResponseDto> sendPush(
                        @RequestHeader(value = "x-api-key", required = false) String apiKey,
                        @RequestHeader(value = "content-type", required = true) String contentType,
                        @RequestHeader(value = "connection", required = false) String connection,
                        @RequestHeader(value = "accept-encoding", required = false) String acceptEncoding,
                        @RequestHeader(value = "x-correlation-id", required = true) String correlationId,
                        @RequestHeader(value = "user-access-key", required = false) String userAccessKey,
                        @RequestHeader(value = "x-smartgbook", required = false) String xSmartgbook,
                        @RequestBody SendPushRequestDto body) {
                RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding,
                                correlationId, userAccessKey, xSmartgbook);
                ResponseDto resultCode = pushService.sendPush(body, header);
                return ResponseEntity.ok(resultCode);
        }

        /**
         * プライマリ連絡先通知送信API
         * 
         * @param apiKey         APIキー
         * @param contentType    Content-Type
         * @param connection     Connection
         * @param acceptEncoding Accept-Encoding
         * @param correlationId  コリレーションID
         * @param userAccessKey  ユーザーアクセスキー
         * @param xSmartgbook    x-smartgbook
         * @param body           リクエストボディ
         * @return 送信結果コード
         */
        @PostMapping("/sendPrimaryContact")
        public ResponseEntity<ResponseDto> sendPrimaryContact(
                        @RequestHeader(value = "x-api-key", required = false) String apiKey,
                        @RequestHeader(value = "content-type", required = true) String contentType,
                        @RequestHeader(value = "connection", required = false) String connection,
                        @RequestHeader(value = "accept-encoding", required = false) String acceptEncoding,
                        @RequestHeader(value = "x-correlation-id", required = true) String correlationId,
                        @RequestHeader(value = "user-access-key", required = false) String userAccessKey,
                        @RequestHeader(value = "x-smartgbook", required = false) String xSmartgbook,
                        @RequestBody SendPrimaryContactRequestDto body) {
                RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding,
                                correlationId, userAccessKey, xSmartgbook);
                ResponseDto resultCode = primaryContactService.sendPrimaryContact(body, header);
                return ResponseEntity.ok(resultCode);
        }
}
