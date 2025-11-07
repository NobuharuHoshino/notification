package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.SmsCountryUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendSmsRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * テストクラス：SendSmsServiceImplTest
 */
@ExtendWith(MockitoExtension.class)
class SendSmsServiceImplTest {

    @InjectMocks
    private SendSmsServiceImpl service;

    @Mock
    private SmsCountryUtil smsCountryUtil;

    private RequestHeaderDto header;

    @BeforeEach
    void setUp() {
        header = new RequestHeaderDto();
        header.setCorrelationId("corr-sms-001");
    }

    /** クラス：SendSmsServiceImpl 正常系（SMS送信成功）を確認するテストケース */
    @Test
    void sendSms_01() {
        // 準備
        SendSmsRequestDto req = new SendSmsRequestDto();
        req.setBrdCd("1");
        req.setMobileNumber("+819000000000");
        req.setBody_sms("hello");

        HttpEntity<String> entity = new HttpEntity<>("payload");
        when(smsCountryUtil.createRequest(eq("+819000000000"), eq("hello"), eq("1"))).thenReturn(entity);
        doNothing().when(smsCountryUtil).sendSmsCountry(eq(entity), eq("+819000000000"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream prev = System.out;
        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> mockedLog = mockStatic(LogUtil.class)) {
            System.setOut(new PrintStream(out));

            // 静的ユーティリティ
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.toJson(any())).thenReturn("{json}");
            mockedCommon.when(() -> CommonUtil.normalizePhoneNumber(eq("+819000000000"))).thenReturn("+819000000000");
            mockedCommon.when(() -> CommonUtil.maskPhoneNumber(eq("+819000000000"))).thenReturn("********0000");
            mockedCommon.when(() -> CommonUtil.getResultCode(eq("SUCCESS"))).thenReturn("SUCCESS");

            // ログ（stdoutへ転写）
            mockedLog.when(() -> LogUtil.info(eq(SendSmsServiceImpl.class), anyString()))
                    .thenAnswer(inv -> {
                        System.out.println("INFO:" + inv.getArgument(1));
                        return null;
                    });

            // 実行
            String result = service.sendSms(req, header);

            // 確認
            assertEquals("SUCCESS", result);
            verify(smsCountryUtil, times(1)).sendSmsCountry(eq(entity), eq("+819000000000"));
        } finally {
            System.setOut(prev);
        }
    }

    /** クラス：SendSmsServiceImpl 異常系（必須不足でTscApplicationException）を確認するテストケース */
    @Test
    void sendSms_02() {
        // 準備
        SendSmsRequestDto req = new SendSmsRequestDto(); // 未設定

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認
            assertThrows(TscApplicationException.class, () -> service.sendSms(req, header));
        }
    }

    /** クラス：SendSmsServiceImpl 異常系（brdCd不正でTscApplicationException）を確認するテストケース */
    @Test
    void sendSms_03() {
        // 準備
        SendSmsRequestDto req = new SendSmsRequestDto();
        req.setBrdCd("9");
        req.setMobileNumber("+819000000001");
        req.setBody_sms("hello");

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認
            assertThrows(TscApplicationException.class, () -> service.sendSms(req, header));
        }
    }

    /**
     * クラス：SendSmsServiceImpl 例外系（送信でTscSMSException→RuntimeException）を確認するテストケース
     */
    @Test
    void sendSms_04() {
        // 準備
        SendSmsRequestDto req = new SendSmsRequestDto();
        req.setBrdCd("1");
        req.setMobileNumber("+819000000002");
        req.setBody_sms("hello");

        HttpEntity<String> entity = new HttpEntity<>("payload");
        when(smsCountryUtil.createRequest(eq("+819000000002"), eq("hello"), eq("1"))).thenReturn(entity);
        doThrow(new TscSMSException(500, "{\"error\":\"sms\"}", "+819000000002"))
                .when(smsCountryUtil).sendSmsCountry(eq(entity), eq("+819000000002"));

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.maskPhoneNumber(eq("+819000000002"))).thenReturn("********0002");
            // 実行・確認
            assertThrows(RuntimeException.class, () -> service.sendSms(req, header));
        }
    }

    /** クラス：SendSmsServiceImpl privateメソッドvalidateRequiredの必須検知を確認するテストケース */
    @Test
    void validateRequired_01() throws Exception {
        // 準備
        SendSmsRequestDto req = new SendSmsRequestDto(); // 未設定
        var m = SendSmsServiceImpl.class.getDeclaredMethod("validateRequired", SendSmsRequestDto.class);
        m.setAccessible(true);

        // 実行
        String missing = (String) m.invoke(service, req);

        // 確認
        assertNotNull(missing);
        assertTrue(missing.contains("brdCd"));
        assertTrue(missing.contains("mobileNumber"));
        assertTrue(missing.contains("body_sms"));
    }

    /** クラス：SendSmsServiceImpl privateメソッドisValidBrdCdの妥当値（1/2）を確認するテストケース */
    @Test
    void isValidBrdCd_01() throws Exception {
        var m = SendSmsServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(service, "1"));
        assertTrue((Boolean) m.invoke(service, "2"));
        assertFalse((Boolean) m.invoke(service, "9"));
    }
}