package com.toyota.tsc.notificationhub.services;

import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.SendGridUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendMailRequestDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** クラス：SendMailServiceImpl 分岐網羅100%を確認するテストケース群 */
@ExtendWith(MockitoExtension.class)
class SendMailServiceImplTest {

    @InjectMocks
    private SendMailServiceImpl service;

    @Mock
    private SendGridUtil sendGridUtil;

    private RequestHeaderDto header;

    @BeforeEach
    void setUp() {
        header = new RequestHeaderDto();
        header.setCorrelationId("corr-mail-001");
    }

    private SendMailRequestDto baseRequest() {
        SendMailRequestDto req = new SendMailRequestDto();
        req.setBrdCd("1");
        req.setEmailAddress("to@example.com");
        req.setTitle("Hello");
        req.setBody_text("TEXT");
        req.setBody_html("<p>HTML</p>");
        return req;
    }

    /** クラス：SendMailServiceImpl sendMail 正常完了を確認するテストケース */
    @Test
    void sendMail_01() {
        // 準備
        SendMailRequestDto req = baseRequest();
        Mail mail = mock(Mail.class);
        when(sendGridUtil.generateEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mail);
        doNothing().when(sendGridUtil).executeSendEmail(any(Mail.class));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream orig = System.out;
        System.setOut(new PrintStream(out));
        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                    .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            // 実行
            String code = service.sendMail(req, header);

            // 確認
            assertEquals("SUCCESS_CODE", code);
            assertTrue(out.toString().contains("RS07I00002"));
        } finally {
            System.setOut(orig);
        }
    }

    /**
     * クラス：SendMailServiceImpl sendMail
     * TscEMailExceptionをRuntimeExceptionへ変換することを確認するテストケース
     */
    @Test
    void sendMail_02() {
        // 準備
        SendMailRequestDto req = baseRequest();
        Mail mail = mock(Mail.class);
        when(sendGridUtil.generateEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mail);

        TscEMailException ex = mock(TscEMailException.class);
        when(ex.getStatusCode()).thenReturn(500);
        doThrow(ex).when(sendGridUtil).executeSendEmail(any(Mail.class));

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenReturn("MSG");
            // 実行・確認
            assertThrows(RuntimeException.class, () -> service.sendMail(req, header));
        }
    }

    /**
     * クラス：SendMailServiceImpl sendMail
     * validateでTscApplicationExceptionがスローされることを確認するテストケース
     */
    @Test
    void sendMail_03() {
        // 準備：brdCd欠落
        SendMailRequestDto req = baseRequest();
        req.setBrdCd("");

        // 実行・確認
        assertThrows(TscApplicationException.class, () -> service.sendMail(req, header));
    }

    /**
     * クラス：SendMailServiceImpl sendMail 予期せぬ例外をRuntimeExceptionへ変換することを確認するテストケース
     */
    @Test
    void sendMail_04() {
        // 準備：generateEmailでRuntimeException
        SendMailRequestDto req = baseRequest();
        when(sendGridUtil.generateEmail(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(inv -> {
                    throw new RuntimeException("unexpected");
                });

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                    .thenReturn("MSG");
            // 実行・確認
            assertThrows(RuntimeException.class, () -> service.sendMail(req, header));
        }
    }

    /**
     * クラス：SendMailServiceImpl validate
     * 必須未入力でTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void validate_01() throws Exception {
        // 準備
        SendMailRequestDto req = new SendMailRequestDto();
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validate", SendMailRequestDto.class,
                RequestHeaderDto.class);
        m.setAccessible(true);

        // 実行・確認
        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header));
        assertTrue(ite.getCause() instanceof TscApplicationException);
    }

    /** クラス：SendMailServiceImpl validate 正常時にnullが返ることを確認するテストケース */
    @Test
    void validate_02() throws Exception {
        // 準備
        SendMailRequestDto req = baseRequest();
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validate", SendMailRequestDto.class,
                RequestHeaderDto.class);
        m.setAccessible(true);

        // 実行
        Object result = m.invoke(service, req, header);

        // 確認
        assertNull(result);
    }

    /**
     * クラス：SendMailServiceImpl validateRequired
     * request==nullで\"requestBody\"となることを確認するテストケース
     */
    @Test
    void validateRequired_01() throws Exception {
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, new Object[] { null });
        assertEquals("requestBody", result);
    }

    // OR両辺（null/empty）— brdCd/to/title/body_text/body_html を個別網羅
    /** クラス：SendMailServiceImpl validateRequired brdCd==null検出を確認するテストケース */
    @Test
    void validateRequired_02() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setBrdCd(null);
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("brdCd"));
    }

    /** クラス：SendMailServiceImpl validateRequired brdCd==\"\"検出を確認するテストケース */
    @Test
    void validateRequired_03() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setBrdCd("");
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("brdCd"));
    }

    /** クラス：SendMailServiceImpl validateRequired to==null検出を確認するテストケース */
    @Test
    void validateRequired_04() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setEmailAddress(null);
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("to"));
    }

    /** クラス：SendMailServiceImpl validateRequired to==\"\"検出を確認するテストケース */
    @Test
    void validateRequired_05() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setEmailAddress("");
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("to"));
    }

    /** クラス：SendMailServiceImpl validateRequired title==null検出を確認するテストケース */
    @Test
    void validateRequired_06() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setTitle(null);
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("title"));
    }

    /** クラス：SendMailServiceImpl validateRequired title==\"\"検出を確認するテストケース */
    @Test
    void validateRequired_07() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setTitle("");
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("title"));
    }

    /** クラス：SendMailServiceImpl validateRequired body_text==null検出を確認するテストケース */
    @Test
    void validateRequired_08() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setBody_text(null);
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("body_text"));
    }

    /** クラス：SendMailServiceImpl validateRequired body_text==\"\"検出を確認するテストケース */
    @Test
    void validateRequired_09() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setBody_text("");
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("body_text"));
    }

    /** クラス：SendMailServiceImpl validateRequired body_html==null検出を確認するテストケース */
    @Test
    void validateRequired_10() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setBody_html(null);
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("body_html"));
    }

    /** クラス：SendMailServiceImpl validateRequired body_html==\"\"検出を確認するテストケース */
    @Test
    void validateRequired_11() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setBody_html("");
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertTrue(r.contains("body_html"));
    }

    /** クラス：SendMailServiceImpl validateRequired 欠落なしでnullとなることを確認するテストケース */
    @Test
    void validateRequired_12() throws Exception {
        SendMailRequestDto req = baseRequest();
        Method m = SendMailServiceImpl.class.getDeclaredMethod("validateRequired", SendMailRequestDto.class);
        m.setAccessible(true);
        String r = (String) m.invoke(service, req);
        assertNull(r);
    }

    /** クラス：SendMailServiceImpl isValidBrdCd \"1\"の真を確認するテストケース */
    @Test
    void isValidBrdCd_01() throws Exception {
        Method m = SendMailServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "1");
        assertTrue(v);
    }

    /** クラス：SendMailServiceImpl isValidBrdCd \"2\"の真を確認するテストケース */
    @Test
    void isValidBrdCd_02() throws Exception {
        Method m = SendMailServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "2");
        assertTrue(v);
    }

    /** クラス：SendMailServiceImpl isValidBrdCd その他の偽を確認するテストケース */
    @Test
    void isValidBrdCd_03() throws Exception {
        Method m = SendMailServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "9");
        assertFalse(v);
    }

    /**
     * brdCd 不正（isValidBrdCd == false）で RS07E00008 ログ＆TscApplicationException を検証
     */
    @Test
    void validate_brdCdInvalid_throwsTscApplicationException_andLogs() throws Exception {
        SendMailRequestDto req = baseRequest();
        req.setBrdCd("9"); // 無効値に変更（isValidBrdCd が false を返す想定）

        Method m = SendMailServiceImpl.class.getDeclaredMethod("validate", SendMailRequestDto.class,
                RequestHeaderDto.class);
        m.setAccessible(true);

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            // 3引数版のみ使用されるため最小スタブ
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any()))
                    .thenReturn("MSG");

            // Act: validate を直接呼び出す（InvocationTargetException でラップされる）
            InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                    () -> m.invoke(service, req, header));
            // Assert: 中身が TscApplicationException
            assertTrue(ite.getCause() instanceof TscApplicationException);

            // Verify: RS07E00008 のメッセージ生成（brdCd, correlationId）
            cm.verify(() -> CommonUtil.getMessage(
                    eq("RS07E00008"),
                    eq("9"),
                    eq(header.getCorrelationId())), times(1));
        }
    }

    /** brdCd 正常（isValidBrdCd == true）の経路で null が返ること（参考：分岐の対となる経路） */
    @Test
    void validate_brdCdValid_returnsNull() throws Exception {
        SendMailRequestDto req = baseRequest(); // brdCd="1"（有効）

        Method m = SendMailServiceImpl.class.getDeclaredMethod("validate", SendMailRequestDto.class,
                RequestHeaderDto.class);
        m.setAccessible(true);

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            // 必須未入力のログなどは通らないのでスタブ不要だが、保険で3引数版のみ最小スタブ
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any()))
                    .thenReturn("MSG");

            Object result = m.invoke(service, req, header);
            assertNull(result);
        }
    }

}