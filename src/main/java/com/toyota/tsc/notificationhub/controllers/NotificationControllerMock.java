package com.toyota.tsc.notificationhub.controllers;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.SendMailRequestDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.models.SendSmsRequestDto;

@RestController
@RequestMapping("/notification/mock")
public class NotificationControllerMock {

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
        return ResponseEntity.ok(CommonUtil.getResultCode("SUCCESS"));
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
        return ResponseEntity.ok(CommonUtil.getResultCode("SUCCESS"));
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
        return ResponseEntity.ok(CommonUtil.getResultCode("SUCCESS"));
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
        return ResponseEntity.ok(CommonUtil.getResultCode("SUCCESS"));
    }

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
        return ResponseEntity.ok(CommonUtil.getResultCode("SUCCESS"));
    }
}
