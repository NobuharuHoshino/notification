
// package com.toyota.tsc.notificationhub.services;

// import com.toyota.tsc.notificationhub.commons.CommonUtil;
// import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
// import com.toyota.tsc.notificationhub.commons.LogUtil;
// import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
// import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
// import com.toyota.tsc.notificationhub.exceptions.CustomException;
// import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
// import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
// import
// com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
// import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
// import com.toyota.tsc.notificationhub.models.ResponseDto;
// import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
// import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
// import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
// import com.windowsazure.messaging.NotificationHubsException;
// import com.windowsazure.messaging.NotificationOutcome;
// import org.junit.jupiter.api.*;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.*;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.mockito.junit.jupiter.MockitoSettings;
// import org.mockito.quality.Strictness;
// import org.springframework.test.context.ActiveProfiles;

// import java.io.ByteArrayOutputStream;
// import java.io.PrintStream;
// import java.lang.reflect.Method;
// import java.sql.SQLException;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.Map;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// @SuppressWarnings("all")
// @ExtendWith(MockitoExtension.class)
// @MockitoSettings(strictness = Strictness.STRICT_STUBS)
// class SendPushServiceImplTest {

// @Mock
// private NtfInfoRepositoryIF ntfInfoRepository;
// @Mock
// private NotificationHubUtil notificationHubUtil;
// @Mock
// private PropertiesUtil properties;

// private SendPushServiceImpl sut;

// private PrintStream originalOut;
// private ByteArrayOutputStream outContent;

// @BeforeEach
// void setUp() {
// sut = new SendPushServiceImpl(ntfInfoRepository, notificationHubUtil,
// properties);
// originalOut = System.out;
// outContent = new ByteArrayOutputStream();
// System.setOut(new PrintStream(outContent));
// }

// @AfterEach
// void tearDown() {
// System.setOut(originalOut);
// }

// // =========================
// // sendPush (public)
// // =========================

// /** クラス：SendPushServiceImpl 正常系（1回で送信成功）で成功コードが返ることを確認するテストケース */
// @Test
// void sendPush_001() throws Exception {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// Map<String, Object> body = new HashMap<>();
// body.put("k", "v");

// when(header.getCorrelationId()).thenReturn("corr-001");
// when(request.getInternalUserId()).thenReturn("iu-001");
// when(request.getBody()).thenReturn(body);

// NtfInfoEntity device = mock(NtfInfoEntity.class);
// when(ntfInfoRepository.selectAllByInternalUserId("iu-001"))
// .thenReturn(Collections.singletonList(device));
// when(device.getPlatformType()).thenReturn("1"); // FCM
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// when(device.getInstallationId()).thenReturn("inst-001");
// when(device.getBrdCd()).thenReturn("0");

// when(properties.getRetryCount()).thenReturn(1);

// when(notificationHubUtil.buildFcmV1Payload(body)).thenReturn("payload");
// NotificationOutcome outcome = mock(NotificationOutcome.class);
// when(notificationHubUtil.postMessage("inst-001", "payload", "0",
// "1")).thenReturn(outcome);

// try (MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
// commonUtil.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> inv.getArgument(0));

// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> {
// return null;
// });
// logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> {
// return null;
// });
// logUtil.when(() -> LogUtil.warn(any(), anyString())).thenAnswer(inv -> {
// return null;
// });

// // Act
// ResponseDto res = sut.sendPush(request, header);

// // Assert
// assertNotNull(res);
// // 実装は CommonUtil.getResultCode("SUCCESS") を呼ぶ
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// assertEquals("SP_SUCCESS", res.getResultCode());
// }
// }

// /** クラス：SendPushServiceImpl 端末情報が空の場合にCustomExceptionとなることを確認するテストケース */
// // @Test
// // void sendPush_002() {
// // // Arrange
// // SendPushRequestDto request = mock(SendPushRequestDto.class);
// // RequestHeaderDto header = mock(RequestHeaderDto.class);

// // when(header.getCorrelationId()).thenReturn("corr-002");
// // when(request.getInternalUserId()).thenReturn("iu-002");
// // when(request.getBody()).thenReturn(Map.of("k", "v"));

// //
// when(ntfInfoRepository.selectAllByInternalUserId("iu-002")).thenReturn(Collections.emptyList());
// // // emptyで例外
// // //
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)

// // try (MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// // MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// // commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// // commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// // .thenAnswer(inv -> "MSG:" + inv.getArgument(0));

// // logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv ->
// null);
// // logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv ->
// // null);

// // // Act + Assert
// // assertThrows(CustomException.class, () -> sut.sendPush(request, header));
// // }
// // }

// /** クラス：SendPushServiceImpl 必須項目不足でTscApplicationExceptionとなることを確認するテストケース */
// @Test
// void sendPush_003() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// when(header.getCorrelationId()).thenReturn("corr-003");
// when(request.getInternalUserId()).thenReturn("");
// when(request.getBody()).thenReturn(Map.of("k", "v"));

// try (MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
// commonUtil.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> inv.getArgument(0));

// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
// logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv ->
// null);

// // Act + Assert
// assertThrows(TscApplicationException.class, () -> sut.sendPush(request,
// header)); // validateで投げる
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// }
// }

// /**
// * クラス：SendPushServiceImpl
// *
// NotificationHubsException（非Transient）でTscNotificationHubsExceptionとなることを確認するテストケース
// */
// @Test
// void sendPush_004() throws Exception {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// Map<String, Object> body = Map.of("k", "v");

// when(header.getCorrelationId()).thenReturn("corr-004");
// when(request.getInternalUserId()).thenReturn("iu-004");
// when(request.getBody()).thenReturn(body);

// NtfInfoEntity device = mock(NtfInfoEntity.class);
// when(ntfInfoRepository.selectAllByInternalUserId("iu-004"))
// .thenReturn(Collections.singletonList(device));
// when(device.getPlatformType()).thenReturn("1");
// when(device.getInstallationId()).thenReturn("inst-004");
// when(device.getBrdCd()).thenReturn("0");

// when(properties.getRetryCount()).thenReturn(1);
// when(notificationHubUtil.buildFcmV1Payload(body)).thenReturn("payload");

// NotificationHubsException ex = mock(NotificationHubsException.class);
// when(ex.isTransient()).thenReturn(false);
// when(ex.httpStatusCode()).thenReturn(500);
// when(notificationHubUtil.postMessage("inst-004", "payload", "0",
// "1")).thenThrow(ex); // executePostMessageでcatch
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)

// try (MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
// commonUtil.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> inv.getArgument(0));

// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
// logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv ->
// null);
// logUtil.when(() -> LogUtil.warn(any(), anyString())).thenAnswer(inv -> null);

// // Act + Assert
// assertThrows(TscNotificationHubsException.class, () -> sut.sendPush(request,
// header)); // 送信例外
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// }
// }

// /**
// * クラス：SendPushServiceImpl
// * NotificationHubsException（Transient→リトライ成功）で正常終了することを確認するテストケース
// */
// @Test
// void sendPush_005() throws Exception {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// Map<String, Object> body = Map.of("k", "v");

// when(header.getCorrelationId()).thenReturn("corr-005");
// when(request.getInternalUserId()).thenReturn("iu-005");
// when(request.getBody()).thenReturn(body);

// NtfInfoEntity device = mock(NtfInfoEntity.class);
// when(ntfInfoRepository.selectAllByInternalUserId("iu-005"))
// .thenReturn(Collections.singletonList(device));
// when(device.getPlatformType()).thenReturn("1");
// when(device.getInstallationId()).thenReturn("inst-005");
// when(device.getBrdCd()).thenReturn("0");

// when(properties.getRetryCount()).thenReturn(2);
// when(notificationHubUtil.buildFcmV1Payload(body)).thenReturn("payload");

// NotificationHubsException ex = mock(NotificationHubsException.class);
// when(ex.isTransient()).thenReturn(true);
// when(ex.httpStatusCode()).thenReturn(503);

// NotificationOutcome outcome = mock(NotificationOutcome.class);
// when(notificationHubUtil.postMessage("inst-005", "payload", "0", "1"))
// .thenThrow(ex)
// .thenReturn(outcome);

// try (MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
// commonUtil.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> inv.getArgument(0));

// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
// logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv ->
// null);
// logUtil.when(() -> LogUtil.warn(any(), anyString())).thenAnswer(inv -> null);

// // Act
// ResponseDto res = sut.sendPush(request, header);

// // Assert
// assertNotNull(res);
// assertEquals("SP_SUCCESS", res.getResultCode()); // 実装仕様
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// }
// }

// /** クラス：SendPushServiceImpl SQL接続エラー判定時にCustomExceptionとなることを確認するテストケース */
// @Test
// void sendPush_006() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// when(header.getCorrelationId()).thenReturn("corr-006");
// when(request.getInternalUserId()).thenReturn("iu-006");
// when(request.getBody()).thenReturn(Map.of("k", "v"));

// SQLException sqlEx = new SQLException("conn");
// when(ntfInfoRepository.selectAllByInternalUserId("iu-006")).thenThrow(new
// RuntimeException(sqlEx));

// try (MockedStatic<ExtractSqlExceptionUtil> exUtil =
// mockStatic(ExtractSqlExceptionUtil.class);
// MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// exUtil.when(() ->
// ExtractSqlExceptionUtil.findSqlException(any())).thenReturn(sqlEx);
// exUtil.when(() ->
// ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)).thenReturn(true);
// exUtil.when(() ->
// ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)).thenReturn(false);

// commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));

// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
// logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv ->
// null);

// // Act + Assert
// assertThrows(CustomException.class, () -> sut.sendPush(request, header)); //
// 接続エラーはCustomException
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// }
// }

// /** クラス：SendPushServiceImpl SQL操作エラー判定時にCustomSqlExceptionとなることを確認するテストケース */
// @Test
// void sendPush_007() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// when(header.getCorrelationId()).thenReturn("corr-007");
// when(request.getInternalUserId()).thenReturn("iu-007");
// when(request.getBody()).thenReturn(Map.of("k", "v"));

// SQLException sqlEx = new SQLException("op");
// when(ntfInfoRepository.selectAllByInternalUserId("iu-007")).thenThrow(new
// RuntimeException(sqlEx));

// try (MockedStatic<ExtractSqlExceptionUtil> exUtil =
// mockStatic(ExtractSqlExceptionUtil.class);
// MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// exUtil.when(() ->
// ExtractSqlExceptionUtil.findSqlException(any())).thenReturn(sqlEx);
// exUtil.when(() ->
// ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)).thenReturn(false);
// exUtil.when(() ->
// ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)).thenReturn(true);

// commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));

// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
// logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv ->
// null);

// // Act + Assert
// assertThrows(CustomSqlException.class, () -> sut.sendPush(request, header));
// // 操作エラーはCustomSqlException
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// }
// }

// // =========================
// // private methods via reflection
// // =========================

// /** クラス：SendPushServiceImpl validateRequiredでrequestがnullの場合の返却を確認するテストケース */
// @Test
// void validateRequired_001() {
// // Arrange
// // Act
// String res = (String) invokePrivate(sut, "validateRequired",
// new Class<?>[] { SendPushRequestDto.class },
// new Object[] { null });
// // Assert
// assertEquals("requestBody", res); // 実装
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// }

// /**
// * クラス：SendPushServiceImpl validateRequiredでinternalUserId不足の場合の返却を確認するテストケース
// */
// @Test
// void validateRequired_002() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// when(request.getInternalUserId()).thenReturn("");
// when(request.getBody()).thenReturn(Map.of("k", "v"));

// // Act
// String res = (String) invokePrivate(sut, "validateRequired",
// new Class<?>[] { SendPushRequestDto.class },
// new Object[] { request });

// // Assert
// assertEquals("internalUserId", res); // 実装
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// }

// /** クラス：SendPushServiceImpl validateRequiredでbody不足の場合の返却を確認するテストケース */
// @Test
// void validateRequired_003() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// when(request.getInternalUserId()).thenReturn("iu");
// when(request.getBody()).thenReturn(Collections.emptyMap()); // isEmpty true
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)

// // Act
// String res = (String) invokePrivate(sut, "validateRequired",
// new Class<?>[] { SendPushRequestDto.class },
// new Object[] { request });

// // Assert
// assertEquals("body", res); // 実装
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)
// }

// /** クラス：SendPushServiceImpl createPayloadでAPNの場合にAPN用payloadが返ることを確認するテストケース
// */
// @Test
// void createPayload_001() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);
// NtfInfoEntity device = mock(NtfInfoEntity.class);

// Map<String, Object> body = Map.of("k", "v");
// when(request.getBody()).thenReturn(body);
// when(device.getPlatformType()).thenReturn("2"); // APN
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/SendPushServiceImpl.java)

// when(notificationHubUtil.buildApnsPayload(body)).thenReturn("payload-apn");

// try (MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);

// // Act
// String payload = (String) invokePrivate(sut, "createPayload",
// new Class<?>[] { SendPushRequestDto.class, RequestHeaderDto.class,
// NtfInfoEntity.class },
// new Object[] { request, header, device });

// // Assert
// assertEquals("payload-apn", payload);
// }
// }

// private static Object invokePrivate(Object target, String method, Class<?>[]
// types, Object[] args) {
// try {
// Method m = target.getClass().getDeclaredMethod(method, types);
// m.setAccessible(true);
// return m.invoke(target, args);
// } catch (Exception e) {
// throw new RuntimeException(e);
// }
// }

// // =====================================================
// // 追加：validateRequired の OR（null側）を網羅
// // =====================================================

// /**
// * クラス：SendPushServiceImpl
// validateRequiredでinternalUserIdがnullの場合の返却を確認するテストケース
// */
// @Test
// void validateRequired_004() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// when(request.getInternalUserId()).thenReturn(null); // ★ null側
// when(request.getBody()).thenReturn(Map.of("k", "v"));

// // Act
// String res = (String) invokePrivate(sut, "validateRequired",
// new Class<?>[] { SendPushRequestDto.class },
// new Object[] { request });

// // Assert
// assertEquals("internalUserId", res);
// }

// /** クラス：SendPushServiceImpl validateRequiredでbodyがnullの場合の返却を確認するテストケース */
// @Test
// void validateRequired_005() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// when(request.getInternalUserId()).thenReturn("iu");
// when(request.getBody()).thenReturn(null); // ★ null側

// // Act
// String res = (String) invokePrivate(sut, "validateRequired",
// new Class<?>[] { SendPushRequestDto.class },
// new Object[] { request });

// // Assert
// assertEquals("body", res);
// }

// // =====================================================
// // 追加：createPayload switch default 分岐
// // =====================================================

// /**
// * クラス：SendPushServiceImpl
// * createPayloadでplatformTypeが不正値の場合にCustomExceptionとなることを確認するテストケース
// */
// @Test
// void createPayload_002() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);
// NtfInfoEntity device = mock(NtfInfoEntity.class);

// when(request.getInternalUserId()).thenReturn("iu");
// when(request.getBody()).thenReturn(Map.of("k", "v"));
// when(header.getCorrelationId()).thenReturn("corr");
// when(device.getPlatformType()).thenReturn("9"); // ★ default

// try (MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// // catch側ログで呼ばれる（defaultの CustomException も catch されるため）
// commonUtil.when(() -> CommonUtil.getMessage(anyString(),
// any())).thenReturn("MSG");
// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);

// // Act +
// // Assert（invokePrivateはRuntimeExceptionで包み、さらにInvocationTargetExceptionが入る）
// RuntimeException ex = assertThrows(RuntimeException.class,
// () -> invokePrivate(sut, "createPayload",
// new Class<?>[] { SendPushRequestDto.class,
// RequestHeaderDto.class, NtfInfoEntity.class },
// new Object[] { request, header, device }));

// assertTrue(ex.getCause() instanceof
// java.lang.reflect.InvocationTargetException);
// Throwable target = ((java.lang.reflect.InvocationTargetException)
// ex.getCause()).getCause();
// assertTrue(target instanceof CustomException);
// }
// }

// // =====================================================
// // 追加：executePostMessage retryCount=0（ループ未突入→null）分岐
// // =====================================================

// /**
// * クラス：SendPushServiceImpl executePostMessageでリトライ回数0の場合にnullが返ることを確認するテストケース
// */
// @Test
// void executePostMessage_001() throws NotificationHubsException {
// // Arrange
// when(properties.getRetryCount()).thenReturn(0); // ★ whileに入らない

// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);
// NtfInfoEntity device = mock(NtfInfoEntity.class);

// // Act
// NotificationOutcome outcome = (NotificationOutcome) invokePrivate(sut,
// "executePostMessage",
// new Class<?>[] { SendPushRequestDto.class, RequestHeaderDto.class,
// NtfInfoEntity.class },
// new Object[] { request, header, device });

// // Assert
// assertNull(outcome);
// verify(notificationHubUtil, never()).postMessage(anyString(), anyString(),
// anyString(), anyString());
// }

// // =====================================================
// // 追加：Transient で回数超過（cnt>=retryCount）分岐
// // =====================================================

// /**
// * クラス：SendPushServiceImpl
// *
// NotificationHubsException（Transient→回数超過）でTscNotificationHubsExceptionとなることを確認するテストケース
// */
// @Test
// void sendPush_008() throws Exception {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);
// Map<String, Object> body = Map.of("k", "v");

// when(header.getCorrelationId()).thenReturn("corr-008");
// when(request.getInternalUserId()).thenReturn("iu-008");
// when(request.getBody()).thenReturn(body);

// NtfInfoEntity device = mock(NtfInfoEntity.class);
// when(ntfInfoRepository.selectAllByInternalUserId("iu-008"))
// .thenReturn(Collections.singletonList(device));
// when(device.getPlatformType()).thenReturn("1");
// when(device.getInstallationId()).thenReturn("inst-008");
// when(device.getBrdCd()).thenReturn("0");

// when(properties.getRetryCount()).thenReturn(1); // ★ 1回で超過
// when(notificationHubUtil.buildFcmV1Payload(body)).thenReturn("payload");

// NotificationHubsException ex = mock(NotificationHubsException.class);
// when(ex.isTransient()).thenReturn(true);
// when(ex.httpStatusCode()).thenReturn(503);
// when(notificationHubUtil.postMessage("inst-008", "payload", "0",
// "1")).thenThrow(ex);

// try (MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
// commonUtil.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> inv.getArgument(0));

// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
// logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv ->
// null);
// logUtil.when(() -> LogUtil.warn(any(), anyString())).thenAnswer(inv -> null);

// // Act + Assert
// assertThrows(TscNotificationHubsException.class, () -> sut.sendPush(request,
// header));
// }
// }

// // =====================================================
// // 追加：catch(Exception) 内の分岐（sqlExあり・どちらでもない / sqlExなし）
// // =====================================================

// /**
// * クラス：SendPushServiceImpl
// * SQL例外が存在するが接続/操作どちらでもない場合にCustomExceptionとなることを確認するテストケース
// */
// @Test
// void sendPush_009() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// when(header.getCorrelationId()).thenReturn("corr-009");
// when(request.getInternalUserId()).thenReturn("iu-009");
// when(request.getBody()).thenReturn(Map.of("k", "v"));

// SQLException sqlEx = new SQLException("other");
// when(ntfInfoRepository.selectAllByInternalUserId("iu-009")).thenThrow(new
// RuntimeException(sqlEx));

// try (MockedStatic<ExtractSqlExceptionUtil> exUtil =
// mockStatic(ExtractSqlExceptionUtil.class);
// MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// exUtil.when(() ->
// ExtractSqlExceptionUtil.findSqlException(any())).thenReturn(sqlEx);
// exUtil.when(() ->
// ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)).thenReturn(false);
// exUtil.when(() ->
// ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)).thenReturn(false); // ★
// // else-if
// // false

// commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));

// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
// logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv ->
// null);

// // Act + Assert（"RS07E00001" → CustomException）
// assertThrows(CustomException.class, () -> sut.sendPush(request, header));
// }
// }

// /** クラス：SendPushServiceImpl SQL例外が存在しない場合にCustomExceptionとなることを確認するテストケース */
// @Test
// void sendPush_010() {
// // Arrange
// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// when(header.getCorrelationId()).thenReturn("corr-010");
// when(request.getInternalUserId()).thenReturn("iu-010");
// when(request.getBody()).thenReturn(Map.of("k", "v"));

// when(ntfInfoRepository.selectAllByInternalUserId("iu-010")).thenThrow(new
// RuntimeException("boom"));

// try (MockedStatic<ExtractSqlExceptionUtil> exUtil =
// mockStatic(ExtractSqlExceptionUtil.class);
// MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// exUtil.when(() ->
// ExtractSqlExceptionUtil.findSqlException(any())).thenReturn(null); // ★
// // sqlExなし

// commonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("REQ_JSON");
// commonUtil.when(() -> CommonUtil.getMessage(anyString(), any()))
// .thenAnswer(inv -> "MSG:" + inv.getArgument(0));

// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
// logUtil.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv ->
// null);

// // Act + Assert（"RS07E00001" → CustomException）
// assertThrows(CustomException.class, () -> sut.sendPush(request, header));
// }
// }

// /**
// * クラス：SendPushServiceImpl executePostMessage
// * NotificationHubsException以外の例外が発生した場合に例外がそのまま送出されることを確認するテストケース
// */
// @Test
// void executePostMessage_002() throws Exception {
// // Arrange
// when(properties.getRetryCount()).thenReturn(1); // whileに入る

// SendPushRequestDto request = mock(SendPushRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);
// NtfInfoEntity device = mock(NtfInfoEntity.class);

// Map<String, Object> body = Map.of("k", "v");
// when(request.getInternalUserId()).thenReturn("iu");
// when(request.getBody()).thenReturn(body);
// when(header.getCorrelationId()).thenReturn("corr");

// // ★ createPayload の switch 判定に必要（ここだけは必須）
// when(device.getPlatformType()).thenReturn("1"); // FCM

// // createPayload内で例外を起こす（→ createPayload catch → CustomException）
// when(notificationHubUtil.buildFcmV1Payload(body)).thenThrow(new
// RuntimeException("boom"));

// Method m = SendPushServiceImpl.class.getDeclaredMethod(
// "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
// NtfInfoEntity.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> commonUtil = mockStatic(CommonUtil.class);
// MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {

// // createPayload catch内のログで呼ばれる
// commonUtil.when(() -> CommonUtil.getMessage(anyString(),
// any())).thenReturn("MSG");
// logUtil.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);

// // Act + Assert（InvocationTargetException の cause を投げ直す）
// assertThrows(CustomException.class, () -> {
// try {
// m.invoke(sut, request, header, device);
// } catch (java.lang.reflect.InvocationTargetException ex) {
// throw (RuntimeException) ex.getCause();
// }
// });

// // NotificationHub送信（postMessage）まで到達しない
// verify(notificationHubUtil, never()).postMessage(anyString(), anyString(),
// anyString(),
// anyString());
// }
// }

// /**
// * クラス：SendPushServiceImpl getLastDataでupdatedAtの降順ソートにより最新データが返ることを確認するテストケース
// */
// @Test
// void getLastData_001() {
// // Arrange
// NtfInfoEntity newer = mock(NtfInfoEntity.class);
// NtfInfoEntity older = mock(NtfInfoEntity.class);

// java.time.LocalDateTime tNew = java.time.LocalDateTime.now();
// java.time.LocalDateTime tOld = tNew.minusMinutes(1);

// when(newer.getUpdatedAt()).thenReturn(tNew);
// when(older.getUpdatedAt()).thenReturn(tOld);

// java.util.List<NtfInfoEntity> list = java.util.List.of(older, newer); //
// わざと逆順に入れる

// // Act
// NtfInfoEntity res = (NtfInfoEntity) invokePrivate(sut, "getLastData",
// new Class<?>[] { java.util.List.class },
// new Object[] { list });

// // Assert
// assertSame(newer, res);
// }

// }
