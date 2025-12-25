// package com.toyota.tsc.notificationhub.commons;

// import com.toyota.tsc.notificationhub.exceptions.CustomException;
// import com.toyota.tsc.notificationhub.models.MailContextDto;
// import com.toyota.tsc.notificationhub.models.SmsContextDto;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.Mock;
// import org.mockito.MockedConstruction;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.http.*;
// import org.springframework.web.client.RestTemplate;

// import java.util.List;
// import java.util.Map;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// /**
// * JsapUtil のテストクラス
// */
// @SuppressWarnings("all")
// @ExtendWith(MockitoExtension.class)
// class JsapUtilTest {

// @Mock
// private PropertiesUtil propertiesUtil;

// /** クラス：JsapUtil executeGetUserId 正常にPOSTが実行されレスポンスが返ることを確認するテストケース */
// @Test
// void executeGetUserId_001() {
// // Arrange
// when(propertiesUtil.getJsapGetUserIdApiKey()).thenReturn("APIKEY");
// when(propertiesUtil.getJsapGetUserIdApiUrl()).thenReturn("https://example/jsap/userid");

// JsapUtil sut = new JsapUtil(propertiesUtil);

// ResponseEntity<String> expected = ResponseEntity.ok("OK");

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, context) -> when(mock.exchange(
// eq("https://example/jsap/userid"),
// eq(HttpMethod.POST),
// any(HttpEntity.class),
// eq(String.class))).thenReturn(expected))) {

// // Act
// ResponseEntity<String> actual = sut.executeGetUserId("internal-1", "col-1");

// // Assert
// assertSame(expected, actual);

// RestTemplate rt = mocked.constructed().get(0);
// ArgumentCaptor<HttpEntity> entityCaptor =
// ArgumentCaptor.forClass(HttpEntity.class);
// verify(rt, times(1)).exchange(eq("https://example/jsap/userid"),
// eq(HttpMethod.POST),
// entityCaptor.capture(), eq(String.class));

// HttpEntity<?> entity = entityCaptor.getValue();
// assertNotNull(entity);

// HttpHeaders headers = entity.getHeaders();
// assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
// assertEquals("APIKEY", headers.getFirst("x-api-key"));
// assertEquals("col-1", headers.getFirst("x-correlation-id"));

// @SuppressWarnings("unchecked")
// Map<String, Object> body = (Map<String, Object>) entity.getBody();
// assertEquals("internal-1", body.get("internalUserId"));
// }
// }

// /**
// * クラス：JsapUtil executeGetUserId
// * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeGetUserId_002() {
// // Arrange
// when(propertiesUtil.getJsapGetUserIdApiKey()).thenReturn("APIKEY");
// when(propertiesUtil.getJsapGetUserIdApiUrl()).thenReturn("https://example/jsap/userid");

// JsapUtil sut = new JsapUtil(propertiesUtil);

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, context) -> when(mock.exchange(anyString(), any(),
// any(HttpEntity.class), eq(String.class)))
// .thenThrow(new RuntimeException("boom")))) {

// // Act
// CustomException ex = assertThrows(CustomException.class, () ->
// sut.executeGetUserId("internal-1", "col-1"));

// // Assert
// assertNotNull(ex.getCause());
// }
// }

// /**
// * クラス：JsapUtil executeDvcLink platform=1(ANDROID)
// * の場合にplatformがandroidへ変換されPOSTされることを確認するテストケース
// */
// @Test
// void executeDvcLink_001() {
// // Arrange
// when(propertiesUtil.getJsapDvcLinkApiKey()).thenReturn("DVCKEY");
// when(propertiesUtil.getJsapDvcLinkApiUrl()).thenReturn("https://example/jsap/dvclink");

// JsapUtil sut = new JsapUtil(propertiesUtil);
// ResponseEntity<String> expected = ResponseEntity.ok("OK");

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, context) -> when(mock.exchange(
// eq("https://example/jsap/dvclink"),
// eq(HttpMethod.POST),
// any(HttpEntity.class),
// eq(String.class))).thenReturn(expected))) {

// // Act
// ResponseEntity<String> actual = sut.executeDvcLink("user-1", "token-1", "1");

// // Assert
// assertSame(expected, actual);

// RestTemplate rt = mocked.constructed().get(0);
// ArgumentCaptor<HttpEntity> entityCaptor =
// ArgumentCaptor.forClass(HttpEntity.class);
// verify(rt, times(1)).exchange(eq("https://example/jsap/dvclink"),
// eq(HttpMethod.POST),
// entityCaptor.capture(), eq(String.class));

// @SuppressWarnings("unchecked")
// Map<String, Object> body = (Map<String, Object>)
// entityCaptor.getValue().getBody();
// assertEquals("user-1", body.get("userId"));
// assertEquals("token-1", body.get("dvcToken"));
// assertEquals("android", body.get("platform"));

// HttpHeaders headers = entityCaptor.getValue().getHeaders();
// assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
// assertEquals("DVCKEY", headers.getFirst("x-api-key"));
// }
// }

// /**
// * クラス：JsapUtil executeDvcLink platform=2(IOS)
// * の場合にplatformがiosへ変換されPOSTされることを確認するテストケース
// */
// @Test
// void executeDvcLink_002() {
// // Arrange
// when(propertiesUtil.getJsapDvcLinkApiKey()).thenReturn("DVCKEY");
// when(propertiesUtil.getJsapDvcLinkApiUrl()).thenReturn("https://example/jsap/dvclink");

// JsapUtil sut = new JsapUtil(propertiesUtil);
// ResponseEntity<String> expected = ResponseEntity.ok("OK");

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, context) -> when(mock.exchange(
// eq("https://example/jsap/dvclink"),
// eq(HttpMethod.POST),
// any(HttpEntity.class),
// eq(String.class))).thenReturn(expected))) {

// // Act
// ResponseEntity<String> actual = sut.executeDvcLink("user-1", "token-1", "2");

// // Assert
// assertSame(expected, actual);

// RestTemplate rt = mocked.constructed().get(0);
// ArgumentCaptor<HttpEntity> entityCaptor =
// ArgumentCaptor.forClass(HttpEntity.class);
// verify(rt, times(1)).exchange(eq("https://example/jsap/dvclink"),
// eq(HttpMethod.POST),
// entityCaptor.capture(), eq(String.class));

// @SuppressWarnings("unchecked")
// Map<String, Object> body = (Map<String, Object>)
// entityCaptor.getValue().getBody();
// assertEquals("ios", body.get("platform"));
// }
// }

// /**
// * クラス：JsapUtil executeDvcLink 不正platformの場合にnullが返り外部呼び出しされないことを確認するテストケース
// */
// @Test
// void executeDvcLink_003() {
// // Arrange
// JsapUtil sut = new JsapUtil(propertiesUtil);

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class)) {

// // Act
// ResponseEntity<String> actual = sut.executeDvcLink("user-1", "token-1", "9");

// // Assert
// assertNull(actual);
// assertEquals(0, mocked.constructed().size());
// }
// }

// /**
// * クラス：JsapUtil executeDvcLink
// * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeDvcLink_004() {
// // Arrange
// when(propertiesUtil.getJsapDvcLinkApiKey()).thenReturn("DVCKEY");
// when(propertiesUtil.getJsapDvcLinkApiUrl()).thenReturn("https://example/jsap/dvclink");

// JsapUtil sut = new JsapUtil(propertiesUtil);

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, context) -> when(mock.exchange(anyString(), any(),
// any(HttpEntity.class), eq(String.class)))
// .thenThrow(new RuntimeException("boom")))) {

// // Act
// CustomException ex = assertThrows(CustomException.class,
// () -> sut.executeDvcLink("user-1", "token-1", "1"));

// // Assert
// assertNotNull(ex.getCause());
// }
// }

// /**
// * クラス：JsapUtil executePushRequest 正常にnoticeMethod=0でPOSTが実行されることを確認するテストケース
// */
// @Test
// void executePushRequest_001() {
// // Arrange
// when(propertiesUtil.getJsapNotificationApiKey()).thenReturn("NKEY");
// when(propertiesUtil.getJsapNotificationApiUrl()).thenReturn("https://example/jsap/notify");

// JsapUtil sut = new JsapUtil(propertiesUtil);
// ResponseEntity<String> expected = ResponseEntity.ok("OK");

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, context) -> when(mock.exchange(
// eq("https://example/jsap/notify"),
// eq(HttpMethod.POST),
// any(HttpEntity.class),
// eq(String.class))).thenReturn(expected))) {

// // Act
// ResponseEntity<String> actual = sut.executePushRequest("user-1", "{\"a\":1}",
// "AUTH");

// // Assert
// assertSame(expected, actual);

// RestTemplate rt = mocked.constructed().get(0);
// ArgumentCaptor<HttpEntity> entityCaptor =
// ArgumentCaptor.forClass(HttpEntity.class);
// verify(rt, times(1)).exchange(eq("https://example/jsap/notify"),
// eq(HttpMethod.POST),
// entityCaptor.capture(), eq(String.class));

// HttpHeaders headers = entityCaptor.getValue().getHeaders();
// assertEquals("NKEY", headers.getFirst("x-api-key"));
// assertEquals("AUTH", headers.getFirst("auth-token"));

// @SuppressWarnings("unchecked")
// Map<String, Object> body = (Map<String, Object>)
// entityCaptor.getValue().getBody();
// assertEquals("user-1", body.get("userId"));
// assertEquals("0", body.get("noticeMethod"));
// assertEquals("{\"a\":1}", body.get("payload"));
// }
// }

// /**
// * クラス：JsapUtil executePushRequest
// * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void executePushRequest_002() {
// // Arrange
// when(propertiesUtil.getJsapNotificationApiKey()).thenReturn("NKEY");
// when(propertiesUtil.getJsapNotificationApiUrl()).thenReturn("https://example/jsap/notify");

// JsapUtil sut = new JsapUtil(propertiesUtil);

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, context) -> when(mock.exchange(anyString(), any(),
// any(HttpEntity.class), eq(String.class)))
// .thenThrow(new RuntimeException("boom")))) {

// // Act
// CustomException ex = assertThrows(CustomException.class,
// () -> sut.executePushRequest("user-1", "p", "AUTH"));

// // Assert
// assertNotNull(ex.getCause());
// }
// }

// /** クラス：JsapUtil executeSendMessage 正常にPOSTされbody項目が設定されることを確認するテストケース */
// @Test
// void executeSendMessage_001() {
// // Arrange
// when(propertiesUtil.getJsapNotificationApiKey()).thenReturn("NKEY");
// when(propertiesUtil.getJsapNotificationApiUrl()).thenReturn("https://example/jsap/notify");

// JsapUtil sut = new JsapUtil(propertiesUtil);
// ResponseEntity<String> expected = ResponseEntity.ok("OK");

// Object context = Map.of("k", "v");

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, c) -> when(mock.exchange(eq("https://example/jsap/notify"),
// eq(HttpMethod.POST),
// any(HttpEntity.class), eq(String.class)))
// .thenReturn(expected))) {

// // Act
// ResponseEntity<String> actual = sut.executeSendMessage("proc-1", "user-1",
// "1", "title", context, "AUTH");

// // Assert
// assertSame(expected, actual);

// RestTemplate rt = mocked.constructed().get(0);
// ArgumentCaptor<HttpEntity> entityCaptor =
// ArgumentCaptor.forClass(HttpEntity.class);
// verify(rt, times(1)).exchange(eq("https://example/jsap/notify"),
// eq(HttpMethod.POST),
// entityCaptor.capture(), eq(String.class));

// @SuppressWarnings("unchecked")
// Map<String, Object> body = (Map<String, Object>)
// entityCaptor.getValue().getBody();
// assertEquals("proc-1", body.get("processID"));
// assertEquals("user-1", body.get("userId"));
// assertEquals("1", body.get("noticeMethod "));
// assertEquals("title", body.get("title"));
// assertSame(context, body.get("context"));

// HttpHeaders headers = entityCaptor.getValue().getHeaders();
// assertEquals("NKEY", headers.getFirst("x-api-key"));
// assertEquals("AUTH", headers.getFirst("auth-token"));
// }
// }

// /**
// * クラス：JsapUtil executeSendMessage
// * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeSendMessage_002() {
// // Arrange
// when(propertiesUtil.getJsapNotificationApiKey()).thenReturn("NKEY");
// when(propertiesUtil.getJsapNotificationApiUrl()).thenReturn("https://example/jsap/notify");

// JsapUtil sut = new JsapUtil(propertiesUtil);

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, c) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
// eq(String.class)))
// .thenThrow(new RuntimeException("boom")))) {

// // Act
// CustomException ex = assertThrows(CustomException.class,
// () -> sut.executeSendMessage("p", "u", "1", "t", Map.of(), "AUTH"));

// // Assert
// assertNotNull(ex.getCause());
// }
// }

// /** クラス：JsapUtil executeGetUserInfo 正常にPOSTが実行されることを確認するテストケース */
// @Test
// void executeGetUserInfo_001() {
// // Arrange
// when(propertiesUtil.getJsapGetUserInfoApiKey()).thenReturn("UIKEY");
// when(propertiesUtil.getJsapGetUserInfoApiUrl()).thenReturn("https://example/jsap/userinfo");

// JsapUtil sut = new JsapUtil(propertiesUtil);
// ResponseEntity<String> expected = ResponseEntity.ok("OK");

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, c) -> when(mock.exchange(eq("https://example/jsap/userinfo"),
// eq(HttpMethod.POST),
// any(HttpEntity.class), eq(String.class)))
// .thenReturn(expected))) {

// // Act
// ResponseEntity<String> actual = sut.executeGetUserInfo("user-1");

// // Assert
// assertSame(expected, actual);

// RestTemplate rt = mocked.constructed().get(0);
// ArgumentCaptor<HttpEntity> entityCaptor =
// ArgumentCaptor.forClass(HttpEntity.class);
// verify(rt, times(1)).exchange(eq("https://example/jsap/userinfo"),
// eq(HttpMethod.POST),
// entityCaptor.capture(), eq(String.class));

// HttpHeaders headers = entityCaptor.getValue().getHeaders();
// assertEquals("UIKEY", headers.getFirst("x-api-key"));

// @SuppressWarnings("unchecked")
// Map<String, Object> body = (Map<String, Object>)
// entityCaptor.getValue().getBody();
// assertEquals("user-1", body.get("userId"));
// }
// }

// /**
// * クラス：JsapUtil executeGetUserInfo
// * RestTemplate例外時にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void executeGetUserInfo_002() {
// // Arrange
// when(propertiesUtil.getJsapGetUserInfoApiKey()).thenReturn("UIKEY");
// when(propertiesUtil.getJsapGetUserInfoApiUrl()).thenReturn("https://example/jsap/userinfo");

// JsapUtil sut = new JsapUtil(propertiesUtil);

// try (MockedConstruction<RestTemplate> mocked =
// mockConstruction(RestTemplate.class,
// (mock, c) -> when(mock.exchange(anyString(), any(), any(HttpEntity.class),
// eq(String.class)))
// .thenThrow(new RuntimeException("boom")))) {

// // Act
// CustomException ex = assertThrows(CustomException.class, () ->
// sut.executeGetUserInfo("user-1"));

// // Assert
// assertNotNull(ex.getCause());
// }
// }

// /** クラス：JsapUtil createSmsContext 正常にSmsContextDtoが生成されることを確認するテストケース */
// @Test
// void createSmsContext_001() {
// // Arrange
// JsapUtil sut = new JsapUtil(propertiesUtil);

// // Act
// SmsContextDto dto = sut.createSmsContext("body");

// // Assert
// assertNotNull(dto);
// }

// /**
// * クラス：JsapUtil createSmsContext bodyがnullでもSmsContextDtoが生成されることを確認するテストケース
// */
// @Test
// void createSmsContext_002() {
// // Arrange
// JsapUtil sut = new JsapUtil(propertiesUtil);

// // Act
// SmsContextDto dto = sut.createSmsContext(null);

// // Assert
// assertNotNull(dto);
// }

// /**
// * クラス：JsapUtil createMailContext 正常に2要素のMailContextDtoリストが生成されることを確認するテストケース
// */
// @Test
// void createMailContext_001() {
// // Arrange
// JsapUtil sut = new JsapUtil(propertiesUtil);

// // Act
// List<MailContextDto> list = sut.createMailContext("text", "<b>html</b>");

// // Assert
// assertNotNull(list);
// assertEquals(2, list.size());
// assertNotNull(list.get(0));
// assertNotNull(list.get(1));
// }

// /**
// * クラス：JsapUtil createMailContext
// * 引数がnullでも2要素のMailContextDtoリストが生成されることを確認するテストケース
// */
// @Test
// void createMailContext_002() {
// // Arrange
// JsapUtil sut = new JsapUtil(propertiesUtil);

// // Act
// List<MailContextDto> list = sut.createMailContext(null, null);

// // Assert
// assertNotNull(list);
// assertEquals(2, list.size());
// }
// }
