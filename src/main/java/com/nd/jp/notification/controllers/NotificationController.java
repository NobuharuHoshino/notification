package com.nd.jp.notification.controllers;

import com.nd.jp.notification.models.*;
import com.nd.jp.notification.service.*;
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
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "content-type", required = false) String contentType,
            @RequestHeader(value = "connection", required = false) String connection,
            @RequestHeader(value = "accept-encoding", required = false) String acceptEncoding,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "user-access-key", required = false) String userAccessKey,
            @RequestHeader(value = "x-smartgbook", required = false) String xSmartgbook,
            @RequestBody RegistNotificationDeviceInfoRequestDto body) {
        RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey, xSmartgbook);
        String resultCode = registService.registDeviceInfo(body, header);
        return ResponseEntity.ok(resultCode);
    }

    @PostMapping("/sendPush")
    public ResponseEntity<String> sendPush(
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "content-type", required = false) String contentType,
            @RequestHeader(value = "connection", required = false) String connection,
            @RequestHeader(value = "accept-encoding", required = false) String acceptEncoding,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "user-access-key", required = false) String userAccessKey,
            @RequestHeader(value = "x-smartgbook", required = false) String xSmartgbook,
            @RequestBody SendPushRequestDto body) {
        RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey, xSmartgbook);
        String resultCode = pushService.sendPush(body, header);
        return ResponseEntity.ok(resultCode);
    }

    @PostMapping("/sendPrimaryContact")
    public ResponseEntity<String> sendPrimaryContact(
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "content-type", required = false) String contentType,
            @RequestHeader(value = "connection", required = false) String connection,
            @RequestHeader(value = "accept-encoding", required = false) String acceptEncoding,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "user-access-key", required = false) String userAccessKey,
            @RequestHeader(value = "x-smartgbook", required = false) String xSmartgbook,
            @RequestBody SendPrimaryContactRequestDto body) {
        RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey, xSmartgbook);
        String resultCode = primaryContactService.sendPrimaryContact(body, header);
        return ResponseEntity.ok(resultCode);
    }

    @PostMapping("/sendMail")
    public ResponseEntity<String> sendMail(
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "content-type", required = false) String contentType,
            @RequestHeader(value = "connection", required = false) String connection,
            @RequestHeader(value = "accept-encoding", required = false) String acceptEncoding,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "user-access-key", required = false) String userAccessKey,
            @RequestHeader(value = "x-smartgbook", required = false) String xSmartgbook,
            @RequestBody SendMailRequestDto body) {
        RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey, xSmartgbook);
        String resultCode = mailService.sendMail(body, header);
        return ResponseEntity.ok(resultCode);
    }

    @PostMapping("/sendSms")
    public ResponseEntity<String> sendSms(
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestHeader(value = "content-type", required = false) String contentType,
            @RequestHeader(value = "connection", required = false) String connection,
            @RequestHeader(value = "accept-encoding", required = false) String acceptEncoding,
            @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
            @RequestHeader(value = "user-access-key", required = false) String userAccessKey,
            @RequestHeader(value = "x-smartgbook", required = false) String xSmartgbook,
            @RequestBody SendSmsRequestDto body) {
        RequestHeaderDto header = new RequestHeaderDto(apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey, xSmartgbook);
        String resultCode = smsService.sendSms(body, header);
        return ResponseEntity.ok(resultCode);
    }
}
