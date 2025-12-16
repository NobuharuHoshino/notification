
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/commons/PersonalInfoUtilTest.java
package com.toyota.tsc.notificationhub.commons;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PersonalInfoUtil のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class PersonalInfoUtilTest {

    @Mock
    private PropertiesUtil propertiesUtil;

    /**
     * クラス：PersonalInfoUtil getPersonalInfoApiResponse 正常（2xx +
     * 正常JSON）でDTOが返ることを確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_001() {
        // Arrange
        when(propertiesUtil.getPersonalInfoApiKey()).thenReturn("PI-KEY");
        when(propertiesUtil.getPersonalInfoApiUrl()).thenReturn("https://example/personalinfo");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);

        String json = "{}";
        ResponseEntity<String> response = ResponseEntity.ok(json);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(
                        eq("https://example/personalinfo"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class))).thenReturn(response))) {

            // Act
            PersonalInfoResponseDto dto = sut.getPersonalInfoApiResponse("internal-1", "col-1");

            // Assert
            assertNotNull(dto);

            RestTemplate rt = mocked.constructed().get(0);
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(rt, times(1)).exchange(eq("https://example/personalinfo"), eq(HttpMethod.POST),
                    entityCaptor.capture(), eq(String.class));

            HttpEntity<?> entity = entityCaptor.getValue();
            HttpHeaders headers = entity.getHeaders();
            assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
            assertEquals("PI-KEY", headers.getFirst("x-api-key"));
            assertEquals("col-1", headers.getFirst("x-correlation-id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) entity.getBody();
            assertEquals("internal-1", body.get("internalUserId"));
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoApiResponse
     * internalUserIdがnullでも2xxならDTOが返ることを確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_002() {
        // Arrange
        when(propertiesUtil.getPersonalInfoApiKey()).thenReturn("PI-KEY");
        when(propertiesUtil.getPersonalInfoApiUrl()).thenReturn("https://example/personalinfo");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);

        String json = "{}";
        ResponseEntity<String> response = ResponseEntity.ok(json);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(response))) {

            // Act
            PersonalInfoResponseDto dto = sut.getPersonalInfoApiResponse(null, "col-1");

            // Assert
            assertNotNull(dto);
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoApiResponse
     * 非2xxの場合にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_003() {
        // Arrange
        when(propertiesUtil.getPersonalInfoApiKey()).thenReturn("PI-KEY");
        when(propertiesUtil.getPersonalInfoApiUrl()).thenReturn("https://example/personalinfo");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);

        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"e\":1}");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(response))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.getPersonalInfoApiResponse("internal-1", "col-1"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoApiResponse
     * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_004() {
        // Arrange
        when(propertiesUtil.getPersonalInfoApiKey()).thenReturn("PI-KEY");
        when(propertiesUtil.getPersonalInfoApiUrl()).thenReturn("https://example/personalinfo");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenThrow(new RuntimeException("boom")))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.getPersonalInfoApiResponse("internal-1", "col-1"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoApiResponse
     * レスポンスbodyが不正JSONの場合にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_005() {
        // Arrange
        when(propertiesUtil.getPersonalInfoApiKey()).thenReturn("PI-KEY");
        when(propertiesUtil.getPersonalInfoApiUrl()).thenReturn("https://example/personalinfo");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);

        ResponseEntity<String> response = ResponseEntity.ok("{"); // invalid json

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(response))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.getPersonalInfoApiResponse("internal-1", "col-1"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoApiResponse
     * レスポンスbodyがnullの場合にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_006() {
        // Arrange
        when(propertiesUtil.getPersonalInfoApiKey()).thenReturn("PI-KEY");
        when(propertiesUtil.getPersonalInfoApiUrl()).thenReturn("https://example/personalinfo");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);

        ResponseEntity<String> response = ResponseEntity.ok(null);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(response))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.getPersonalInfoApiResponse("internal-1", "col-1"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoApiResponse
     * exchangeがnullを返す場合にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_007() {
        // Arrange
        when(propertiesUtil.getPersonalInfoApiKey()).thenReturn("PI-KEY");
        when(propertiesUtil.getPersonalInfoApiUrl()).thenReturn("https://example/personalinfo");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenReturn(null))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.getPersonalInfoApiResponse("internal-1", "col-1"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

}
