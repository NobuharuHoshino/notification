package com.toyota.tsc.notificationhub.services;

import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.SendGridUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendMailRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SendMailServiceImplTest {

    @InjectMocks
    private SendMailServiceImpl service;

    @Mock
    private SendGridUtil sendGridUtil;

    private RequestHeaderDto header;

    @BeforeEach
    void setUp() {
        header = new RequestHeaderDto();
        header.setCorrelationId("corr-001");
    }

    /** クラス：SendMailServiceImpl 正常系（メール送信成功）を確認するテストケース */
    @Test
    void sendMail_01() {
        // 準備
        SendMailRequestDto req = new SendMailRequestDto();
        req.setBrdCd("1");
        req.setEmailAddress("user@example.com");
        req.setTitle("T");
        req.setBody_text("text");
        req.setBody_html("<p>html</p>");

        Mail mail = mock(Mail.class);
        when(sendGridUtil.generateEmail(eq("user@example.com"), eq("T"), eq("text"), eq("<p>html</p>"), eq("1")))
                .thenReturn(mail);
        doNothing().when(sendGridUtil).executeSendEmail(eq(mail));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream prev = System.out;
        try (MockedStatic<LogUtil> mockedLog = mockStatic(LogUtil.class);
                MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            System.setOut(new PrintStream(out));

            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.toJson(any())).thenReturn("{json}");
            mockedCommon.when(() -> CommonUtil.getResultCode(eq("SUCCESS"))).thenReturn("SUCCESS_CODE");

            mockedLog.when(() -> LogUtil.info(eq(SendMailServiceImpl.class), anyString()))
                    .thenAnswer(inv -> {
                        System.out.println("INFO:" + inv.getArgument(1));
                        return null;
                    });
            mockedLog.when(() -> LogUtil.error(eq(SendMailServiceImpl.class), anyString()))
                    .thenAnswer(inv -> {
                        System.out.println("ERROR:" + inv.getArgument(1));
                        return null;
                    });

            // 実行
            String result = service.sendMail(req, header);

            // 確認
            assertEquals("SUCCESS_CODE", result);
            verify(sendGridUtil, times(1)).executeSendEmail(eq(mail));
        } finally {
            System.setOut(prev);
        }
    }

    /** クラス：SendMailServiceImpl 異常系（必須項目不足でTscApplicationException）を確認するテストケース */
    @Test
    void sendMail_02() {
        // 準備
        SendMailRequestDto req = new SendMailRequestDto(); // すべて未設定

        try (MockedStatic<LogUtil> mockedLog = mockStatic(LogUtil.class);
                MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // validateRequiredが不足検知→TscApplicationException（実装通り）
            // 実行・確認
            assertThrows(TscApplicationException.class, () -> service.sendMail(req, header));
        }
    }

    /**
     * クラス：SendMailServiceImpl
     * 例外系（SendGrid送信でTscEMailException→RuntimeException）を確認するテストケース
     */
    @Test
    void sendMail_03() {
        // 準備
        SendMailRequestDto req = new SendMailRequestDto();
        req.setBrdCd("1");
        req.setEmailAddress("user@example.com");
        req.setTitle("T");
        req.setBody_text("text");
        req.setBody_html("<p>html</p>");

        Mail mail = mock(Mail.class);
        when(sendGridUtil.generateEmail(any(), any(), any(), any(), any())).thenReturn(mail);
        doThrow(new TscEMailException(500, "{\"errors\":[]}", "user@example.com"))
                .when(sendGridUtil).executeSendEmail(eq(mail));

        try (MockedStatic<LogUtil> mockedLog = mockStatic(LogUtil.class);
                MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");

            // 実行・確認（catch (TscEMailException) → RuntimeException）
            assertThrows(RuntimeException.class, () -> service.sendMail(req, header));
        }
    }

    /**
     * クラス：SendMailServiceImpl 例外系（generateEmailで一般例外→RuntimeException）を確認するテストケース
     */
    @Test
    void sendMail_04() {
        // 準備
        SendMailRequestDto req = new SendMailRequestDto();
        req.setBrdCd("1");
        req.setEmailAddress("user@example.com");
        req.setTitle("T");
        req.setBody_text("text");
        req.setBody_html("<p>html</p>");

        when(sendGridUtil.generateEmail(any(), any(), any(), any(), any())).thenThrow(new RuntimeException("fail"));

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認（catch (Exception) → RuntimeException）
            assertThrows(RuntimeException.class, () -> service.sendMail(req, header));
        }
    }

    /** クラス：SendMailServiceImpl privateメソッドvalidateRequiredの必須検知を確認するテストケース */
    @Test
    void validateRequired_01() throws Exception {
        // 準備
        SendMailRequestDto req = new SendMailRequestDto(); // 未設定

        var m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);

        // 実行
        String missing = (String) m.invoke(service, req);

        // 確認
        assertNotNull(missing);
        assertTrue(missing.contains("brdCd"));
        assertTrue(missing.contains("to"));
        assertTrue(missing.contains("title"));
        assertTrue(missing.contains("body_text"));
        assertTrue(missing.contains("body_html"));
    }

    /** クラス：SendMailServiceImpl privateメソッドisValidBrdCdの妥当値（1/2）を確認するテストケース */
    @Test
    void isValidBrdCd_01() throws Exception {
        // 準備
        var m = SendMailServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);

        // 実行・確認
        assertTrue((Boolean) m.invoke(service, "1"));
        assertTrue((Boolean) m.invoke(service, "2"));
        assertFalse((Boolean) m.invoke(service, "9"));
    }
}