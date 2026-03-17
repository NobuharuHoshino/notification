package com.toyota.tsc.notificationhub.commons;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

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

    /** クラス：SmsCountryUtil executeSendSms TOYOTAブランド(1)でSMSが送信されることを確認するテストケース */
    @Test
    void executeSendSms_001() {
        // Arrange
        when(propertiesUtil.getSenderIdToyota()).thenReturn("SIDT");
        when(propertiesUtil.getSmsCountryApiUrl()).thenReturn("https://example/sms");
        when(propertiesUtil.getSmsCountryUser()).thenReturn("user");
        when(propertiesUtil.getSmsCountryPass()).thenReturn("pass");
        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);
        ResponseEntity<String> expected = ResponseEntity.ok("OK");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.getForEntity(any(URI.class), eq(String.class)))
                        .thenReturn(expected))) {
            // Act
            ResponseEntity<String> actual = sut.executeSendSms("819012345678", "hello", "1");
            // Assert
            assertSame(expected, actual);
        }
    }

    /** クラス：SmsCountryUtil executeSendSms 無効ブランド(0)でもTOYOTA扱いで送信されることを確認するテストケース */
    @Test
    void executeSendSms_002() {
        // Arrange
        when(propertiesUtil.getSenderIdToyota()).thenReturn("SIDT");
        when(propertiesUtil.getSmsCountryApiUrl()).thenReturn("https://example/sms");
        when(propertiesUtil.getSmsCountryUser()).thenReturn("user");
        when(propertiesUtil.getSmsCountryPass()).thenReturn("pass");
        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);
        ResponseEntity<String> expected = ResponseEntity.ok("OK");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.getForEntity(any(URI.class), eq(String.class)))
                        .thenReturn(expected))) {
            // Act
            ResponseEntity<String> actual = sut.executeSendSms("1", "msg", "0");
            // Assert
            assertNotNull(actual);
        }
    }

    /** クラス：SmsCountryUtil executeSendSms LEXUSブランド(2)でSMSが送信されることを確認するテストケース */
    @Test
    void executeSendSms_003() {
        // Arrange
        when(propertiesUtil.getSenderIdLexus()).thenReturn("SIDL");
        when(propertiesUtil.getSmsCountryApiUrl()).thenReturn("https://example/sms");
        when(propertiesUtil.getSmsCountryUser()).thenReturn("user");
        when(propertiesUtil.getSmsCountryPass()).thenReturn("pass");
        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);
        ResponseEntity<String> expected = ResponseEntity.ok("OK");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.getForEntity(any(URI.class), eq(String.class)))
                        .thenReturn(expected))) {
            // Act
            ResponseEntity<String> actual = sut.executeSendSms("1", "msg", "2");
            // Assert
            assertNotNull(actual);
        }
    }

    /** クラス：SmsCountryUtil executeSendSms 不正ブランドの場合にnullが返ることを確認するテストケース */
    @Test
    void executeSendSms_004() {
        // Arrange
        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);
        // Act
        ResponseEntity<String> actual = sut.executeSendSms("1", "msg", "9");
        // Assert
        assertNull(actual);
    }

    /** クラス：SmsCountryUtil executeSendSms 例外発生時にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void executeSendSms_005() {
        // Arrange
        when(propertiesUtil.getSenderIdToyota()).thenReturn("SIDT");
        when(propertiesUtil.getSmsCountryApiUrl()).thenReturn("https://example/sms");
        when(propertiesUtil.getSmsCountryUser()).thenReturn("user");
        when(propertiesUtil.getSmsCountryPass()).thenReturn("pass");
        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.getForEntity(any(URI.class), eq(String.class)))
                        .thenThrow(new RuntimeException("boom")))) {
            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.executeSendSms("1", "msg", "1"));
            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /**
     * クラス：SmsCountryUtil executeSendSms
     * HttpStatusCodeException発生時に再スローされることを確認するテストケース
     */
    @Test
    void executeSendSms_006() {
        // Arrange
        when(propertiesUtil.getSenderIdToyota()).thenReturn("SIDT");
        when(propertiesUtil.getSmsCountryApiUrl()).thenReturn("https://example/sms");
        when(propertiesUtil.getSmsCountryUser()).thenReturn("user");
        when(propertiesUtil.getSmsCountryPass()).thenReturn("pass");
        SmsCountryUtil sut = new SmsCountryUtil(propertiesUtil);
        HttpClientErrorException expected = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.getForEntity(any(URI.class), eq(String.class)))
                        .thenThrow(expected))) {
            // Act & Assert
            HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                    () -> sut.executeSendSms("1", "msg", "1"));
            assertSame(expected, ex);
        }
    }
}
