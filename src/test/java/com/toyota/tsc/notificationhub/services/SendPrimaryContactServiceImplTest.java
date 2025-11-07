package com.toyota.tsc.notificationhub.services;

import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.SendGridUtil;
import com.toyota.tsc.notificationhub.commons.SmsCountryUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.springframework.http.HttpEntity;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class SendPrimaryContactServiceImplTest {

    @InjectMocks
    private SendPrimaryContactServiceImpl service;

    @Mock
    private SmsCountryUtil smsCountryUtil;
    @Mock
    private SendGridUtil sendGridUtil;

    private RequestHeaderDto header;

    @BeforeEach
    void setUp() {
        header = new RequestHeaderDto();
        header.setCorrelationId("corr-002");
    }

    private PersonalInfoResponseDto buildContacts(String... kindsAndValues) {
        var dto = new PersonalInfoResponseDto();
        var list = new java.util.ArrayList<PersonalInfoResponseDto.ContactDto>();
        for (int i = 0; i < kindsAndValues.length; i += 3) {
            var c = new PersonalInfoResponseDto.ContactDto();
            c.setContactType(kindsAndValues[i]); // "1"=PHONE, "2"=EMAIL
            c.setContact(kindsAndValues[i + 1]); // value
            c.setPrimaryContactFlag(Boolean.parseBoolean(kindsAndValues[i + 2])); // "true"/"false"
            list.add(c);
        }
        dto.setContactList(list);
        return dto;
    }

    /**
     * クラス：SendPrimaryContactServiceImpl
     * 正常系（電話のPrimaryにSMS送信）を確認するテストケース
     */
    @Test
    void sendPrimaryContact_01() {
        // 準備
        SendPrimaryContactRequestDto req = new SendPrimaryContactRequestDto();
        req.setProcessId("p");
        req.setInternalUserId("user1");
        req.setBrdCd("1");
        req.setBody_sms("sms");
        req.setTitle("t");
        req.setBody_text("text");
        req.setBody_html("<p>html</p>");

        HttpEntity<String> entity = new HttpEntity<>("body");
        when(smsCountryUtil.createRequest(anyString(), eq("sms"), eq("1"))).thenReturn(entity);
        doNothing().when(smsCountryUtil).sendSmsCountry(eq(entity), eq("+819000000000"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream prev = System.out;
        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> mockedLog = mockStatic(LogUtil.class)) {
            System.setOut(new PrintStream(out));

            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.toJson(any())).thenReturn("{json}");
            mockedCommon.when(() -> CommonUtil.getPersonalInfoApiResponse(eq("user1")))
                    .thenReturn(buildContacts("1", "+819000000000", "true"));
            mockedCommon.when(() -> CommonUtil.normalizePhoneNumber(anyString()))
                    .thenAnswer(inv -> inv.getArgument(0)); // 正規化はそのまま返す
            mockedCommon.when(() -> CommonUtil.getResultCode(eq("SUCCESS"))).thenReturn("SUCCESS");

            mockedLog.when(() -> LogUtil.info(eq(SendPrimaryContactServiceImpl.class), anyString()))
                    .thenAnswer(inv -> {
                        System.out.println("INFO:" + inv.getArgument(1));
                        return null;
                    });

            // 実行
            String result = service.sendPrimaryContact(req, header);

            // 確認
            assertEquals("SUCCESS", result);
            verify(smsCountryUtil, times(1)).sendSmsCountry(eq(entity), eq("+819000000000"));
            // メールユーティリティには一切触れていないことを保証
            verifyNoInteractions(sendGridUtil);
        } finally {
            System.setOut(prev);
        }
    }

    /** クラス：SendPrimaryContactServiceImpl 正常系（メールのPrimaryにメール送信）を確認するテストケース */
    @Test
    void sendPrimaryContact_02() {
        // 準備
        SendPrimaryContactRequestDto req = new SendPrimaryContactRequestDto();
        req.setProcessId("p");
        req.setInternalUserId("user2");
        req.setBrdCd("2");
        req.setTitle("title");
        req.setBody_text("text");
        req.setBody_html("<p>html</p>");

        Mail mail = mock(Mail.class);
        when(sendGridUtil.generateEmail(eq("u2@example.com"), eq("title"), eq("text"), eq("<p>html</p>"), eq("2")))
                .thenReturn(mail);
        doNothing().when(sendGridUtil).executeSendEmail(eq(mail));

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.toJson(any())).thenReturn("{json}");
            mockedCommon.when(() -> CommonUtil.getPersonalInfoApiResponse(eq("user2")))
                    .thenReturn(buildContacts("2", "u2@example.com", "true"));
            mockedCommon.when(() -> CommonUtil.getResultCode(eq("SUCCESS"))).thenReturn("SUCCESS");

            // 実行
            String result = service.sendPrimaryContact(req, header);

            // 確認
            assertEquals("SUCCESS", result);
            verify(sendGridUtil, times(1)).executeSendEmail(eq(mail));
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl
     * 異常系（必須不足でTscApplicationException）を確認するテストケース
     */
    @Test
    void sendPrimaryContact_03() {
        // 準備
        SendPrimaryContactRequestDto req = new SendPrimaryContactRequestDto(); // 未設定

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認
            assertThrows(TscApplicationException.class, () -> service.sendPrimaryContact(req, header));
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl
     * 例外系（SMS送信でTscSMSException→RuntimeException）を確認するテストケース
     */
    @Test
    void sendPrimaryContact_04() {
        // 準備
        SendPrimaryContactRequestDto req = new SendPrimaryContactRequestDto();
        req.setProcessId("p");
        req.setInternalUserId("user3");
        req.setBrdCd("1");
        req.setBody_sms("s");

        HttpEntity<String> entity = new HttpEntity<>("body");
        when(smsCountryUtil.createRequest(anyString(), eq("s"), eq("1"))).thenReturn(entity);
        // 3引数コンストラクタ (statusCode, responseBody, phoneNo)
        doThrow(new TscSMSException(500, "{\"error\":\"sms\"}", "+819000000001"))
                .when(smsCountryUtil).sendSmsCountry(eq(entity), eq("+819000000001"));

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.getPersonalInfoApiResponse(eq("user3")))
                    .thenReturn(buildContacts("1", "+819000000001", "true"));

            // 実行・確認（catch (TscSMSException) → RuntimeException）
            assertThrows(RuntimeException.class, () -> service.sendPrimaryContact(req, header));
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl
     * 例外系（メール送信でTscEMailException→RuntimeException）を確認するテストケース
     */
    @Test
    void sendPrimaryContact_05() {
        // 準備
        SendPrimaryContactRequestDto req = new SendPrimaryContactRequestDto();
        req.setProcessId("p");
        req.setInternalUserId("user4");
        req.setBrdCd("2");
        req.setTitle("t");
        req.setBody_text("txt");
        req.setBody_html("<p>h</p>");

        Mail mail = mock(Mail.class);
        when(sendGridUtil.generateEmail(any(), any(), any(), any(), any())).thenReturn(mail);
        // 3引数コンストラクタ (statusCode, responseBody, address)
        doThrow(new TscEMailException(500, "{\"error\":\"sendgrid\"}", "x@example.com"))
                .when(sendGridUtil).executeSendEmail(eq(mail));

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.getPersonalInfoApiResponse(eq("user4")))
                    .thenReturn(buildContacts("2", "x@example.com", "true"));
            // 実行・確認（catch (TscEMailException) → RuntimeException）
            assertThrows(RuntimeException.class, () -> service.sendPrimaryContact(req, header));
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl privateメソッドvalidateRequiredの必須検知を確認するテストケース
     */
    @Test
    void validateRequired_01() throws Exception {
        // 準備
        SendPrimaryContactRequestDto req = new SendPrimaryContactRequestDto(); // 未設定
        var m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPrimaryContactRequestDto.class);
        m.setAccessible(true);

        // 実行
        String missing = (String) m.invoke(service, req);

        // 確認
        assertNotNull(missing);
        assertTrue(missing.contains("processId"));
        assertTrue(missing.contains("internalUserId"));
        assertTrue(missing.contains("brdCd"));
    }

    /**
     * クラス：SendPrimaryContactServiceImpl
     * privateメソッドisValidBrdCdの妥当値（0/1/2）を確認するテストケース
     */
    @Test
    void isValidBrdCd_01() throws Exception {
        // 準備
        var m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);

        // 実行・確認
        assertTrue((Boolean) m.invoke(service, "0"));
        assertTrue((Boolean) m.invoke(service, "1"));
        assertTrue((Boolean) m.invoke(service, "2"));
        assertFalse((Boolean) m.invoke(service, "9"));
    }
}
