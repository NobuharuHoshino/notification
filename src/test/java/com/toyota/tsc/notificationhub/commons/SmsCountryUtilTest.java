package com.toyota.tsc.notificationhub.commons;

import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * テストクラス：SmsCountryUtilTest
 */
class SmsCountryUtilTest {

    private SmsCountryUtil util;

    @BeforeEach
    void setUp() throws Exception {
        util = new SmsCountryUtil();
        set(util, "smsCountryUser", "user@example.com");
        set(util, "smsCountryPass", "pass-123");
        set(util, "senderIdToyota", "TOYOTA-ID");
        set(util, "senderIdLexus", "LEXUS-ID");
        set(util, "smsCountryApiUrl", "https://api.smscountry.local/send");
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static String hexUtf16BE(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_16BE);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
            sb.append(String.format("%02X", b));
        return sb.toString();
    }

    /**
     * クラス：SmsCountryUtil createRequest（TOYOTAのSenderID/URLエンコード/Hex化）を確認するテストケース
     */
    @Test
    void createRequest_01() {
        // 準備
        String to = "+819000000000";
        String msg = "こんにちは";
        String hex = hexUtf16BE(msg);

        // 実行
        HttpEntity<String> entity = util.createRequest(to, msg, "1");

        // 確認
        assertNotNull(entity);
        HttpHeaders headers = entity.getHeaders();
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, headers.getContentType());
        String body = entity.getBody();
        assertTrue(body.contains("User=user%40example.com")); // URLエンコード済みユーザ
        assertTrue(body.contains("mobilenumber=" + to));
        assertTrue(body.contains("message=" + hex));
        assertTrue(body.contains("SenderID=TOYOTA-ID"));
    }

    /** クラス：SmsCountryUtil createRequest（LEXUSのSenderID）を確認するテストケース */
    @Test
    void createRequest_02() {
        // 実行
        HttpEntity<String> entity = util.createRequest("+819000000001", "HI", "2");
        // 確認
        assertNotNull(entity);
        assertTrue(entity.getBody().contains("SenderID=LEXUS-ID"));
    }

    /** クラス：SmsCountryUtil createRequest（未知ブランド→null）を確認するテストケース */
    @Test
    void createRequest_03() {
        // 実行
        HttpEntity<String> entity = util.createRequest("+819000000002", "HI", "9");
        // 確認
        assertNull(entity);
    }

    // /** クラス：SmsCountryUtil sendSmsCountry（2xx成功）を確認するテストケース */
    // @Test
    // void sendSmsCountry_01() {
    // // 準備
    // HttpEntity<String> entity = new HttpEntity<>("payload");

    // try (MockedConstruction<RestTemplate> mocked =
    // mockConstruction(RestTemplate.class,
    // (mock, ctx) ->
    // when(mock.postForEntity(eq("https://api.smscountry.local/send"),
    // eq(entity), eq(String.class))).thenReturn(ResponseEntity.ok("OK")))) {

    // // 実行・確認
    // assertDoesNotThrow(() -> util.sendSmsCountry(entity, "+819000000000"));

    // // RestTemplate#postForEntityの呼び出し確認
    // RestTemplate rt = mocked.constructed().get(0);
    // verify(rt, times(1)).postForEntity(eq("https://api.smscountry.local/send"),
    // eq(entity), eq(String.class));
    // }
    // }

    // /** クラス：SmsCountryUtil sendSmsCountry（非2xx→TscSMSException）を確認するテストケース */
    // @Test
    // void sendSmsCountry_02() {
    // // 準備
    // HttpEntity<String> entity = new HttpEntity<>("payload");

    // try (MockedConstruction<RestTemplate> mocked =
    // mockConstruction(RestTemplate.class,
    // (mock, ctx) ->
    // when(mock.postForEntity(eq("https://api.smscountry.local/send"),
    // eq(entity),
    // eq(String.class))).thenReturn(ResponseEntity.status(500).body("ERR")))) {

    // // 実行・確認
    // TscSMSException ex = assertThrows(TscSMSException.class,
    // () -> util.sendSmsCountry(entity, "+819000000003"));
    // assertEquals(500, ex.getStatusCode());
    // assertEquals("+819000000003", ex.getPhoneNo());
    // }
    // }

    // /** クラス：SmsCountryUtil sendSmsCountry（一般例外→RuntimeException）を確認するテストケース */
    // @Test
    // void sendSmsCountry_03() {
    // // 準備
    // HttpEntity<String> entity = new HttpEntity<>("payload");

    // try (MockedConstruction<RestTemplate> mocked =
    // mockConstruction(RestTemplate.class,
    // (mock, ctx) ->
    // when(mock.postForEntity(eq("https://api.smscountry.local/send"),
    // eq(entity), eq(String.class))).thenThrow(new RuntimeException("I/O")))) {

    // // 実行・確認
    // assertThrows(RuntimeException.class, () -> util.sendSmsCountry(entity,
    // "+819000000004"));
    // }
    // }

    /**
     * クラス：SmsCountryUtil createRequest 例外をRuntimeExceptionにラップして再送出することを確認するテストケース
     */
    @Test
    void createRequest_04() throws Exception {
        // 準備：URLEncoder.encode(...) で NPE を発生させるためユーザ名を null に
        setField(util, SmsCountryUtil.class, "smsCountryUser", null);

        // 実行・確認：RuntimeException にラップされて送出される
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> util.createRequest("+819012345678", "日本語もOK", "1")); // BRD_TOYOTA
        assertNotNull(ex.getCause()); // 原因例外が内包される
        assertTrue(ex.getCause() instanceof NullPointerException); // NPE が原因であること
    }

    private static void setField(Object target, Class<?> declaring, String name, Object value) throws Exception {
        var f = declaring.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

}
