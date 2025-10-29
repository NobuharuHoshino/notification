package com.toyota.tsc.notificationhub.controllers;

import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.SendMailRequestDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.models.SendSmsRequestDto;
import com.toyota.tsc.notificationhub.services.RegistNotificationDeviceInfoServiceIF;
import com.toyota.tsc.notificationhub.services.SendMailServiceIF;
import com.toyota.tsc.notificationhub.services.SendPrimaryContactServiceIF;
import com.toyota.tsc.notificationhub.services.SendPushServiceIF;
import com.toyota.tsc.notificationhub.services.SendSmsServiceIF;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/dcm/service-list")
@RequiredArgsConstructor
public class NotificationController {
        private final RegistNotificationDeviceInfoServiceIF registService;
        private final SendPushServiceIF pushService;
        private final SendPrimaryContactServiceIF primaryContactService;
        private final SendMailServiceIF mailService;
        private final SendSmsServiceIF smsService;

        @PostMapping("/registNotificationDeviceInfo")
        public ResponseEntity<String> registNotificationDeviceInfo(
                        @RequestHeader(value = "x-api-key", required = true) String apiKey,
                        @RequestHeader(value = "content-type", required = true) String contentType,
                        @RequestHeader(value = "connection", required = true) String connection,
                        @RequestHeader(value = "accept-encoding", required = true) String acceptEncoding,
                        @RequestHeader(value = "x-correlation-id", required = true) String correlationId,
                        @RequestHeader(value = "user-access-key", required = true) String userAccessKey,
                        @RequestHeader(value = "x-smartgbook", required = true) String xSmartgbook,
                        @RequestBody RegistNotificationDeviceInfoRequestDto body) {
                RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding,
                                correlationId,
                                userAccessKey, xSmartgbook);
                String resultCode = registService.registDeviceInfo(body, header);
                return ResponseEntity.ok(resultCode);
        }

        @PostMapping("/sendPush")
        public ResponseEntity<String> sendPush(
                        @RequestHeader(value = "x-api-key", required = true) String apiKey,
                        @RequestHeader(value = "content-type", required = true) String contentType,
                        @RequestHeader(value = "connection", required = true) String connection,
                        @RequestHeader(value = "accept-encoding", required = true) String acceptEncoding,
                        @RequestHeader(value = "x-correlation-id", required = true) String correlationId,
                        @RequestHeader(value = "user-access-key", required = true) String userAccessKey,
                        @RequestHeader(value = "x-smartgbook", required = true) String xSmartgbook,
                        @RequestBody SendPushRequestDto body) {
                RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding,
                                correlationId,
                                userAccessKey, xSmartgbook);
                String resultCode = pushService.sendPush(body, header);
                return ResponseEntity.ok(resultCode);
        }

        @PostMapping("/sendPrimaryContact")
        public ResponseEntity<String> sendPrimaryContact(
                        @RequestHeader(value = "x-api-key", required = true) String apiKey,
                        @RequestHeader(value = "content-type", required = true) String contentType,
                        @RequestHeader(value = "connection", required = true) String connection,
                        @RequestHeader(value = "accept-encoding", required = true) String acceptEncoding,
                        @RequestHeader(value = "x-correlation-id", required = true) String correlationId,
                        @RequestHeader(value = "user-access-key", required = true) String userAccessKey,
                        @RequestHeader(value = "x-smartgbook", required = true) String xSmartgbook,
                        @RequestBody SendPrimaryContactRequestDto body) {
                RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding,
                                correlationId,
                                userAccessKey, xSmartgbook);
                String resultCode = primaryContactService.sendPrimaryContact(body, header);
                return ResponseEntity.ok(resultCode);
        }

        @PostMapping("/sendMail")
        public ResponseEntity<String> sendMail(
                        @RequestHeader(value = "x-api-key", required = true) String apiKey,
                        @RequestHeader(value = "content-type", required = true) String contentType,
                        @RequestHeader(value = "connection", required = true) String connection,
                        @RequestHeader(value = "accept-encoding", required = true) String acceptEncoding,
                        @RequestHeader(value = "x-correlation-id", required = true) String correlationId,
                        @RequestHeader(value = "user-access-key", required = true) String userAccessKey,
                        @RequestHeader(value = "x-smartgbook", required = true) String xSmartgbook,
                        @RequestBody SendMailRequestDto body) {
                RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding,
                                correlationId,
                                userAccessKey, xSmartgbook);
                String resultCode = mailService.sendMail(body, header);
                return ResponseEntity.ok(resultCode);
        }

        @PostMapping("/sendSms")
        public ResponseEntity<String> sendSms(
                        @RequestHeader(value = "x-api-key", required = true) String apiKey,
                        @RequestHeader(value = "content-type", required = true) String contentType,
                        @RequestHeader(value = "connection", required = true) String connection,
                        @RequestHeader(value = "accept-encoding", required = true) String acceptEncoding,
                        @RequestHeader(value = "x-correlation-id", required = true) String correlationId,
                        @RequestHeader(value = "user-access-key", required = true) String userAccessKey,
                        @RequestHeader(value = "x-smartgbook", required = true) String xSmartgbook,
                        @RequestBody SendSmsRequestDto body) {
                RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding,
                                correlationId,
                                userAccessKey, xSmartgbook);
                String resultCode = smsService.sendSms(body, header);
                return ResponseEntity.ok(resultCode);
        }
}
