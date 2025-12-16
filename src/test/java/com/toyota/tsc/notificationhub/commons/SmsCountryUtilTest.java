
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/commons/SmsCountryUtilTest.java
package com.toyota.tsc.notificationhub.commons;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SmsCountryUtil のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class SmsCountryUtilTest {

    @Mock
    private PropertiesUtil propertiesUtil;

    /** クラス：SmsCountryUtil createRequest TOYOTAブランド(1)でリクエストが生成されることを確認するテストケース */
    @Test
    void createRequest_001() throws Exception {
        // Arrange
        when(propertiesUtil.getSenderIdToyota()).thenReturn("SIDT");
        when(propertiesUtil.getSmsCountryUser()).thenReturn("user");
        when(propertiesUtil.getSmsCountryPass()).thenReturn("pass");

        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);
        String encodedUser = URLEncoder.encode("user", "UTF-8");

        // Act
        HttpEntity<String> entity = sut.createRequest("819012345678", "A", "1");

        // Assert
        assertNotNull(entity);
        assertEquals(MediaType.APPLICATION_FORM_URLENCODED, entity.getHeaders().getContentType());
        assertNotNull(entity.getBody());
        assertTrue(entity.getBody().contains("User=" + encodedUser));
        assertTrue(entity.getBody().contains("&passwd=pass"));
        assertTrue(entity.getBody().contains("&mobilenumber=819012345678"));
        assertTrue(entity.getBody().contains("&message=0041"));
        assertTrue(entity.getBody().contains("&sid=SIDT"));
        assertTrue(entity.getBody().contains("&Mtype=OL&DR=N"));
    }

    /** クラス：SmsCountryUtil createRequest 無効ブランド(0)でもTOYOTA扱いで生成されることを確認するテストケース */
    @Test
    void createRequest_002() {
        // Arrange
        when(propertiesUtil.getSenderIdToyota()).thenReturn("SIDT");
        when(propertiesUtil.getSmsCountryUser()).thenReturn("user");
        when(propertiesUtil.getSmsCountryPass()).thenReturn("pass");

        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        // Act
        HttpEntity<String> entity = sut.createRequest("1", "A", "0");

        // Assert
        assertNotNull(entity);
        assertTrue(entity.getBody().contains("&sid=SIDT"));
    }

    /** クラス：SmsCountryUtil createRequest LEXUSブランド(2)でリクエストが生成されることを確認するテストケース */
    @Test
    void createRequest_003() {
        // Arrange
        when(propertiesUtil.getSenderIdLexus()).thenReturn("SIDL");
        when(propertiesUtil.getSmsCountryUser()).thenReturn("user");
        when(propertiesUtil.getSmsCountryPass()).thenReturn("pass");

        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        // Act
        HttpEntity<String> entity = sut.createRequest("1", "A", "2");

        // Assert
        assertNotNull(entity);
        assertTrue(entity.getBody().contains("&sid=SIDL"));
    }

    /** クラス：SmsCountryUtil createRequest 不正ブランドの場合にnullが返ることを確認するテストケース */
    @Test
    void createRequest_004() {
        // Arrange
        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        // Act
        HttpEntity<String> entity = sut.createRequest("1", "A", "9");

        // Assert
        assertNull(entity);
    }

    /**
     * クラス：SmsCountryUtil createRequest
     * messageがnullの場合にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void createRequest_005() {
        // Arrange
        when(propertiesUtil.getSenderIdToyota()).thenReturn("SIDT");
        when(propertiesUtil.getSmsCountryUser()).thenReturn("user");
        // ★ getSmsCountryPass() は message=null で toHexUtf16BE 到達時に例外となり参照されないためスタブしない

        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        // Act
        CustomException ex = assertThrows(CustomException.class, () -> sut.createRequest("1", null, "1"));

        // Assert
        assertNotNull(ex.getCause());
    }

    /**
     * クラス：SmsCountryUtil createRequest
     * userがnullの場合にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void createRequest_006() {
        // Arrange
        when(propertiesUtil.getSenderIdToyota()).thenReturn("SIDT");
        when(propertiesUtil.getSmsCountryUser()).thenReturn(null);
        // ★ getSmsCountryPass() は URLEncoder.encode で例外になり参照されないためスタブしない

        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        // Act
        CustomException ex = assertThrows(CustomException.class, () -> sut.createRequest("1", "A", "1"));

        // Assert
        assertNotNull(ex.getCause());
    }

    /** クラス：SmsCountryUtil sendSmsCountry 2xxの場合に例外が発生しないことを確認するテストケース */
    @Test
    void sendSmsCountry_001() {
        // Arrange
        when(propertiesUtil.getSmsCountryApiUrl()).thenReturn("https://example/sms");

        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        HttpEntity<String> entity = new HttpEntity<>("payload", new HttpHeaders());
        ResponseEntity<String> ok = ResponseEntity.ok("OK");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.postForEntity(eq("https://example/sms"), eq(entity), eq(String.class)))
                        .thenReturn(ok))) {

            // Act
            assertDoesNotThrow(() -> sut.sendSmsCountry(entity, "090"));

            // Assert
            RestTemplate rt = mocked.constructed().get(0);
            verify(rt, times(1)).postForEntity(eq("https://example/sms"), eq(entity), eq(String.class));
        }
    }

    /**
     * クラス：SmsCountryUtil sendSmsCountry 非2xxの場合にTscSMSExceptionが送出されることを確認するテストケース
     */
    @Test
    void sendSmsCountry_002() {
        // Arrange
        when(propertiesUtil.getSmsCountryApiUrl()).thenReturn("https://example/sms");

        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        HttpEntity<String> entity = new HttpEntity<>("payload", new HttpHeaders());
        ResponseEntity<String> ng = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("NG");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.postForEntity(eq("https://example/sms"), eq(entity), eq(String.class)))
                        .thenReturn(ng))) {

            // Act
            TscSMSException ex = assertThrows(TscSMSException.class, () -> sut.sendSmsCountry(entity, "090"));

            // Assert
            assertEquals(400, ex.getStatusCode());
            assertEquals("NG", ex.getResponseBody());
            assertEquals("090", ex.getPhoneNo());
        }
    }

    /**
     * クラス：SmsCountryUtil sendSmsCountry
     * postForEntityがTscSMSExceptionを投げた場合にTscSMSExceptionが送出されることを確認するテストケース
     */
    @Test
    void sendSmsCountry_003() {
        // Arrange
        when(propertiesUtil.getSmsCountryApiUrl()).thenReturn("https://example/sms");

        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        HttpEntity<String> entity = new HttpEntity<>("payload", new HttpHeaders());
        TscSMSException cause = new TscSMSException(500, "ERR", "090");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.postForEntity(eq("https://example/sms"), eq(entity), eq(String.class)))
                        .thenThrow(cause))) {

            // Act
            TscSMSException ex = assertThrows(TscSMSException.class, () -> sut.sendSmsCountry(entity, "090"));

            // Assert
            assertEquals(500, ex.getStatusCode());
            assertEquals("ERR", ex.getResponseBody());
            assertEquals("090", ex.getPhoneNo());
        }
    }

    /**
     * クラス：SmsCountryUtil sendSmsCountry
     * postForEntityが例外を投げた場合にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void sendSmsCountry_004() {
        // Arrange
        when(propertiesUtil.getSmsCountryApiUrl()).thenReturn("https://example/sms");

        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        HttpEntity<String> entity = new HttpEntity<>("payload", new HttpHeaders());

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.postForEntity(anyString(), any(), eq(String.class)))
                        .thenThrow(new RuntimeException("boom")))) {

            // Act
            CustomException ex = assertThrows(CustomException.class, () -> sut.sendSmsCountry(entity, "090"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /** クラス：SmsCountryUtil toHexUtf16BE 文字列がUTF-16BEで16進変換されることを確認するテストケース */
    @Test
    void toHexUtf16BE_001() throws Exception {
        // Arrange
        Method m = SmsCountryUtil.class.getDeclaredMethod("toHexUtf16BE", String.class);
        m.setAccessible(true);

        // Act
        String hex = (String) m.invoke(null, "あ");

        // Assert
        assertEquals("3042", hex);
    }

    /** クラス：SmsCountryUtil toHexUtf16BE ASCII文字がUTF-16BEで16進変換されることを確認するテストケース */
    @Test
    void toHexUtf16BE_002() throws Exception {
        // Arrange
        Method m = SmsCountryUtil.class.getDeclaredMethod("toHexUtf16BE", String.class);
        m.setAccessible(true);

        // Act
        String hex = (String) m.invoke(null, "A");

        // Assert
        assertEquals("0041", hex);
    }
}
