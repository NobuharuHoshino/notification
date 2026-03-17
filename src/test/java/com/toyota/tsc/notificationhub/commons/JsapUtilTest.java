package com.toyota.tsc.notificationhub.commons;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.models.MailContextDto;
import com.toyota.tsc.notificationhub.models.SmsContextDto;

import jp.toyota.res.common.utils.ALJToken;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JsapUtil のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class JsapUtilTest {

    @Mock
    private PropertiesUtil propertiesUtil;
    @Mock
    private ALJToken aljToken;

    /** クラス：JsapUtil executeGetUserId 正常にPOSTが実行されレスポンスが返ることを確認するテストケース */
    @Test
    void executeGetUserId_001() {
        // Arrange
        when(propertiesUtil.getJsapGetUserIdApiUrl()).thenReturn("https://example/jsap/userid");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        ResponseEntity<String> expected = ResponseEntity.ok("OK");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(
                        eq("https://example/jsap/userid"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class))).thenReturn(expected))) {

            // Act
            ResponseEntity<String> actual = sut.executeGetUserId("internal-1", "col-1");

            // Assert
            assertSame(expected, actual);

            RestTemplate rt = mocked.constructed().get(0);
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(rt, times(1)).exchange(eq("https://example/jsap/userid"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(), eq(String.class));

            HttpEntity<?> entity = entityCaptor.getValue();
            assertNotNull(entity);

            HttpHeaders headers = entity.getHeaders();
            assertEquals("col-1", headers.getFirst("x-correlation-id"));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) entity.getBody();
            assertEquals("internal-1", body.get("internaluserId"));
        }
    }

    /**
     * クラス：JsapUtil executeGetUserId
     * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void executeGetUserId_002() {
        // Arrange
        when(propertiesUtil.getJsapGetUserIdApiUrl()).thenReturn("https://example/jsap/userid");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(anyString(), any(),
                        any(HttpEntity.class), eq(String.class)))
                        .thenThrow(new RuntimeException("boom")))) {

            // Act
            CustomException ex = assertThrows(CustomException.class, () -> sut.executeGetUserId("internal-1", "col-1"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /**
     * クラス：JsapUtil executeDvcLink platform=1(ANDROID)
     * の場合にplatformがandroidへ変換されPOSTされることを確認するテストケース
     */
    @Test
    void executeDvcLink_001() {
        // Arrange
        when(propertiesUtil.getJsapDvcLinkApiUrl()).thenReturn("https://example/jsap/dvclink");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);
        ResponseEntity<String> expected = ResponseEntity.ok("OK");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(
                        eq("https://example/jsap/dvclink"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class))).thenReturn(expected))) {

            // Act
            ResponseEntity<String> actual = sut.executeDvcLink("user-1", "token-1", "1", "test-token");

            // Assert
            assertSame(expected, actual);

            RestTemplate rt = mocked.constructed().get(0);
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(rt, times(1)).exchange(eq("https://example/jsap/dvclink"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(), eq(String.class));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
            assertEquals("user-1", body.get("userId"));
            assertEquals("token-1", body.get("deviceToken"));
            assertEquals("android", body.get("osType"));

            HttpHeaders headers = entityCaptor.getValue().getHeaders();
            assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
            assertEquals("bearer test-token", headers.getFirst("authorization"));
        }
    }

    /**
     * クラス：JsapUtil executeDvcLink platform=2(IOS)
     * の場合にplatformがiosへ変換されPOSTされることを確認するテストケース
     */
    @Test
    void executeDvcLink_002() {
        // Arrange
        when(propertiesUtil.getJsapDvcLinkApiUrl()).thenReturn("https://example/jsap/dvclink");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);
        ResponseEntity<String> expected = ResponseEntity.ok("OK");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(
                        eq("https://example/jsap/dvclink"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class))).thenReturn(expected))) {

            // Act
            ResponseEntity<String> actual = sut.executeDvcLink("user-1", "token-1", "2", "test-token");

            // Assert
            assertSame(expected, actual);

            RestTemplate rt = mocked.constructed().get(0);
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(rt, times(1)).exchange(eq("https://example/jsap/dvclink"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(), eq(String.class));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
            assertEquals("ios", body.get("osType"));
        }
    }

    /**
     * クラス：JsapUtil executeDvcLink 不正platformの場合にnullが返り外部呼び出しされないことを確認するテストケース
     */
    @Test
    void executeDvcLink_003() {
        // Arrange
        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class)) {

            // Act
            ResponseEntity<String> actual = sut.executeDvcLink("user-1", "token-1", "9", "test-token");

            // Assert
            assertNull(actual);
            assertEquals(0, mocked.constructed().size());
        }
    }

    /**
     * クラス：JsapUtil executeDvcLink
     * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void executeDvcLink_004() {
        // Arrange
        when(propertiesUtil.getJsapDvcLinkApiUrl()).thenReturn("https://example/jsap/dvclink");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(anyString(), any(),
                        any(HttpEntity.class), eq(String.class)))
                        .thenThrow(new RuntimeException("boom")))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.executeDvcLink("user-1", "token-1", "1", "test-token"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /**
     * クラス：JsapUtil executePushRequest 正常にnoticeMethod=0でPOSTが実行されることを確認するテストケース
     */
    @Test
    void executePushRequest_001() {
        // Arrange
        when(propertiesUtil.getJsapNotificationApiUrl()).thenReturn("https://example/jsap/notify");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);
        ResponseEntity<String> expected = ResponseEntity.ok("OK");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(
                        eq("https://example/jsap/notify"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class))).thenReturn(expected))) {

            // Act
            ResponseEntity<String> actual = sut.executePushRequest("user-1", "{\"a\":1}",
                    "AUTH");

            // Assert
            assertSame(expected, actual);

            RestTemplate rt = mocked.constructed().get(0);
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(rt, times(1)).exchange(eq("https://example/jsap/notify"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(), eq(String.class));

            HttpHeaders headers = entityCaptor.getValue().getHeaders();
            assertEquals("bearer AUTH", headers.getFirst("authorization"));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
            assertEquals("user-1", body.get("userId"));
            assertEquals("0", body.get("sendMethod"));
            assertEquals("{\"a\":1}", body.get("payload"));
        }
    }

    /**
     * クラス：JsapUtil executePushRequest
     * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void executePushRequest_002() {
        // Arrange
        when(propertiesUtil.getJsapNotificationApiUrl()).thenReturn("https://example/jsap/notify");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(anyString(), any(),
                        any(HttpEntity.class), eq(String.class)))
                        .thenThrow(new RuntimeException("boom")))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.executePushRequest("user-1", "p", "AUTH"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /** クラス：JsapUtil executeSendMessage 正常にPOSTされbody項目が設定されることを確認するテストケース */
    @Test
    void executeSendMessage_001() {
        // Arrange
        when(propertiesUtil.getJsapNotificationApiUrl()).thenReturn("https://example/jsap/notify");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);
        ResponseEntity<String> expected = ResponseEntity.ok("OK");

        Object context = Map.of("k", "v");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, c) -> when(mock.exchange(eq("https://example/jsap/notify"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class), eq(String.class)))
                        .thenReturn(expected))) {

            // Act
            ResponseEntity<String> actual = sut.executeSendMessage("proc-1", "user-1",
                    "1", "title", context, "AUTH");

            // Assert
            assertSame(expected, actual);

            RestTemplate rt = mocked.constructed().get(0);
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(rt, times(1)).exchange(eq("https://example/jsap/notify"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(), eq(String.class));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
            assertEquals("proc-1", body.get("processID"));
            assertEquals("user-1", body.get("userId"));
            assertEquals("1", body.get("sendMethod "));
            assertEquals("title", body.get("title"));
            assertSame(context, body.get("context"));

            HttpHeaders headers = entityCaptor.getValue().getHeaders();
            assertEquals("bearer AUTH", headers.getFirst("authorization"));
        }
    }

    /**
     * クラス：JsapUtil executeSendMessage
     * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void executeSendMessage_002() {
        // Arrange
        when(propertiesUtil.getJsapNotificationApiUrl()).thenReturn("https://example/jsap/notify");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, c) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
                        eq(String.class)))
                        .thenThrow(new RuntimeException("boom")))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.executeSendMessage("p", "u", "1", "t", Map.of(), "AUTH"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /** クラス：JsapUtil executeGetToken 正常にALJTokenが取得されることを確認するテストケース */
    @Test
    void executeGetToken_001() throws Exception {
        // Arrange
        jp.toyota.res.common.auth.GetALJTokenResultDto expected =
                mock(jp.toyota.res.common.auth.GetALJTokenResultDto.class);
        when(aljToken.getALJToken()).thenReturn(expected);

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        // Act
        jp.toyota.res.common.auth.GetALJTokenResultDto actual = sut.executeGetToken();

        // Assert
        assertSame(expected, actual);
        verify(aljToken, times(1)).getALJToken();
    }

    /**
     * クラス：JsapUtil executeGetToken result=trueのとき requestUpdateALJTokenが呼ばれないことを確認するテストケース
     */
    @Test
    void executeGetToken_003() throws Exception {
        // Arrange
        jp.toyota.res.common.auth.GetALJTokenResultDto expected =
                mock(jp.toyota.res.common.auth.GetALJTokenResultDto.class);
        when(expected.getResult()).thenReturn(true);
        when(aljToken.getALJToken()).thenReturn(expected);

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        // Act
        jp.toyota.res.common.auth.GetALJTokenResultDto actual = sut.executeGetToken();

        // Assert
        assertSame(expected, actual);
        verify(aljToken, never()).requestUpdateALJToken();
    }

    /**
     * クラス：JsapUtil executeGetToken
     * ALJToken例外時にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void executeGetToken_002() throws Exception {
        // Arrange
        when(aljToken.getALJToken()).thenThrow(new RuntimeException("alj-error"));

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        // Act
        CustomException ex = assertThrows(CustomException.class, () -> sut.executeGetToken());

        // Assert
        assertNotNull(ex.getCause());
        verify(aljToken, times(1)).getALJToken();
    }

    /** クラス：JsapUtil executeGetUserInfo 正常にPOSTが実行されレスポンスが返ることを確認するテストケース */
    @Test
    void executeGetUserInfo_001() {
        // Arrange
        when(propertiesUtil.getJsapGetUserInfoApiUrl()).thenReturn("https://example/jsap/userinfo");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);
        ResponseEntity<String> expected = ResponseEntity.ok("OK");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(
                        eq("https://example/jsap/userinfo"),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class))).thenReturn(expected))) {

            // Act
            ResponseEntity<String> actual = sut.executeGetUserInfo("user-1", "AUTH");

            // Assert
            assertSame(expected, actual);

            RestTemplate rt = mocked.constructed().get(0);
            ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(rt, times(1)).exchange(eq("https://example/jsap/userinfo"),
                    eq(HttpMethod.POST),
                    entityCaptor.capture(), eq(String.class));

            HttpHeaders headers = entityCaptor.getValue().getHeaders();
            assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
            assertEquals("bearer AUTH", headers.getFirst("authorization"));

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
            assertEquals("user-1", body.get("userId"));
        }
    }

    /**
     * クラス：JsapUtil executeGetUserInfo
     * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
     */
    @Test
    void executeGetUserInfo_002() {
        // Arrange
        when(propertiesUtil.getJsapGetUserInfoApiUrl()).thenReturn("https://example/jsap/userinfo");

        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (mock, context) -> when(mock.exchange(anyString(), any(),
                        any(HttpEntity.class), eq(String.class)))
                        .thenThrow(new RuntimeException("boom")))) {

            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.executeGetUserInfo("user-1", "AUTH"));

            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /** クラス：JsapUtil createSmsContext 正常にSmsContextDtoが生成されることを確認するテストケース */
    @Test
    void createSmsContext_001() {
        // Arrange
        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        // Act
        SmsContextDto dto = sut.createSmsContext("body");

        // Assert
        assertNotNull(dto);
    }

    /**
     * クラス：JsapUtil createSmsContext bodyがnullでもSmsContextDtoが生成されることを確認するテストケース
     */
    @Test
    void createSmsContext_002() {
        // Arrange
        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        // Act
        SmsContextDto dto = sut.createSmsContext(null);

        // Assert
        assertNotNull(dto);
    }

    /**
     * クラス：JsapUtil createMailContext 正常に2要素のMailContextDtoリストが生成されることを確認するテストケース
     */
    @Test
    void createMailContext_001() {
        // Arrange
        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        // Act
        List<MailContextDto> list = sut.createMailContext("text", "<b>html</b>");

        // Assert
        assertNotNull(list);
        assertEquals(2, list.size());
        assertNotNull(list.get(0));
        assertNotNull(list.get(1));
    }

    /**
     * クラス：JsapUtil createMailContext
     * 引数がnullでも2要素のMailContextDtoリストが生成されることを確認するテストケース
     */
    @Test
    void createMailContext_002() {
        // Arrange
        JsapUtil sut = new JsapUtil(propertiesUtil, aljToken);

        // Act
        List<MailContextDto> list = sut.createMailContext(null, null);

        // Assert
        assertNotNull(list);
        assertEquals(2, list.size());
    }
}
