package com.toyota.tsc.notificationhub.controllers;

import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.services.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
        @Mock
        private RegistNotificationDeviceInfoServiceIF registService;
        @Mock
        private SendPushServiceIF pushService;
        @Mock
        private SendPrimaryContactServiceIF primaryContactService;

        @InjectMocks
        private NotificationController controller;

        /** クラス：NotificationController registNotificationDeviceInfoの正常系を確認するテストケース */
        @Test
        void registNotificationDeviceInfo_01() {
                // 準備
                com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto req = new com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto();
                org.mockito.Mockito
                                .when(registService.registDeviceInfo(org.mockito.Mockito.any(),
                                                org.mockito.Mockito.any()))
                                .thenReturn(new ResponseDto("00001548N001"));
                // 実行
                org.springframework.http.ResponseEntity<ResponseDto> res = controller.registNotificationDeviceInfo(
                                "apiKey",
                                "application/json", "keep-alive", "gzip", "corr01", "accessKey", "smartgbook", req);
                // 確認
                org.junit.jupiter.api.Assertions.assertEquals("00001548N001", res.getBody().getResultCode());
        }

        /** クラス：NotificationController registNotificationDeviceInfoの異常系を確認するテストケース */
        @Test
        void registNotificationDeviceInfo_02() {
                // 準備：サービスが例外を投げる
                com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto req = new com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto();
                org.mockito.Mockito
                                .when(registService.registDeviceInfo(org.mockito.Mockito.any(),
                                                org.mockito.Mockito.any()))
                                .thenThrow(new com.toyota.tsc.notificationhub.exceptions.TscApplicationException());
                // 実行・確認
                org.junit.jupiter.api.Assertions
                                .assertThrows(com.toyota.tsc.notificationhub.exceptions.TscApplicationException.class,
                                                () -> {
                                                        controller.registNotificationDeviceInfo("apiKey",
                                                                        "application/json", "keep-alive", "gzip",
                                                                        "corr01", "accessKey", "smartgbook", req);
                                                });
        }

        /** クラス：NotificationController registNotificationDeviceInfoの例外系を確認するテストケース */
        @Test
        void registNotificationDeviceInfo_03() {
                // 準備：サービスがRuntimeExceptionを投げる
                com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto req = new com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto();
                org.mockito.Mockito
                                .when(registService.registDeviceInfo(org.mockito.Mockito.any(),
                                                org.mockito.Mockito.any()))
                                .thenThrow(new RuntimeException());
                // 実行・確認
                org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                        controller.registNotificationDeviceInfo("apiKey", "application/json", "keep-alive", "gzip",
                                        "corr01",
                                        "accessKey", "smartgbook", req);
                });
        }

        /** クラス：NotificationController sendPushの正常系を確認するテストケース */
        @Test
        void sendPush_01() {
                // 準備
                com.toyota.tsc.notificationhub.models.SendPushRequestDto req = new com.toyota.tsc.notificationhub.models.SendPushRequestDto(
                                "proc01", "user01", "pushBody");
                org.mockito.Mockito.when(pushService.sendPush(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                                .thenReturn(new ResponseDto("00001548N001"));
                // 実行
                org.springframework.http.ResponseEntity<ResponseDto> res = controller.sendPush("apiKey",
                                "application/json",
                                "keep-alive", "gzip", "corr01", "accessKey", "smartgbook", req);
                // 確認
                org.junit.jupiter.api.Assertions.assertEquals("00001548N001", res.getBody().getResultCode());
        }

        /** クラス：NotificationController sendPushの異常系を確認するテストケース */
        @Test
        void sendPush_02() {
                // 準備：サービスが例外を投げる
                com.toyota.tsc.notificationhub.models.SendPushRequestDto req = new com.toyota.tsc.notificationhub.models.SendPushRequestDto(
                                "proc01", "user01", "pushBody");
                org.mockito.Mockito.when(pushService.sendPush(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                                .thenThrow(new com.toyota.tsc.notificationhub.exceptions.TscApplicationException());
                // 実行・確認
                org.junit.jupiter.api.Assertions
                                .assertThrows(com.toyota.tsc.notificationhub.exceptions.TscApplicationException.class,
                                                () -> {
                                                        controller.sendPush("apiKey", "application/json", "keep-alive",
                                                                        "gzip", "corr01", "accessKey",
                                                                        "smartgbook", req);
                                                });
        }

        /** クラス：NotificationController sendPushの例外系を確認するテストケース */
        @Test
        void sendPush_03() {
                // 準備：サービスがRuntimeExceptionを投げる
                com.toyota.tsc.notificationhub.models.SendPushRequestDto req = new com.toyota.tsc.notificationhub.models.SendPushRequestDto(
                                "proc01", "user01", "pushBody");
                org.mockito.Mockito.when(pushService.sendPush(org.mockito.Mockito.any(), org.mockito.Mockito.any()))
                                .thenThrow(new RuntimeException());
                // 実行・確認
                org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                        controller.sendPush("apiKey", "application/json", "keep-alive", "gzip", "corr01", "accessKey",
                                        "smartgbook",
                                        req);
                });
        }

        /** クラス：NotificationController sendPrimaryContactの正常系を確認するテストケース */
        @Test
        void sendPrimaryContact_01() {
                // 準備
                com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto req = new com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto(
                                "proc01", "user01", "1", "title", "body_text", "body_html", "body_sms");
                org.mockito.Mockito
                                .when(primaryContactService.sendPrimaryContact(org.mockito.Mockito.any(),
                                                org.mockito.Mockito.any()))
                                .thenReturn(new ResponseDto("00001548N001"));
                // 実行
                org.springframework.http.ResponseEntity<ResponseDto> res = controller.sendPrimaryContact("apiKey",
                                "application/json", "keep-alive", "gzip", "corr01", "accessKey", "smartgbook", req);
                // 確認
                org.junit.jupiter.api.Assertions.assertEquals("00001548N001", res.getBody().getResultCode());
        }

        /** クラス：NotificationController sendPrimaryContactの異常系を確認するテストケース */
        @Test
        void sendPrimaryContact_02() {
                // 準備：サービスが例外を投げる
                com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto req = new com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto(
                                "proc01", "user01", "1", "title", "body_text", "body_html", "body_sms");
                org.mockito.Mockito
                                .when(primaryContactService.sendPrimaryContact(org.mockito.Mockito.any(),
                                                org.mockito.Mockito.any()))
                                .thenThrow(new com.toyota.tsc.notificationhub.exceptions.TscApplicationException());
                // 実行・確認
                org.junit.jupiter.api.Assertions
                                .assertThrows(com.toyota.tsc.notificationhub.exceptions.TscApplicationException.class,
                                                () -> {
                                                        controller.sendPrimaryContact("apiKey", "application/json",
                                                                        "keep-alive", "gzip", "corr01",
                                                                        "accessKey", "smartgbook", req);
                                                });
        }

        /** クラス：NotificationController sendPrimaryContactの例外系を確認するテストケース */
        @Test
        void sendPrimaryContact_03() {
                // 準備：サービスがRuntimeExceptionを投げる
                com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto req = new com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto(
                                "proc01", "user01", "1", "title", "body_text", "body_html", "body_sms");
                org.mockito.Mockito
                                .when(primaryContactService.sendPrimaryContact(org.mockito.Mockito.any(),
                                                org.mockito.Mockito.any()))
                                .thenThrow(new RuntimeException());
                // 実行・確認
                org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                        controller.sendPrimaryContact("apiKey", "application/json", "keep-alive", "gzip", "corr01",
                                        "accessKey",
                                        "smartgbook", req);
                });
        }
}