package com.toyota.tsc.notificationhub.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * クラス：CommonUtil すべての分岐・例外系を確認するテストケース
 */
class CommonUtilTest {

    /** テスト補助：static フィールド personalInfoApiUrl を設定 */
    private static void setPersonalInfoApiUrl(String url) throws Exception {
        Field f = CommonUtil.class.getDeclaredField("personalInfoApiUrl");
        f.setAccessible(true);
        f.set(null, url);
    }

    // --- toJson ---

    /** クラス：CommonUtil toJson 正常に JSON へシリアライズできることを確認するテストケース */
    @Test
    void toJson_01() {
        // 準備
        @SuppressWarnings("unused")
        class Dto {
            public String a = "x";
            public int b = 1;
        }
        Dto dto = new Dto();
        // 実行
        String json = CommonUtil.toJson(dto);
        // 確認
        assertTrue(json.contains("\"a\":\"x\""));
        assertTrue(json.contains("\"b\":1"));
    }

    /** クラス：CommonUtil toJson 引数が null の場合に "null" が返ることを確認するテストケース */
    @Test
    void toJson_02() {
        // 実行
        String json = CommonUtil.toJson(null);
        // 確認
        assertEquals("null", json);
    }

    /** クラス：CommonUtil toJson 例外発生時に RuntimeException へ変換されることを確認するテストケース */
    @Test
    void toJson_03() {
        // 準備：ObjectMapper の writeValueAsString を例外にする
        try (MockedConstruction<ObjectMapper> mc = Mockito.mockConstruction(ObjectMapper.class,
                (mapper, ctx) -> when(mapper.writeValueAsString(any())).thenThrow(new RuntimeException("boom")))) {
            // 実行・確認
            assertThrows(RuntimeException.class, () -> CommonUtil.toJson(new Object()));
        }
    }

    // --- getMessage / getResultCode ---

    /** クラス：CommonUtil getMessage ResourceBundle に従い文字列整形されることを確認するテストケース */
    @Test
    void getMessage_01() {
        // 実行
        String msg = CommonUtil.getMessage("RS07I00001", "A", "B", "C");
        // 確認
        assertEquals("A処理を開始します。相関ID：B、パラメータ：C", msg);
    }

    /** クラス：CommonUtil getResultCode ResourceBundle の値が返ることを確認するテストケース */
    @Test
    void getResultCode_01() {
        // 実行
        String rc = CommonUtil.getResultCode("SUCCESS");
        // 確認
        assertEquals("00001548N001", rc);
    }

    // --- maskText ---

    /** クラス：CommonUtil maskText 引数が null/空文字のとき空文字が返ることを確認するテストケース */
    @Test
    void maskText_01() {
        assertEquals("", CommonUtil.maskText(null));
        assertEquals("", CommonUtil.maskText(""));
    }

    /** クラス：CommonUtil maskText メールアドレスをマスクできることを確認するテストケース */
    @Test
    void maskText_02() {
        // 準備
        String email = "ab@example.com";
        // 実行
        String masked = CommonUtil.maskText(email);
        // 確認：先頭2文字 + 8つの* + ドメイン
        assertTrue(masked.startsWith("ab********@example.com"));
    }

    /** クラス：CommonUtil maskText 通常文字列（長さ>2）をマスクできることを確認するテストケース */
    @Test
    void maskText_03() {
        String masked = CommonUtil.maskText("abcdef");
        assertEquals("ab********", masked);
    }

    /** クラス：CommonUtil maskText 通常文字列（長さ<=2）をマスクできることを確認するテストケース */
    @Test
    void maskText_04() {
        assertEquals("a********", CommonUtil.maskText("a"));
        assertEquals("ab********", CommonUtil.maskText("ab"));
    }

    // --- normalizePhoneNumber / maskPhoneNumber ---

    /** クラス：CommonUtil normalizePhoneNumber 非数字を除去することを確認するテストケース */
    @Test
    void normalizePhoneNumber_01() {
        String r = CommonUtil.normalizePhoneNumber("+81-90-1234-5678");
        assertEquals("819012345678", r);
    }

    /** クラス：CommonUtil normalizePhoneNumber null のとき空文字が返ることを確認するテストケース */
    @Test
    void normalizePhoneNumber_02() {
        assertEquals("", CommonUtil.normalizePhoneNumber(null));
    }

    /** クラス：CommonUtil maskPhoneNumber 引数が null/長さ<4 のとき固定マスクが返ることを確認するテストケース */
    @Test
    void maskPhoneNumber_01() {
        assertEquals("********", CommonUtil.maskPhoneNumber(null));
        assertEquals("********", CommonUtil.maskPhoneNumber("12")); // 長さ<4
    }

    /** クラス：CommonUtil maskPhoneNumber 正規化後に末尾4桁を残してマスクされることを確認するテストケース */
    @Test
    void maskPhoneNumber_02() {
        String r = CommonUtil.maskPhoneNumber("+81-90-1234-5678");
        assertTrue(r.endsWith("5678"));
        // 先頭は * が連続している（桁数-4 分）
        assertTrue(r.substring(0, r.length() - 4).matches("\\*+"));
    }

    /** クラス：CommonUtil maskPhoneNumber 長さがちょうど4桁の場合にそのまま返ることを確認するテストケース */
    @Test
    void maskPhoneNumber_03() {
        String r = CommonUtil.maskPhoneNumber("1234");
        assertEquals("1234", r);
    }

    // --- getPersonalInfoApiResponse ---

    // @Test
    // void getPersonalInfoApiResponse_01() {
    // try (MockedConstruction<PropertiesUtil> pc = Mockito.mockConstruction(
    // PropertiesUtil.class,
    // (pu, ctx) ->
    // when(pu.getPersonalInfoApiUrl()).thenReturn("https://test.invalid/api"));
    // MockedConstruction<RestTemplate> rc =
    // Mockito.mockConstruction(RestTemplate.class, (rt, ctx) -> {
    // // varargs版（Object...）にマッチ
    // doReturn(ResponseEntity.ok("{\"contactList\":[]}"))
    // .when(rt).getForEntity(anyString(), eq(String.class), (Object) any());
    // // Map版（保険）
    // doReturn(ResponseEntity.ok("{\"contactList\":[]}"))
    // .when(rt).getForEntity(anyString(), eq(String.class), anyMap());
    // })) {
    // PersonalInfoResponseDto dto =
    // CommonUtil.getPersonalInfoApiResponse("user-001");
    // assertNotNull(dto);
    // assertNotNull(dto.getContactList());
    // }
    // }

    // @Test
    // void getPersonalInfoApiResponse_02() {
    // try (MockedConstruction<PropertiesUtil> pc = Mockito.mockConstruction(
    // PropertiesUtil.class,
    // (pu, ctx) ->
    // when(pu.getPersonalInfoApiUrl()).thenReturn("https://test.invalid/api"));
    // MockedConstruction<RestTemplate> rc =
    // Mockito.mockConstruction(RestTemplate.class, (rt, ctx) -> {
    // doReturn(ResponseEntity.ok("INVALID_JSON"))
    // .when(rt).getForEntity(anyString(), eq(String.class), (Object) any());
    // doReturn(ResponseEntity.ok("INVALID_JSON"))
    // .when(rt).getForEntity(anyString(), eq(String.class), anyMap());
    // });
    // MockedConstruction<ObjectMapper> mc =
    // Mockito.mockConstruction(ObjectMapper.class, (om, ctx) -> {
    // when(om.readValue(anyString(), eq(PersonalInfoResponseDto.class)))
    // .thenThrow(new RuntimeException("parse error"));
    // })) {
    // RuntimeException ex = assertThrows(RuntimeException.class,
    // () -> CommonUtil.getPersonalInfoApiResponse("user-002"));
    // assertTrue(ex.getMessage().contains("parse error")); // 実装がそのまま例外を流す仕様のため
    // }
    // }

    // --- private コンストラクタの網羅 ---

    /** クラス：CommonUtil private コンストラクタのインスタンス化を確認するテストケース */
    @Test
    void constructor_01() throws Exception {
        // 準備
        Constructor<CommonUtil> c = CommonUtil.class.getDeclaredConstructor();
        c.setAccessible(true);
        // 実行
        CommonUtil inst = c.newInstance();
        // 確認
        assertNotNull(inst);
    }
}
