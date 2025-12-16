
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/commons/CommonUtilTest.java
package com.toyota.tsc.notificationhub.commons;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.MissingResourceException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommonUtil のテストクラス
 */
@SuppressWarnings("all")
class CommonUtilTest {

    private static final String MASKED_STRING = "********";

    @AfterEach
    void tearDown() {
        // Arrange/Act/Assert から分離し、各テストの独立性を確保
        System.clearProperty("spring.profiles.active");
    }

    /** クラス：CommonUtil コンストラクタがprivateであることを確認するテストケース */
    @Test
    void CommonUtil_001() throws Exception {
        // Arrange
        Constructor<CommonUtil> ctor = CommonUtil.class.getDeclaredConstructor();

        // Act
        int mod = ctor.getModifiers();

        // Assert
        assertTrue(Modifier.isPrivate(mod));
    }

    /** クラス：CommonUtil toJson 正常にJSON文字列へ変換できることを確認するテストケース */
    @Test
    void toJson_001() {
        // Arrange
        DummyDto dto = new DummyDto();
        dto.id = 1;
        dto.name = "test";

        // Act
        String json = CommonUtil.toJson(dto);

        // Assert
        assertTrue(json.contains("\"id\":1"));
        assertTrue(json.contains("\"name\":\"test\""));
    }

    /** クラス：CommonUtil toJson Jackson変換例外時にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void toJson_002() {
        // Arrange
        SelfRefDto dto = new SelfRefDto();

        // Act
        CustomException ex = assertThrows(CustomException.class, () -> CommonUtil.toJson(dto));

        // Assert
        assertNotNull(ex.getCause());
    }

    /**
     * クラス：CommonUtil getMessage
     * spring.profiles.activeが未設定の場合にLogMessagesを使用することを確認するテストケース
     */
    @Test
    void getMessage_001() {
        // Arrange
        System.clearProperty("spring.profiles.active");

        // Act
        String msg = CommonUtil.getMessage("TEST001", "World");

        // Assert
        assertEquals("LOG: Hello World", msg);
    }

    /**
     * クラス：CommonUtil getMessage
     * spring.profiles.activeにsaが含まれる場合にSaLogMessagesを使用することを確認するテストケース
     */
    @Test
    void getMessage_002() {
        // Arrange
        System.setProperty("spring.profiles.active", "sa");

        // Act
        String msg = CommonUtil.getMessage("TEST001", "World");

        // Assert
        assertEquals("SA: Hello World", msg);
    }

    /**
     * クラス：CommonUtil getMessage
     * spring.profiles.activeに複数プロファイルがありsaが含まれる場合にSaLogMessagesを使用することを確認するテストケース
     */
    @Test
    void getMessage_003() {
        // Arrange
        System.setProperty("spring.profiles.active", "dev, sa,  ");

        // Act
        String msg = CommonUtil.getMessage("TEST001", "World");

        // Assert
        assertEquals("SA: Hello World", msg);
    }

    /**
     * クラス：CommonUtil getMessage
     * 存在しないメッセージIDの場合にMissingResourceExceptionが送出されることを確認するテストケース
     */
    @Test
    void getMessage_004() {
        // Arrange
        System.clearProperty("spring.profiles.active");

        // Act
        MissingResourceException ex = assertThrows(MissingResourceException.class,
                () -> CommonUtil.getMessage("NOT_FOUND_ID"));

        // Assert
        assertNotNull(ex);
    }

    /** クラス：CommonUtil getResultCode 正常に結果コードを取得できることを確認するテストケース */
    @Test
    void getResultCode_001() {
        // Arrange
        String key = "EXCEPTION";

        // Act
        String code = CommonUtil.getResultCode(key);

        // Assert
        assertEquals("RC-EXCEPTION", code);
    }

    /**
     * クラス：CommonUtil getResultCode
     * 存在しないキーの場合にMissingResourceExceptionが送出されることを確認するテストケース
     */
    @Test
    void getResultCode_002() {
        // Arrange
        String key = "NOT_FOUND_CODE";

        // Act
        MissingResourceException ex = assertThrows(MissingResourceException.class, () -> CommonUtil.getResultCode(key));

        // Assert
        assertNotNull(ex);
    }

    /** クラス：CommonUtil maskText 引数がnullの場合に空文字が返ることを確認するテストケース */
    @Test
    void maskText_001() {
        // Arrange
        String text = null;

        // Act
        String masked = CommonUtil.maskText(text);

        // Assert
        assertEquals("", masked);
    }

    /** クラス：CommonUtil maskText 引数が空文字の場合に空文字が返ることを確認するテストケース */
    @Test
    void maskText_002() {
        // Arrange
        String text = "";

        // Act
        String masked = CommonUtil.maskText(text);

        // Assert
        assertEquals("", masked);
    }

    /** クラス：CommonUtil maskText メール形式で@より前が1文字の場合に先頭1文字+マスク+ドメインが返ることを確認するテストケース */
    @Test
    void maskText_003() {
        // Arrange
        String text = "a@example.com";

        // Act
        String masked = CommonUtil.maskText(text);

        // Assert
        assertEquals("a" + MASKED_STRING + "@example.com", masked);
    }

    /**
     * クラス：CommonUtil maskText メール形式で@より前が2文字以上の場合に先頭2文字+マスク+ドメインが返ることを確認するテストケース
     */
    @Test
    void maskText_004() {
        // Arrange
        String text = "ab@example.com";

        // Act
        String masked = CommonUtil.maskText(text);

        // Assert
        assertEquals("ab" + MASKED_STRING + "@example.com", masked);
    }

    /** クラス：CommonUtil maskText @を含まない2文字以下の文字列の場合に元文字列+マスクが返ることを確認するテストケース */
    @Test
    void maskText_005() {
        // Arrange
        String text = "ab";

        // Act
        String masked = CommonUtil.maskText(text);

        // Assert
        assertEquals("ab" + MASKED_STRING, masked);
    }

    /** クラス：CommonUtil maskText @を含まない3文字以上の文字列の場合に先頭2文字+マスクが返ることを確認するテストケース */
    @Test
    void maskText_006() {
        // Arrange
        String text = "abc";

        // Act
        String masked = CommonUtil.maskText(text);

        // Assert
        assertEquals("ab" + MASKED_STRING, masked);
    }

    /** クラス：CommonUtil maskPhoneNumber 引数がnullの場合に固定マスクが返ることを確認するテストケース */
    @Test
    void maskPhoneNumber_001() {
        // Arrange
        String phone = null;

        // Act
        String masked = CommonUtil.maskPhoneNumber(phone);

        // Assert
        assertEquals(MASKED_STRING, masked);
    }

    /** クラス：CommonUtil maskPhoneNumber 引数長が4未満の場合に固定マスクが返ることを確認するテストケース */
    @Test
    void maskPhoneNumber_002() {
        // Arrange
        String phone = "123";

        // Act
        String masked = CommonUtil.maskPhoneNumber(phone);

        // Assert
        assertEquals(MASKED_STRING, masked);
    }

    /** クラス：CommonUtil maskPhoneNumber 正規化後4桁の場合にそのまま4桁が返ることを確認するテストケース */
    @Test
    void maskPhoneNumber_003() {
        // Arrange
        String phone = "1234";

        // Act
        String masked = CommonUtil.maskPhoneNumber(phone);

        // Assert
        assertEquals("1234", masked);
    }

    /** クラス：CommonUtil maskPhoneNumber 正規化後5桁の場合に先頭がマスクされ末尾4桁が残ることを確認するテストケース */
    @Test
    void maskPhoneNumber_004() {
        // Arrange
        String phone = "12345";

        // Act
        String masked = CommonUtil.maskPhoneNumber(phone);

        // Assert
        assertEquals("*2345", masked);
    }

    /** クラス：CommonUtil maskPhoneNumber 記号を含む電話番号が正規化され末尾4桁のみ残ることを確認するテストケース */
    @Test
    void maskPhoneNumber_005() {
        // Arrange
        String phone = "090-1234-5678";

        // Act
        String masked = CommonUtil.maskPhoneNumber(phone);

        // Assert
        assertEquals("*******5678", masked);

    }

    /** クラス：CommonUtil maskPhoneNumber 入力長は4以上だが正規化後が4未満の場合に例外が発生することを確認するテストケース */
    @Test
    void maskPhoneNumber_006() {
        // Arrange
        String phone = "a-b-c-1";

        // Act
        StringIndexOutOfBoundsException ex = assertThrows(StringIndexOutOfBoundsException.class,
                () -> CommonUtil.maskPhoneNumber(phone));

        // Assert
        assertNotNull(ex);
    }

    /** クラス：CommonUtil normalizePhoneNumber 引数がnullの場合に空文字が返ることを確認するテストケース */
    @Test
    void normalizePhoneNumber_001() {
        // Arrange
        String phone = null;

        // Act
        String normalized = CommonUtil.normalizePhoneNumber(phone);

        // Assert
        assertEquals("", normalized);
    }

    /** クラス：CommonUtil normalizePhoneNumber 非数字が除去され数字のみになることを確認するテストケース */
    @Test
    void normalizePhoneNumber_002() {
        // Arrange
        String phone = "+81 (90) 1234-5678";

        // Act
        String normalized = CommonUtil.normalizePhoneNumber(phone);

        // Assert
        assertEquals("819012345678", normalized);
    }

    // ----- テスト用DTO -----
    static class DummyDto {
        public int id;
        public String name;
    }

    static class SelfRefDto {
        public SelfRefDto self = this;
    }
}
