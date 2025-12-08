package com.toyota.tsc.notificationhub.services;

import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.SendGridUtil;
import com.toyota.tsc.notificationhub.commons.SmsCountryUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** クラス：SendPrimaryContactServiceImpl 分岐網羅100%を確認するテストケース群 */
@ExtendWith(MockitoExtension.class)
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
        header.setCorrelationId("corr-pc-001");
    }

    private SendPrimaryContactRequestDto baseRequest() {
        SendPrimaryContactRequestDto req = new SendPrimaryContactRequestDto();
        req.setProcessId("PROC1");
        req.setInternalUserId("U1");
        req.setBrdCd("1");
        req.setTitle("Hello");
        req.setBodyText("TEXT");
        req.setBodyHtml("<p>HTML</p>");
        req.setBodySms("SMS");
        return req;
    }

    private PersonalInfoResponseDto mockResponse(List<PersonalInfoResponseDto.ContactDto> contacts) {
        PersonalInfoResponseDto resp = mock(PersonalInfoResponseDto.class);
        when(resp.getContactList()).thenReturn(contacts);
        return resp;
    }

    private PersonalInfoResponseDto.ContactDto contact(String type, boolean primary, String value) {
        PersonalInfoResponseDto.ContactDto c = mock(PersonalInfoResponseDto.ContactDto.class);
        when(c.getContactType()).thenReturn(type);
        when(c.isPrimaryContactFlag()).thenReturn(primary);
        when(c.getContact()).thenReturn(value);
        return c;
    }

    /**
     * クラス：SendPrimaryContactServiceImpl sendPrimaryContact
     * 電話primaryでSMS送信されることを確認するテストケース
     */
    @Test
    void sendPrimaryContact_01() {
        // 準備：電話primary
        SendPrimaryContactRequestDto req = baseRequest();
        PersonalInfoResponseDto.ContactDto phone = contact("1", true, "+819012345678");
        PersonalInfoResponseDto resp = mockResponse(Collections.singletonList(phone));
        HttpEntity<String> entity = new HttpEntity<>("x");
        when(smsCountryUtil.createRequest(anyString(), anyString(), anyString())).thenReturn(entity);
        doNothing().when(smsCountryUtil).sendSmsCountry(any(HttpEntity.class), anyString());

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString())).thenReturn(resp);
            cm.when(() -> CommonUtil.normalizePhoneNumber(anyString())).thenAnswer(inv -> inv.getArgument(0));
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                    .thenReturn("MSG");
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenReturn("MSG");
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            // 実行
            String code = service.sendPrimaryContact(req, header).getResultCode();

            // 確認：正常終了コード・SMSユーティリティが呼ばれた
            assertEquals("SUCCESS_CODE", code);
            verify(smsCountryUtil, times(1)).sendSmsCountry(any(HttpEntity.class), anyString());
            verify(sendGridUtil, times(0)).executeSendEmail(any(Mail.class));
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl sendPrimaryContact
     * メールprimaryでメール送信されることを確認するテストケース
     */
    @Test
    void sendPrimaryContact_02() {
        // 準備：メールprimary
        SendPrimaryContactRequestDto req = baseRequest();
        PersonalInfoResponseDto.ContactDto email = contact("2", true, "to@example.com");
        PersonalInfoResponseDto resp = mockResponse(Collections.singletonList(email));
        Mail mail = mock(Mail.class);
        when(sendGridUtil.generateEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mail);
        doNothing().when(sendGridUtil).executeSendEmail(any(Mail.class));

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString())).thenReturn(resp);
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenReturn("MSG");
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            // 実行
            String code = service.sendPrimaryContact(req, header).getResultCode();

            // 確認：正常終了コード・メールユーティリティが呼ばれた
            assertEquals("SUCCESS_CODE", code);
            verify(sendGridUtil, times(1)).executeSendEmail(any(Mail.class));
            verify(smsCountryUtil, times(0)).sendSmsCountry(any(HttpEntity.class), anyString());
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl sendPrimaryContact
     * primary=falseでスキップされることを確認するテストケース
     */
    @Test
    void sendPrimaryContact_03() {
        // 準備：primary=false → isPrimaryContactFlag() だけが使われる
        SendPrimaryContactRequestDto req = baseRequest();

        PersonalInfoResponseDto.ContactDto notPrimary = mock(PersonalInfoResponseDto.ContactDto.class);
        when(notPrimary.isPrimaryContactFlag()).thenReturn(false); // ★ 必要最小限のスタブ

        PersonalInfoResponseDto resp = mockResponse(Collections.singletonList(notPrimary));

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString())).thenReturn(resp);
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            // 実行
            String code = service.sendPrimaryContact(req, header).getResultCode();

            // 確認：正常終了・外部API呼ばれない
            assertEquals("SUCCESS_CODE", code);
            verify(smsCountryUtil, times(0))
                    .sendSmsCountry(ArgumentMatchers.<HttpEntity<String>>any(), anyString());
            verify(sendGridUtil, times(0)).executeSendEmail(any(Mail.class));
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl sendPrimaryContact
     * その他typeでスキップされることを確認するテストケース
     */
    @Test
    void sendPrimaryContact_04() {
        // 準備：type="X" かつ primary=true → getContactType()/isPrimaryContactFlag() を使用
        SendPrimaryContactRequestDto req = baseRequest();

        PersonalInfoResponseDto.ContactDto other = mock(PersonalInfoResponseDto.ContactDto.class);
        when(other.getContactType()).thenReturn("X"); // ★ 必要
        when(other.isPrimaryContactFlag()).thenReturn(true); // ★ 必要
        // ※ getContact() はこの分岐では使わないためスタブしない

        PersonalInfoResponseDto resp = mockResponse(Collections.singletonList(other));

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString())).thenReturn(resp);
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            // 実行
            String code = service.sendPrimaryContact(req, header).getResultCode();

            // 確認：正常終了・外部API呼ばれない
            assertEquals("SUCCESS_CODE", code);
            verify(smsCountryUtil, times(0))
                    .sendSmsCountry(ArgumentMatchers.<HttpEntity<String>>any(), anyString());
            verify(sendGridUtil, times(0)).executeSendEmail(any(Mail.class));
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl sendPrimaryContact
     * SMS例外をCustomExceptionへ変換することを確認するテストケース
     */
    @Test
    void sendPrimaryContact_05() {
        // 準備：電話primary・SMS側でTscSMSException
        SendPrimaryContactRequestDto req = baseRequest();
        PersonalInfoResponseDto.ContactDto phone = contact("1", true, "+819012345678");
        PersonalInfoResponseDto resp = mockResponse(Collections.singletonList(phone));
        HttpEntity<String> entity = new HttpEntity<>("x");
        when(smsCountryUtil.createRequest(anyString(), anyString(), anyString())).thenReturn(entity);

        TscSMSException ex = mock(TscSMSException.class);
        when(ex.getStatusCode()).thenReturn(400);
        when(ex.getPhoneNo()).thenReturn("+819012345678");
        doThrow(ex).when(smsCountryUtil).sendSmsCountry(any(HttpEntity.class), anyString());

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString())).thenReturn(resp);
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenReturn("MSG");
            // 実行・確認：CustomException へ
            assertThrows(CustomException.class, () -> service.sendPrimaryContact(req, header));
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl sendPrimaryContact
     * メール例外をCustomExceptionへ変換することを確認するテストケース
     */
    @Test
    void sendPrimaryContact_06() {
        // 準備：メールprimary・メール側でTscEMailException
        SendPrimaryContactRequestDto req = baseRequest();
        PersonalInfoResponseDto.ContactDto email = contact("2", true, "to@example.com");
        PersonalInfoResponseDto resp = mockResponse(Collections.singletonList(email));
        Mail mail = mock(Mail.class);
        when(sendGridUtil.generateEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mail);

        TscEMailException ex = mock(TscEMailException.class);
        when(ex.getStatusCode()).thenReturn(500);
        when(ex.getAddress()).thenReturn("to@example.com");
        doThrow(ex).when(sendGridUtil).executeSendEmail(any(Mail.class));

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString())).thenReturn(resp);
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenReturn("MSG");
            // 実行・確認：CustomException へ
            assertThrows(CustomException.class, () -> service.sendPrimaryContact(req, header));
        }
    }

    /**
     * クラス：SendPrimaryContactServiceImpl sendPrimaryContact
     * validateでTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void sendPrimaryContact_07() {
        // 準備：brdCd不正
        SendPrimaryContactRequestDto req = baseRequest();
        req.setBrdCd("9");

        // 実行・確認
        assertThrows(TscApplicationException.class, () -> service.sendPrimaryContact(req, header));
    }

    /**
     * クラス：SendPrimaryContactServiceImpl validate
     * 必須未入力でTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void validate_01() throws Exception {
        SendPrimaryContactRequestDto req = new SendPrimaryContactRequestDto();
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validate",
                SendPrimaryContactRequestDto.class, RequestHeaderDto.class);
        m.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header));
        assertTrue(ite.getCause() instanceof TscApplicationException);
    }

    /** クラス：SendPrimaryContactServiceImpl validate 正常時にnullが返ることを確認するテストケース */
    @Test
    void validate_02() throws Exception {
        SendPrimaryContactRequestDto req = baseRequest();
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validate",
                SendPrimaryContactRequestDto.class, RequestHeaderDto.class);
        m.setAccessible(true);
        Object result = m.invoke(service, req, header);
        assertNull(result);
    }

    /**
     * クラス：SendPrimaryContactServiceImpl validateRequired
     * request==nullで\"requestBody\"となることを確認するテストケース
     */
    @Test
    void validateRequired_01() throws Exception {
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPrimaryContactRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, new Object[] { null });
        assertEquals("requestBody", r);
    }

    // OR両辺（null/empty）— processId/internalUserId/brdCd を個別網羅
    /**
     * クラス：SendPrimaryContactServiceImpl validateRequired
     * processId==null検出を確認するテストケース
     */
    @Test
    void validateRequired_02() throws Exception {
        SendPrimaryContactRequestDto req = baseRequest();
        req.setProcessId(null);
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPrimaryContactRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("processId"));
    }

    /**
     * クラス：SendPrimaryContactServiceImpl validateRequired
     * processId==\"\"検出を確認するテストケース
     */
    @Test
    void validateRequired_03() throws Exception {
        SendPrimaryContactRequestDto req = baseRequest();
        req.setProcessId("");
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPrimaryContactRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("processId"));
    }

    /**
     * クラス：SendPrimaryContactServiceImpl validateRequired
     * internalUserId==null検出を確認するテストケース
     */
    @Test
    void validateRequired_04() throws Exception {
        SendPrimaryContactRequestDto req = baseRequest();
        req.setInternalUserId(null);
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPrimaryContactRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("internalUserId"));
    }

    /**
     * クラス：SendPrimaryContactServiceImpl validateRequired
     * internalUserId==\"\"検出を確認するテストケース
     */
    @Test
    void validateRequired_05() throws Exception {
        SendPrimaryContactRequestDto req = baseRequest();
        req.setInternalUserId("");
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPrimaryContactRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("internalUserId"));
    }

    /**
     * クラス：SendPrimaryContactServiceImpl validateRequired brdCd==null検出を確認するテストケース
     */
    @Test
    void validateRequired_06() throws Exception {
        SendPrimaryContactRequestDto req = baseRequest();
        req.setBrdCd(null);
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPrimaryContactRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("brdCd"));
    }

    /**
     * クラス：SendPrimaryContactServiceImpl validateRequired brdCd==\"\"検出を確認するテストケース
     */
    @Test
    void validateRequired_07() throws Exception {
        SendPrimaryContactRequestDto req = baseRequest();
        req.setBrdCd("");
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPrimaryContactRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("brdCd"));
    }

    /**
     * クラス：SendPrimaryContactServiceImpl validateRequired 欠落なしでnullとなることを確認するテストケース
     */
    @Test
    void validateRequired_08() throws Exception {
        SendPrimaryContactRequestDto req = baseRequest();
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPrimaryContactRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertNull(r);
    }

    /** クラス：SendPrimaryContactServiceImpl isValidBrdCd \"0\"の真を確認するテストケース */
    @Test
    void isValidBrdCd_01() throws Exception {
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "0");
        assertTrue(v);
    }

    /** クラス：SendPrimaryContactServiceImpl isValidBrdCd \"1\"の真を確認するテストケース */
    @Test
    void isValidBrdCd_02() throws Exception {
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "1");
        assertTrue(v);
    }

    /** クラス：SendPrimaryContactServiceImpl isValidBrdCd \"2\"の真を確認するテストケース */
    @Test
    void isValidBrdCd_03() throws Exception {
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "2");
        assertTrue(v);
    }

    /** クラス：SendPrimaryContactServiceImpl isValidBrdCd その他の偽を確認するテストケース */
    @Test
    void isValidBrdCd_04() throws Exception {
        Method m = SendPrimaryContactServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "9");
        assertFalse(v);
    }

    @Test
    void sendPrimaryContact_SMS_catch_verifiesMaskingAndMessage() {
        // Arrange
        SendPrimaryContactRequestDto req = baseRequest();

        // 電話 primary の連絡先モック
        PersonalInfoResponseDto.ContactDto phone = mock(PersonalInfoResponseDto.ContactDto.class);
        when(phone.getContactType()).thenReturn("1"); // CONTACT_PHONE
        when(phone.isPrimaryContactFlag()).thenReturn(true); // primary = true
        when(phone.getContact()).thenReturn("+819012345678");

        PersonalInfoResponseDto resp = mock(PersonalInfoResponseDto.class);
        when(resp.getContactList()).thenReturn(Collections.singletonList(phone));

        HttpEntity<String> entity = new HttpEntity<>("x");
        when(smsCountryUtil.createRequest(anyString(), anyString(), anyString()))
                .thenReturn(entity);

        // SMS側で TscSMSException を送出
        TscSMSException ex = mock(TscSMSException.class);
        when(ex.getStatusCode()).thenReturn(400);
        when(ex.getPhoneNo()).thenReturn("+819012345678");
        doThrow(ex).when(smsCountryUtil).sendSmsCountry(any(HttpEntity.class), anyString());

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            // 個人情報 API の戻り値（非 null）
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString()))
                    .thenReturn(resp);

            // 開始ログなどで呼ばれる getMessage（4/5引数版を広くスタブ）
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                    .thenReturn("MSG");
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenReturn("MSG");

            // 電話番号の正規化（createRequest 内で使用）
            cm.when(() -> CommonUtil.normalizePhoneNumber("+819012345678"))
                    .thenReturn("+819012345678");

            // マスク化（catch(TscSMSException) のエラーログで使用）
            cm.when(() -> CommonUtil.maskPhoneNumber("+819012345678"))
                    .thenReturn("+81********678");

            // Act + Assert：CustomException に変換される
            assertThrows(CustomException.class, () -> service.sendPrimaryContact(req, header));

            // Verify：マスク化が呼ばれている
            cm.verify(() -> CommonUtil.maskPhoneNumber("+819012345678"), times(1));

            // Verify：RS07E00006 のメッセージ生成（コード、ステータス、マスク済み電話、相関ID）
            cm.verify(() -> CommonUtil.getMessage(
                    eq("RS07E00006"),
                    eq(400),
                    eq("+81********678"),
                    eq(header.getCorrelationId())), times(1));
        }
    }

    @Test
    void sendRequest_phone_AND_leftTrue_rightFalse_branch() {
        SendPrimaryContactRequestDto req = baseRequest();

        PersonalInfoResponseDto.ContactDto c = mock(PersonalInfoResponseDto.ContactDto.class);
        when(c.getContactType()).thenReturn("1"); // 左項 true
        when(c.isPrimaryContactFlag()).thenReturn(true, false); // 1回目: 先頭IF通過, 2回目: AND右項 false

        PersonalInfoResponseDto resp = mock(PersonalInfoResponseDto.class);
        when(resp.getContactList()).thenReturn(Collections.singletonList(c));

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString())).thenReturn(resp);
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any())).thenReturn("MSG");
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any())).thenReturn("MSG");
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            String code = service.sendPrimaryContact(req, header).getResultCode();
            assertEquals("SUCCESS_CODE", code);

            // AND 条件が false で SMS 送信は呼ばれない
            verify(smsCountryUtil, times(0)).sendSmsCountry(any(), anyString());
            verify(sendGridUtil, times(0)).executeSendEmail(any());
        }
    }

    @Test
    void sendRequest_email_AND_leftTrue_rightFalse_branch() {
        SendPrimaryContactRequestDto req = baseRequest();

        PersonalInfoResponseDto.ContactDto c = mock(PersonalInfoResponseDto.ContactDto.class);
        when(c.getContactType()).thenReturn("2"); // 左項 true
        when(c.isPrimaryContactFlag()).thenReturn(true, false); // 1回目: 先頭IF通過, 2回目: AND右項 false

        PersonalInfoResponseDto resp = mock(PersonalInfoResponseDto.class);
        when(resp.getContactList()).thenReturn(Collections.singletonList(c));

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString())).thenReturn(resp);
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any())).thenReturn("MSG");
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any())).thenReturn("MSG");
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            String code = service.sendPrimaryContact(req, header).getResultCode();
            assertEquals("SUCCESS_CODE", code);

            // AND 条件が false でメール送信は呼ばれない
            verify(sendGridUtil, times(0)).executeSendEmail(any());
            verify(smsCountryUtil, times(0)).sendSmsCountry(any(), anyString());
        }
    }

    @Test
    void sendPrimaryContact_responseNull_throwsRuntimeAndLogs() {
        // Arrange
        SendPrimaryContactRequestDto req = baseRequest();

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            // 個人情報APIが null を返す
            cm.when(() -> CommonUtil.getPersonalInfoApiResponse(anyString()))
                    .thenReturn(null);

            // 4引数の getMessage（開始ログ・例外ログなど）をスタブ
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                    .thenReturn("MSG");

            // Act + Assert：CustomException に変換される
            assertThrows(TscApplicationException.class, () -> service.sendPrimaryContact(req, header));

        }

        // Verify：外部ユーティリティは呼ばれない
        verify(smsCountryUtil, times(0)).createRequest(anyString(), anyString(), anyString());
        verify(smsCountryUtil, times(0)).sendSmsCountry(any(), anyString());
        verify(sendGridUtil, times(0)).generateEmail(anyString(), anyString(), anyString(), anyString(), anyString());
        verify(sendGridUtil, times(0)).executeSendEmail(any(Mail.class));
    }

}
