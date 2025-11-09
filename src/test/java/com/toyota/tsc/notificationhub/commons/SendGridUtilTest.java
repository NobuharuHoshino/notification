package com.toyota.tsc.notificationhub.commons;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * クラス：SendGridUtil すべての分岐を確認するテストケース
 */
class SendGridUtilTest {

    private SendGridUtil util;

    @BeforeEach
    void setup() throws Exception {
        util = new SendGridUtil();
        // @Value を直接セット（最小）
        setField(util, SendGridUtil.class, "sendGridApiKey", "SG.TEST");
        setField(util, SendGridUtil.class, "fromAddressToyota", "t@toyota.example");
        setField(util, SendGridUtil.class, "fromNameToyota", "TOYOTA");
        setField(util, SendGridUtil.class, "fromAddressLexus", "l@lexus.example");
        setField(util, SendGridUtil.class, "fromNameLexus", "LEXUS");
    }

    private static void setField(Object target, Class<?> declaring, String name, Object value) throws Exception {
        var f = declaring.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // --- generateEmail ---

    /** クラス：SendGridUtil generateEmail TOYOTAブランドの生成を確認するテストケース */
    @Test
    void generateEmail_01() {
        Mail mail = util.generateEmail("to@example.com", "Title", "TEXT", "<p>HTML</p>", "1");
        assertNotNull(mail);
        assertEquals("Title", mail.getSubject());
        assertEquals("t@toyota.example", mail.getFrom().getEmail());
    }

    /** クラス：SendGridUtil generateEmail LEXUSブランドの生成を確認するテストケース */
    @Test
    void generateEmail_02() {
        Mail mail = util.generateEmail("to@example.com", "Title", "TEXT", "<p>HTML</p>", "2");
        assertNotNull(mail);
        assertEquals("l@lexus.example", mail.getFrom().getEmail());
    }

    /** クラス：SendGridUtil generateEmail 不正ブランドでnullとなることを確認するテストケース */
    @Test
    void generateEmail_03() {
        assertNull(util.generateEmail("to@example.com", "Title", "TEXT", "<p>HTML</p>", "9"));
    }

    // --- executeSendEmail ---

    /** クラス：SendGridUtil executeSendEmail 成功（2xx）を確認するテストケース */
    @Test
    void executeSendEmail_01() {
        Mail mail = util.generateEmail("to@example.com", "Title", "TEXT", "<p>HTML</p>", "1");
        try (MockedConstruction<SendGrid> sgc = Mockito.mockConstruction(SendGrid.class, (sg, ctx) -> {
            Response resp = mock(Response.class);
            when(resp.getStatusCode()).thenReturn(202);
            when(sg.api(any(Request.class))).thenReturn(resp);
        })) {
            assertDoesNotThrow(() -> util.executeSendEmail(mail));
        }
    }

    /**
     * クラス：SendGridUtil executeSendEmail 非2xx（400等）でTscEMailException送出を確認するテストケース
     */
    @Test
    void executeSendEmail_02() {
        Mail mail = util.generateEmail("to@example.com", "Title", "TEXT", "<p>HTML</p>", "1");
        try (MockedConstruction<SendGrid> sgc = Mockito.mockConstruction(SendGrid.class, (sg, ctx) -> {
            Response resp = mock(Response.class);
            when(resp.getStatusCode()).thenReturn(400);
            when(resp.getBody()).thenReturn("Bad Request");
            when(sg.api(any(Request.class))).thenReturn(resp);
        })) {
            TscEMailException ex = assertThrows(TscEMailException.class, () -> util.executeSendEmail(mail));
            assertEquals(400, ex.getStatusCode());
            assertEquals("to@example.com",
                    mail.getPersonalization().get(0).getTos().get(0).getEmail());
        }
    }

    /**
     * クラス：SendGridUtil executeSendEmail
     * SendGrid側がTscEMailException（statusCode=-1系）を投げた場合の再スローを確認するテストケース
     */
    @Test
    void executeSendEmail_03() {
        Mail mail = util.generateEmail("to@example.com", "Title", "TEXT", "<p>HTML</p>", "1");
        try (MockedConstruction<SendGrid> sgc = Mockito.mockConstruction(SendGrid.class, (sg, ctx) -> {
            // メッセージ・cause・address コンストラクタ（statusCode は -1）
            when(sg.api(any(Request.class)))
                    .thenThrow(new TscEMailException("SG Error", new RuntimeException("io"), "to@example.com"));
        })) {
            TscEMailException ex = assertThrows(TscEMailException.class, () -> util.executeSendEmail(mail));
            assertEquals(-1, ex.getStatusCode());
            assertEquals("to@example.com", ex.getAddress());
        }
    }

    /**
     * クラス：SendGridUtil executeSendEmail 予期せぬ例外がRuntimeExceptionへ変換されることを確認するテストケース
     */
    @Test
    void executeSendEmail_04() {
        Mail mail = util.generateEmail("to@example.com", "Title", "TEXT", "<p>HTML</p>", "1");
        try (MockedConstruction<SendGrid> sgc = Mockito.mockConstruction(SendGrid.class, (sg, ctx) -> {
            when(sg.api(any(Request.class))).thenThrow(new RuntimeException("boom"));
        })) {
            assertThrows(RuntimeException.class, () -> util.executeSendEmail(mail));
        }
    }

    /**
     * クラス：SendGridUtil executeSendEmail
     * ステータスコードが 199（<200）で TscEMailException を送出することを確認するテストケース
     */
    @Test
    void executeSendEmail_05_status199_triggersExceptionLeftOperand() {
        // 準備
        Mail mail = util.generateEmail("to@example.com", "Title", "TEXT", "<p>HTML</p>", "1");

        try (MockedConstruction<SendGrid> sgc = Mockito.mockConstruction(SendGrid.class, (sg, ctx) -> {
            Response resp = mock(Response.class);
            when(resp.getStatusCode()).thenReturn(199); // ★ 左辺 true を狙って 199
            when(resp.getBody()).thenReturn("Below range");
            when(sg.api(any(Request.class))).thenReturn(resp);
        })) {
            // 実行・確認
            TscEMailException ex = assertThrows(TscEMailException.class, () -> util.executeSendEmail(mail));
            assertEquals(199, ex.getStatusCode());
            assertEquals("to@example.com",
                    mail.getPersonalization().get(0).getTos().get(0).getEmail());
        }
    }

    /**
     * クラス：SendGridUtil executeSendEmail
     * ステータスコードが 300（>=300）で TscEMailException を送出することを確認するテストケース
     */
    @Test
    void executeSendEmail_06_status300_triggersExceptionRightOperandBoundary() {
        // 準備
        Mail mail = util.generateEmail("to@example.com", "Title", "TEXT", "<p>HTML</p>", "1");

        try (MockedConstruction<SendGrid> sgc = Mockito.mockConstruction(SendGrid.class, (sg, ctx) -> {
            Response resp = mock(Response.class);
            when(resp.getStatusCode()).thenReturn(300); // ★ 右辺 true の境界
            when(resp.getBody()).thenReturn("Boundary 300");
            when(sg.api(any(Request.class))).thenReturn(resp);
        })) {
            // 実行・確認
            TscEMailException ex = assertThrows(TscEMailException.class, () -> util.executeSendEmail(mail));
            assertEquals(300, ex.getStatusCode());
            assertEquals("to@example.com",
                    mail.getPersonalization().get(0).getTos().get(0).getEmail());
        }
    }
}