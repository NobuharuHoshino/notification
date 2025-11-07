package com.toyota.tsc.notificationhub.commons;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * テストクラス：CommonUtilTest
 */
@ExtendWith(MockitoExtension.class)
class CommonUtilTest {

    // ---- テスト補助：static @Value を直接設定 ----
    @BeforeEach
    void init() throws Exception {
        setStatic("personalInfoApiUrl", "https://example.local/personalinfo");
    }

    private static void setStatic(String field, Object value) throws Exception {
        Field f = CommonUtil.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, value);
    }

    // ---------- toJson のテスト ----------

    /** クラス：CommonUtil toJson（正常：シンプルDTOをJSON化）を確認するテストケース */
    @Test
    void toJson_01() {
        // 準備
        class Dto {
            @JsonProperty("a")
            int a = 1;
            @JsonProperty("b")
            String b = "x";
        }
        Dto dto = new Dto();

        // 実行
        String json = CommonUtil.toJson(dto);

        // 確認
        assertTrue(json.contains("\"a\":1"));
        assertTrue(json.contains("\"b\":\"x\""));
    }

    /** クラス：CommonUtil toJson（正常：nullは"null"文字列を返す）を確認するテストケース */
    @Test
    void toJson_02() {
        // 準備：なし

        // 実行
        String json = CommonUtil.toJson(null);

        // 確認
        assertEquals("null", json);
    }

    /** クラス：CommonUtil toJson（例外：循環参照はRuntimeException）を確認するテストケース */
    @Test
    void toJson_03() {
        // 準備
        class Cyclic {
            @SuppressWarnings("unused")
            Cyclic ref;
        }
        Cyclic c = new Cyclic();
        c.ref = c;

        // 実行・確認
        assertThrows(RuntimeException.class, () -> CommonUtil.toJson(c));
    }

    // ---------- maskText のテスト ----------

    /** クラス：CommonUtil maskText（メールアドレスをマスク）を確認するテストケース */
    @Test
    void maskText_01() {
        // 準備
        String in = "na@example.com";

        // 実行
        String out = CommonUtil.maskText(in);

        // 確認（先頭2文字＋8個の*＋ドメイン）
        assertTrue(out.startsWith("na********@example.com"));
    }

    /** クラス：CommonUtil maskText（短い文字列：長さ<=2なら末尾に********）を確認するテストケース */
    @Test
    void maskText_02() {
        // 準備
        String in = "A";

        // 実行
        String out = CommonUtil.maskText(in);

        // 確認
        assertEquals("A********", out);
    }

    /** クラス：CommonUtil maskText（通常文字列：先頭2文字＋********）を確認するテストケース */
    @Test
    void maskText_03() {
        // 準備
        String in = "ABCDEFG";

        // 実行
        String out = CommonUtil.maskText(in);

        // 確認
        assertEquals("AB********", out);
    }

    /** クラス：CommonUtil maskText（null/空文字は空文字）を確認するテストケース */
    @Test
    void maskText_04() {
        // 準備・実行
        String out1 = CommonUtil.maskText(null);
        String out2 = CommonUtil.maskText("");

        // 確認
        assertEquals("", out1);
        assertEquals("", out2);
    }

    // ---------- normalizePhoneNumber / maskPhoneNumber のテスト ----------

    /** クラス：CommonUtil normalizePhoneNumber（数字以外除去）を確認するテストケース */
    @Test
    void normalizePhoneNumber_01() {
        // 準備
        String in = "090-1234-5678";

        // 実行
        String out = CommonUtil.normalizePhoneNumber(in);

        // 確認（数字のみ）
        assertEquals("09012345678", out);
    }

    /** クラス：CommonUtil normalizePhoneNumber（null入力は空文字）を確認するテストケース */
    @Test
    void normalizePhoneNumber_02() {
        // 準備・実行
        String out = CommonUtil.normalizePhoneNumber(null);

        // 確認
        assertEquals("", out);
    }

    /** クラス：CommonUtil maskPhoneNumber（末尾4桁以外を*化）を確認するテストケース */
    @Test
    void maskPhoneNumber_01() {
        // 準備
        String in = "090-1234-5678";

        // 実行
        String out = CommonUtil.maskPhoneNumber(in);

        // 確認（normalize後の桁数に応じて * を付与）
        assertTrue(out.endsWith("5678"));
        assertEquals("*******5678", out); // "09012345678" => 11桁 → 7個の* + 5678
    }

    /** クラス：CommonUtil maskPhoneNumber（4桁未満は********固定）を確認するテストケース */
    @Test
    void maskPhoneNumber_02() {
        // 準備・実行
        String out = CommonUtil.maskPhoneNumber("123");

        // 確認
        assertEquals("********", out);
    }

    /** クラス：CommonUtil getMessage（ResourceBundleの書式適用）を確認するテストケース */
    @Test
    void getMessage_01() {
        // 準備：LogMessages.properties を src/test/resources/properties に配置済み
        // 実行
        String msg = CommonUtil.getMessage("RS07I00001", "PROC", "corr-001", "{json}");

        // 確認（MessageFormat適用結果）
        assertTrue(msg.contains("PROC"));
        assertTrue(msg.contains("corr-001"));
        assertTrue(msg.contains("{json}"));
    }

    /** クラス：CommonUtil getResultCode（ResultCodeマッピング）を確認するテストケース */
    @Test
    void getResultCode_01() {
        // 準備：ResultCode.properties を src/test/resources/properties に配置済み
        // 実行
        String code = CommonUtil.getResultCode("SUCCESS");

        // 確認
        assertEquals("SUCCESS_CODE", code);
    }

    // ---------- getPersonalInfoApiResponse のテスト ----------

    /** クラス：CommonUtil getPersonalInfoApiResponse（正常：JSON→DTO変換）を確認するテストケース */
    @Test
    void getPersonalInfoApiResponse_01() throws Exception {
        // 準備
        String json = "{\"contactList\":[{\"contactType\":\"1\",\"contact\":\"+819000000000\",\"primaryContactFlag\":true}]}";
        ResponseEntity<String> ok = ResponseEntity.ok(json);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.getForEntity(eq("https://example.local/personalinfo"),
                        eq(String.class), any(HttpHeaders.class))).thenReturn(ok))) {

            // 実行
            PersonalInfoResponseDto dto = CommonUtil.getPersonalInfoApiResponse("U1");

            // 確認
            assertNotNull(dto);
            assertNotNull(dto.getContactList());
            assertEquals(1, dto.getContactList().size());
            assertTrue(dto.getContactList().get(0).isPrimaryContactFlag());
        }
    }

    /**
     * クラス：CommonUtil
     * getPersonalInfoApiResponse（例外：JSON不正→RuntimeException）を確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_02() throws Exception {
        // 準備：不正JSON
        ResponseEntity<String> bad = ResponseEntity.ok("{not-json}");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.getForEntity(eq("https://example.local/personalinfo"),
                        eq(String.class), any(HttpHeaders.class))).thenReturn(bad))) {

            // 実行・確認（パース失敗）
            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> CommonUtil.getPersonalInfoApiResponse("U2"));
            assertTrue(ex.getMessage().contains("Failed to parse"), "パース失敗メッセージが含まれること");
        }
    }

    /**
     * クラス：CommonUtil getPersonalInfoApiResponse（例外：RestTemplate一般例外→伝播）を確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_03() throws Exception {
        // 準備：通信例外
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.getForEntity(eq("https://example.local/personalinfo"),
                        eq(String.class), any(HttpHeaders.class))).thenThrow(new RuntimeException("I/O")))) {

            // 実行・確認（通信例外はそのまま伝播）
            assertThrows(RuntimeException.class, () -> CommonUtil.getPersonalInfoApiResponse("U3"));
        }
    }
}