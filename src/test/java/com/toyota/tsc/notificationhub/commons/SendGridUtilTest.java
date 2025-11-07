package com.toyota.tsc.notificationhub.commons;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * テストクラス：SendGridUtilTest
 */
class SendGridUtilTest {

    private SendGridUtil util;

    @BeforeEach
    void setUp() throws Exception {
        util = new SendGridUtil();
        set(util, "sendGridApiKey", "SG.DUMMY.KEY");
        set(util, "fromAddressToyota", "t-noreply@example.com");
        set(util, "fromNameToyota", "TOYOTA");
        set(util, "fromAddressLexus", "l-noreply@example.com");
        set(util, "fromNameLexus", "LEXUS");
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** クラス：SendGridUtil generateEmail（TOYOTAのFrom設定）を確認するテストケース */
    @Test
    void generateEmail_01() {
        // 準備
        // 実行
        Mail mail = util.generateEmail("to@example.com", "TITLE", "TEXT", "<p>HTML</p>", "1");
        // 確認
        assertNotNull(mail);
        assertEquals("TITLE", mail.getSubject());
        // Fromの検証（アドレス/名前）
        assertEquals("t-noreply@example.com", mail.getFrom().getEmail());
        assertEquals("TOYOTA", mail.getFrom().getName());
        // Toの検証
        Personalization p = mail.getPersonalization().get(0);
        Email to = p.getTos().get(0);
        assertEquals("to@example.com", to.getEmail());
    }

    /** クラス：SendGridUtil generateEmail（LEXUSのFrom設定）を確認するテストケース */
    @Test
    void generateEmail_02() {
        // 準備・実行
        Mail mail = util.generateEmail("to2@example.com", "TITLE2", "TEXT2", "<p>HTML2</p>", "2");
        // 確認
        assertNotNull(mail);
        assertEquals("l-noreply@example.com", mail.getFrom().getEmail());
        assertEquals("LEXUS", mail.getFrom().getName());
    }

    /** クラス：SendGridUtil generateEmail（未知ブランド→null）を確認するテストケース */
    @Test
    void generateEmail_03() {
        // 準備・実行
        Mail mail = util.generateEmail("x@example.com", "X", "T", "<p>H</p>", "9");
        // 箮認
        assertNull(mail);
    }

    /** クラス：SendGridUtil executeSendEmail（2xx成功）を確認するテストケース */
    @Test
    void executeSendEmail_01() throws Exception {
        // 準備：Mail生成
        Mail mail = util.generateEmail("ok@example.com", "OK", "T", "<p>H</p>", "1");

        // SendGridコンストラクションをモック
        Response ok = mock(Response.class);
        when(ok.getStatusCode()).thenReturn(202);

        try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
                (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(ok))) {

            // 実行（例外なし）
            assertDoesNotThrow(() -> util.executeSendEmail(mail));

            // 確認：SendGrid#apiが1回呼ばれている
            SendGrid sg = mocked.constructed().get(0);
            verify(sg, times(1)).api(any(Request.class));
        }
    }

    /** クラス：SendGridUtil executeSendEmail（非2xx→TscEMailException）を確認するテストケース */
    @Test
    void executeSendEmail_02() {
        // 準備：Mail生成
        Mail mail = util.generateEmail("ng@example.com", "NG", "T", "<p>H</p>", "1");

        Response ng = mock(Response.class);
        when(ng.getStatusCode()).thenReturn(500);
        when(ng.getBody()).thenReturn("{\"error\":\"sendgrid\"}");

        try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
                (mock, ctx) -> when(mock.api(any(Request.class))).thenReturn(ng))) {

            // 実行・確認（TscEMailException）
            TscEMailException ex = assertThrows(TscEMailException.class, () -> util.executeSendEmail(mail));
            assertEquals(500, ex.getStatusCode());
            assertEquals("ng@example.com", ex.getAddress());
        }
    }

    /** クラス：SendGridUtil executeSendEmail（一般例外→RuntimeException）を確認するテストケース */
    @Test
    void executeSendEmail_03() {
        // 準備
        Mail mail = util.generateEmail("io@example.com", "IO", "T", "<p>H</p>", "2");

        try (MockedConstruction<SendGrid> mocked = mockConstruction(SendGrid.class,
                (mock, ctx) -> when(mock.api(any(Request.class))).thenThrow(new RuntimeException("I/O")))) {

            // 実行・確認（RuntimeException）
            assertThrows(RuntimeException.class, () -> util.executeSendEmail(mail));
        }
    }
}