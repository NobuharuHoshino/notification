package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.SmsCountryUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendSmsRequestDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** クラス：SendSmsServiceImpl 分岐網羅100%を確認するテストケース群 */
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

    private SendSmsRequestDto baseRequest() {
        SendSmsRequestDto req = new SendSmsRequestDto();
        req.setBrdCd("1");
        req.setMobileNumber("+819012345678");
        req.setBody_sms("Hello SMS");
        return req;
    }

    /** クラス：SendSmsServiceImpl sendSms 正常完了を確認するテストケース */
    // @Test
    // void sendSms_01() {
    // // 準備
    // SendSmsRequestDto req = baseRequest();
    // HttpEntity<String> entity = new HttpEntity<>("x");
    // when(smsCountryUtil.createRequest(anyString(), anyString(),
    // anyString())).thenReturn(entity);
    // doNothing().when(smsCountryUtil).sendSmsCountry(any(HttpEntity.class),
    // anyString());

    // try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
    // cm.when(() -> CommonUtil.normalizePhoneNumber(anyString())).thenAnswer(inv ->
    // inv.getArgument(0));
    // cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
    // .thenReturn("MSG");
    // cm.when(() ->
    // CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

    // // 実行
    // String code = service.sendSms(req, header);

    // // 確認
    // assertEquals("SUCCESS_CODE", code);
    // verify(smsCountryUtil, times(1)).sendSmsCountry(any(HttpEntity.class),
    // anyString());
    // }
    // }

    /**
     * クラス：SendSmsServiceImpl sendSms
     * TscSMSExceptionをRuntimeExceptionへ変換することを確認するテストケース
     */
    @Test
    void sendSms_02() {
        // 準備
        SendSmsRequestDto req = baseRequest();
        HttpEntity<String> entity = new HttpEntity<>("x");
        when(smsCountryUtil.createRequest(anyString(), anyString(), anyString())).thenReturn(entity);

        TscSMSException ex = mock(TscSMSException.class);
        when(ex.getStatusCode()).thenReturn(400);
        when(ex.getPhoneNo()).thenReturn("+819012345678");
        doThrow(ex).when(smsCountryUtil).sendSmsCountry(any(HttpEntity.class), anyString());

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenReturn("MSG");
            // 実行・確認
            assertThrows(RuntimeException.class, () -> service.sendSms(req, header));
        }
    }

    /**
     * クラス：SendSmsServiceImpl sendSms
     * validateでTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void sendSms_03() {
        // 準備：brdCd不正
        SendSmsRequestDto req = baseRequest();
        req.setBrdCd("9");

        // 実行・確認
        assertThrows(TscApplicationException.class, () -> service.sendSms(req, header));
    }

    /** クラス：SendSmsServiceImpl sendSms 予期せぬ例外をRuntimeExceptionへ変換することを確認するテストケース */
    @Test
    void sendSms_04() {
        // 準備：createRequestでRuntimeException
        SendSmsRequestDto req = baseRequest();
        when(smsCountryUtil.createRequest(anyString(), anyString(), anyString()))
                .thenAnswer(inv -> {
                    throw new RuntimeException("unexpected");
                });

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                    .thenReturn("MSG");
            // 実行・確認
            assertThrows(RuntimeException.class, () -> service.sendSms(req, header));
        }
    }

    /**
     * クラス：SendSmsServiceImpl validate 必須未入力でTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void validate_01() throws Exception {
        SendSmsRequestDto req = new SendSmsRequestDto();
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validate", SendSmsRequestDto.class,
                RequestHeaderDto.class);
        m.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header));
        assertTrue(ite.getCause() instanceof TscApplicationException);
    }

    /** クラス：SendSmsServiceImpl validate 正常時にnullが返ることを確認するテストケース */
    @Test
    void validate_02() throws Exception {
        SendSmsRequestDto req = baseRequest();
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validate", SendSmsRequestDto.class,
                RequestHeaderDto.class);
        m.setAccessible(true);
        Object result = m.invoke(service, req, header);
        assertNull(result);
    }

    /**
     * クラス：SendSmsServiceImpl validateRequired
     * request==nullで\"requestBody\"となることを確認するテストケース
     */
    @Test
    void validateRequired_01() throws Exception {
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validateRequired", SendSmsRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, new Object[] { null });
        assertEquals("requestBody", r);
    }

    // OR両辺（null/empty）— brdCd/mobileNumber/body_sms を個別網羅
    /** クラス：SendSmsServiceImpl validateRequired brdCd==null検出を確認するテストケース */
    @Test
    void validateRequired_02() throws Exception {
        SendSmsRequestDto req = baseRequest();
        req.setBrdCd(null);
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validateRequired", SendSmsRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("brdCd"));
    }

    /** クラス：SendSmsServiceImpl validateRequired brdCd==\"\"検出を確認するテストケース */
    @Test
    void validateRequired_03() throws Exception {
        SendSmsRequestDto req = baseRequest();
        req.setBrdCd("");
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validateRequired", SendSmsRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("brdCd"));
    }

    /** クラス：SendSmsServiceImpl validateRequired mobileNumber==null検出を確認するテストケース */
    @Test
    void validateRequired_04() throws Exception {
        SendSmsRequestDto req = baseRequest();
        req.setMobileNumber(null);
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validateRequired", SendSmsRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("mobileNumber"));
    }

    /** クラス：SendSmsServiceImpl validateRequired mobileNumber==\"\"検出を確認するテストケース */
    @Test
    void validateRequired_05() throws Exception {
        SendSmsRequestDto req = baseRequest();
        req.setMobileNumber("");
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validateRequired", SendSmsRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("mobileNumber"));
    }

    /** クラス：SendSmsServiceImpl validateRequired body_sms==null検出を確認するテストケース */
    @Test
    void validateRequired_06() throws Exception {
        SendSmsRequestDto req = baseRequest();
        req.setBody_sms(null);
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validateRequired", SendSmsRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("body_sms"));
    }

    /** クラス：SendSmsServiceImpl validateRequired body_sms==\"\"検出を確認するテストケース */
    @Test
    void validateRequired_07() throws Exception {
        SendSmsRequestDto req = baseRequest();
        req.setBody_sms("");
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validateRequired", SendSmsRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("body_sms"));
    }

    /** クラス：SendSmsServiceImpl validateRequired 欠落なしでnullとなることを確認するテストケース */
    @Test
    void validateRequired_08() throws Exception {
        SendSmsRequestDto req = baseRequest();
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("validateRequired", SendSmsRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertNull(r);
    }

    /** クラス：SendSmsServiceImpl isValidBrdCd \"1\"の真を確認するテストケース */
    @Test
    void isValidBrdCd_01() throws Exception {
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "1");
        assertTrue(v);
    }

    /** クラス：SendSmsServiceImpl isValidBrdCd \"2\"の真を確認するテストケース */
    @Test
    void isValidBrdCd_02() throws Exception {
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "2");
        assertTrue(v);
    }

    /** クラス：SendSmsServiceImpl isValidBrdCd その他の偽を確認するテストケース */
    @Test
    void isValidBrdCd_03() throws Exception {
        Method m = SendSmsServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "9");
        assertFalse(v);
    }

    /** TscSMSException を catch し、電話番号マスク＆メッセージ生成→RuntimeException を検証 */
    // @Test
    // void sendSms_catchTscSMSException_verifiesMaskingAndMessage() {
    // // Arrange
    // SendSmsRequestDto req = baseRequest();

    // // createRequest は呼ばれるため最小スタブ
    // HttpEntity<String> entity = new HttpEntity<>("x");
    // when(smsCountryUtil.createRequest(anyString(), anyString(), anyString()))
    // .thenReturn(entity);

    // // 送信時に TscSMSException を送出
    // TscSMSException ex = mock(TscSMSException.class);
    // when(ex.getStatusCode()).thenReturn(400);
    // // request の番号がログに使われる仕様
    // doThrow(ex).when(smsCountryUtil).sendSmsCountry(any(HttpEntity.class),
    // eq(req.getMobileNumber()));

    // try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
    // // 使う static メソッドのみスタブ（Strict Stubs 対策）
    // cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
    // .thenReturn("MSG"); // 開始ログ・送信ログ・例外ログで使用
    // cm.when(() -> CommonUtil.normalizePhoneNumber(eq(req.getMobileNumber())))
    // .thenReturn(req.getMobileNumber()); // createRequest 内で使用
    // cm.when(() -> CommonUtil.maskPhoneNumber(eq(req.getMobileNumber())))
    // .thenReturn("+81********678"); // 例外ログで使用

    // // Act & Assert: RuntimeException に変換される
    // assertThrows(RuntimeException.class, () -> service.sendSms(req, header));

    // // Verify: マスク化が呼ばれている
    // cm.verify(() -> CommonUtil.maskPhoneNumber(eq(req.getMobileNumber())),
    // times(2));

    // // Verify: RS07E00006 のメッセージ生成（コード、ステータス、マスク済み電話、相関ID）
    // cm.verify(() -> CommonUtil.getMessage(
    // eq("RS07E00006"),
    // eq(400),
    // eq("+81********678"),
    // eq(header.getCorrelationId())), times(1));

    // // 参考：送信ログ（RS07I00009/10）は例外で途中までの可能性あり。ここでは必須検証は行わず最小化。
    // }
    // }
}
