package com.toyota.tsc.notificationhub.commons;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.PersonalInfoListRequestDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
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

        try (MockedConstruction<RestTemplate> mocked =
        mockConstruction(RestTemplate.class,
        (mock, ctx) -> when(mock.exchange(
                eq("https://example/personalinfo"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class))).thenReturn(response))) {

            // Act
            PersonalInfoResponseDto dto = sut.getPersonalInfoApiResponse("internal-1",
            "col-1");

            // Assert
            assertNotNull(dto);

            RestTemplate rt = mocked.constructed().get(0);
            ArgumentCaptor<HttpEntity> entityCaptor =
            ArgumentCaptor.forClass(HttpEntity.class);
            verify(rt, times(1)).exchange(eq("https://example/personalinfo"),
            eq(HttpMethod.POST),
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

        try (MockedConstruction<RestTemplate> mocked =
        mockConstruction(RestTemplate.class,
        (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
        eq(String.class)))
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

        ResponseEntity<String> response =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"e\":1}");

        try (MockedConstruction<RestTemplate> mocked =
        mockConstruction(RestTemplate.class,
        (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
        eq(String.class)))
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

        try (MockedConstruction<RestTemplate> mocked =
        mockConstruction(RestTemplate.class,
        (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
        eq(String.class)))
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

        try (MockedConstruction<RestTemplate> mocked =
        mockConstruction(RestTemplate.class,
        (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
        eq(String.class)))
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

        try (MockedConstruction<RestTemplate> mocked =
        mockConstruction(RestTemplate.class,
        (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
        eq(String.class)))
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

        try (MockedConstruction<RestTemplate> mocked =
        mockConstruction(RestTemplate.class,
        (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
        eq(String.class)))
                .thenReturn(null))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
            () -> sut.getPersonalInfoApiResponse("internal-1", "col-1"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoListApiResponse
     * 正常にレスポンスが返ることを確認するテストケース
     */
    @Test
    void getPersonalInfoListApiResponse_001() {
        // Arrange
        when(propertiesUtil.getPersonalInfoListApiKey()).thenReturn("PIL-KEY");
        when(propertiesUtil.getPersonalInfoListApiUrl()).thenReturn("https://example/personalinfolist");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);
        PersonalInfoListRequestDto request = new PersonalInfoListRequestDto(
                "2", "", "", List.of("user001", "user002", "user003", "user004", "user005"));

        String json = "{\"resultList\":[{\"internalUserId\":\"user001\"},{\"internalUserId\":\"user005\"}]}";
        ResponseEntity<String> expectedResponse = ResponseEntity.ok(json);

        try (MockedConstruction<RestTemplate> mocked =
        mockConstruction(RestTemplate.class,
        (mock, ctx) -> when(mock.exchange(
                eq("https://example/personalinfolist"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class))).thenReturn(expectedResponse))) {

            // Act
            ResponseEntity<String> response = sut.getPersonalInfoListApiResponse(request, "col-1");

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().contains("user001"));
            assertTrue(response.getBody().contains("user005"));
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoListApiResponse
     * 単一ユーザーで正常に動作することを確認するテストケース
     */
    @Test
    void getPersonalInfoListApiResponse_002() {
        // Arrange
        when(propertiesUtil.getPersonalInfoListApiKey()).thenReturn("PIL-KEY");
        when(propertiesUtil.getPersonalInfoListApiUrl()).thenReturn("https://example/personalinfolist");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);
        PersonalInfoListRequestDto request = new PersonalInfoListRequestDto(
                "2", "", "", List.of("user001"));

        String json = "{\"resultList\":[{\"internalUserId\":\"user001\"}]}";
        ResponseEntity<String> expectedResponse = ResponseEntity.ok(json);

        try (MockedConstruction<RestTemplate> mocked =
        mockConstruction(RestTemplate.class,
        (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
        eq(String.class)))
                .thenReturn(expectedResponse))) {

            // Act
            ResponseEntity<String> response = sut.getPersonalInfoListApiResponse(request, "col-1");

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoListApiResponse
     * 例外発生時にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void getPersonalInfoListApiResponse_003() {
        // Arrange
        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);

        // Act & Assert
        assertThrows(CustomException.class,
        () -> sut.getPersonalInfoListApiResponse(null, "col-1"));
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoApiResponse
     * HttpStatusCodeException発生時に再スローされることを確認するテストケース
     */
    @Test
    void getPersonalInfoApiResponse_008() {
        // Arrange
        when(propertiesUtil.getPersonalInfoApiKey()).thenReturn("PI-KEY");
        when(propertiesUtil.getPersonalInfoApiUrl()).thenReturn("https://example/personalinfo");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);
        HttpClientErrorException expected = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenThrow(expected))) {
            // Act & Assert
            HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                    () -> sut.getPersonalInfoApiResponse("internal-1", "col-1"));
            assertSame(expected, ex);
        }
    }

    /**
     * クラス：PersonalInfoUtil getPersonalInfoListApiResponse
     * HttpStatusCodeException発生時に再スローされることを確認するテストケース
     */
    @Test
    void getPersonalInfoListApiResponse_004() {
        // Arrange
        when(propertiesUtil.getPersonalInfoListApiKey()).thenReturn("PIL-KEY");
        when(propertiesUtil.getPersonalInfoListApiUrl()).thenReturn("https://example/personalinfolist");

        PersonalInfoUtil sut = new PersonalInfoUtil(propertiesUtil);
        HttpClientErrorException expected = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, ctx) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class)))
                        .thenThrow(expected))) {
            // Act & Assert
            HttpClientErrorException ex = assertThrows(HttpClientErrorException.class,
                    () -> sut.getPersonalInfoListApiResponse(new PersonalInfoListRequestDto("1", "D001", "DL001", List.of("user-1")), "col-1"));
            assertSame(expected, ex);
        }
    }

}
