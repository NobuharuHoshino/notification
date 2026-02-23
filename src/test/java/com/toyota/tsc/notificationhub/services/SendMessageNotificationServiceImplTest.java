// package com.toyota.tsc.notificationhub.services;

// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.sendgrid.Response;
// import com.sendgrid.helpers.mail.Mail;
// import com.toyota.tsc.notificationhub.commons.*;
// import com.toyota.tsc.notificationhub.exceptions.CustomException;
// import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
// import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
// import com.toyota.tsc.notificationhub.models.*;
// import
// com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto.PersonalInfoList;
// import
// com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto.ContactDto;
// import
// com.toyota.tsc.notificationhub.models.SendMessageNotificationRequestDto.NotificationContent;
// import com.toyota.tsc.notificationhub.repositories.*;
// import com.windowsazure.messaging.NotificationHubsException;
// import com.windowsazure.messaging.NotificationOutcome;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.*;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;

// import java.lang.reflect.Method;
// import java.sql.SQLException;
// import java.time.LocalDateTime;
// import java.util.*;
// import java.util.concurrent.ExecutionException;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// /**
// * SendMessageNotificationServiceImpl のテストクラス
// */
// @SuppressWarnings("all")
// @ExtendWith(MockitoExtension.class)
// class SendMessageNotificationServiceImplTest {

// @InjectMocks
// private SendMessageNotificationServiceImpl service;

// @Mock
// private NotificationRepositoryIF notificationRepository;
// @Mock
// private NotificationVinListRepositoryIF notificationVinListRepository;
// @Mock
// private NtfBatchExecErrorInfoRepositoryIF ntfBatchExecErrorInfoRepository;
// @Mock
// private NtfBatchExecHistoryRepositoryIF ntfBatchExecHistoryRepository;
// @Mock
// private NtfInfoRepositoryIF ntfInfoRepository;
// @Mock
// private BatApisUtil batApisUtil;
// @Mock
// private NotificationHubUtil notificationHubUtil;
// @Mock
// private PersonalInfoUtil personalInfoUtil;
// @Mock
// private PropertiesUtil properties;
// @Mock
// private SendGridUtil sendGridUtil;
// @Mock
// private SmsCountryUtil smsCountryUtil;

// private RequestHeaderDto buildHeader() {
// RequestHeaderDto header = new RequestHeaderDto();
// header.setCorrelationId("corr-001");
// return header;
// }

// private SendMessageNotificationRequestDto buildRequest(String
// notificationType, String isPushRequired) {
// SendMessageNotificationRequestDto req = new
// SendMessageNotificationRequestDto();
// req.setRegistrationSerialNumber(1);
// req.setNotificationType(notificationType);
// req.setIsPushNotificationRequired(isPushRequired);
// req.setScheduledSendData("2024-01-01");
// req.setTitle("Title");
// req.setBodySms("SMS Body");
// req.setBodyText("Text Body");
// req.setBodyHtml("<html>HTML</html>");
// req.setPayload("{\"pushFlg\":\"1\"}");
// req.setNotificationContents(new ArrayList<>());
// return req;
// }

// private NotificationVinListEntity buildVinListEntity(String seqNum) {
// return new NotificationVinListEntity(1, "ME", 100, "VIN001",
// "VIN001:user001:LC001:1", 1L, seqNum, 0, false, null, null);
// }

// private PersonalInfoListResponseDto buildPersonalInfoDto(String
// internalUserId, String contactType, boolean primaryFlag) {
// PersonalInfoListResponseDto dto = new PersonalInfoListResponseDto();
// dto.setResultCode("00001581U000");
// PersonalInfoList info = new PersonalInfoList();
// info.setInternalUserId(internalUserId);
// info.setUserId("userId001");
// ContactDto contact = new ContactDto();
// contact.setContactType(contactType);
// contact.setPrimaryContactFlag(primaryFlag);
// contact.setContact("09012345678");
// info.setContactList(List.of(contact));
// dto.setPersonalInfoList(List.of(info));
// return dto;
// }

// /** クラス：SendMessageNotificationServiceImpl 通知区分=1で正常終了することを確認するテストケース */
// @Test
// void sendMessageNotification_001() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// TscApplicationExceptionが再スローされることを確認するテストケース */
// @Test
// void sendMessageNotification_002() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = new
// SendMessageNotificationRequestDto();

// // Act & Assert
// assertThrows(TscApplicationException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// SQL接続エラー時にCustomExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_003() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// SQLException sqlEx = new SQLException("connection error", "08001");
// when(notificationVinListRepository.select(any(), any())).thenThrow(new
// RuntimeException(sqlEx));

// // Act & Assert
// assertThrows(CustomException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// SQL操作エラー時にCustomSqlExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_004() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// SQLException sqlEx = new SQLException("unique violation", "23505");
// when(notificationVinListRepository.select(any(), any())).thenThrow(new
// RuntimeException(sqlEx));

// // Act & Assert
// assertThrows(CustomSqlException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// その他SQLエラー時にCustomExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_005() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// SQLException sqlEx = new SQLException("other", "99999");
// when(notificationVinListRepository.select(any(), any())).thenThrow(new
// RuntimeException(sqlEx));

// // Act & Assert
// assertThrows(CustomException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// 非SQL例外時にCustomExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_006() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// when(notificationVinListRepository.select(any(), any())).thenThrow(new
// RuntimeException("non-sql"));

// // Act & Assert
// assertThrows(CustomException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// requestがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_007() {
// // Arrange
// RequestHeaderDto header = buildHeader();

// // Act & Assert
// assertThrows(TscApplicationException.class, () ->
// service.sendMessageNotification(null, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// registrationSerialNumberがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_008() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = new
// SendMessageNotificationRequestDto();
// req.setNotificationType("1");

// // Act & Assert
// assertThrows(TscApplicationException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// notificationTypeがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_009() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = new
// SendMessageNotificationRequestDto();
// req.setRegistrationSerialNumber(1);

// // Act & Assert
// assertThrows(TscApplicationException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// notificationTypeが空文字の場合TscApplicationExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_010() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = new
// SendMessageNotificationRequestDto();
// req.setRegistrationSerialNumber(1);
// req.setNotificationType("");

// // Act & Assert
// assertThrows(TscApplicationException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// registrationSerialNumberとnotificationTypeが両方nullの場合TscApplicationExceptionが投げられることを確認するテストケース
// */
// @Test
// void sendMessageNotification_011() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = new
// SendMessageNotificationRequestDto();

// // Act & Assert
// assertThrows(TscApplicationException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// ユーザー情報がnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_012() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// when(notificationVinListRepository.select(1, 0)).thenReturn(null);

// // Act & Assert
// assertThrows(TscApplicationException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// ユーザー情報が空の場合TscApplicationExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_013() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// when(notificationVinListRepository.select(1,
// 0)).thenReturn(Collections.emptyList());

// // Act & Assert
// assertThrows(TscApplicationException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// historyInsertが0件の場合CustomExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_014() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(0);

// // Act & Assert
// assertThrows(CustomException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// vinListUpdate(NOTLINKED)が0件の場合CustomExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_015() {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(0);

// // Act & Assert
// assertThrows(CustomException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// 個人情報APIレスポンスがnullの場合エラー処理が実行されることを確認するテストケース */
// @Test
// void sendMessageNotification_016() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(),
// any())).thenReturn(null);
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(ntfBatchExecErrorInfoRepository, times(1)).insert(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// 個人情報APIが非2xxの場合nullが返ることを確認するテストケース */
// @Test
// void sendMessageNotification_017() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR));
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// 個人情報APIレスポンスボディが空の場合nullが返ることを確認するテストケース */
// @Test
// void sendMessageNotification_018() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>("", HttpStatus.OK));
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// 個人情報APIのresultCodeが正常でない場合nullが返ることを確認するテストケース */
// @Test
// void sendMessageNotification_019() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto errorDto = new PersonalInfoListResponseDto();
// errorDto.setResultCode("99999999");
// errorDto.setPersonalInfoList(new ArrayList<>());
// String resJson = new ObjectMapper().writeValueAsString(errorDto);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl 個人情報リストが空の場合nullが返ることを確認するテストケース
// */
// @Test
// void sendMessageNotification_020() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto emptyDto = new PersonalInfoListResponseDto();
// emptyDto.setResultCode("00001581U000");
// emptyDto.setPersonalInfoList(Collections.emptyList());
// String resJson = new ObjectMapper().writeValueAsString(emptyDto);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// Notification登録エラー時にエラー処理が実行されることを確認するテストケース */
// @Test
// void sendMessageNotification_021() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "1", true);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto failRes = new
// RegisterNotificationResponseDto("999999", "ntf001", "error");
// String failResJson = new ObjectMapper().writeValueAsString(failRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(failResJson, HttpStatus.OK));
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// デバイスデータがnullの場合プッシュエラー処理が実行されることを確認するテストケース */
// @Test
// void sendMessageNotification_022() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "1");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(Collections.emptyList());
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// FCMプラットフォームでプッシュ通知が正常に送信されることを確認するテストケース */
// @Test
// void sendMessageNotification_023() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "1");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
// "dev001", "1", "1",
// LocalDateTime.now().minusHours(1), LocalDateTime.now());
// when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
// String fcmPayload =
// "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
// when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
// when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
// any())).thenReturn(fcmPayload);
// when(properties.getRetryCount()).thenReturn(3);
// when(notificationHubUtil.postMessage(any(), any(), any(),
// any())).thenReturn(mock(NotificationOutcome.class));
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// APNプラットフォームでプッシュ通知が正常に送信されることを確認するテストケース */
// @Test
// void sendMessageNotification_024() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "1");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
// "dev001", "1", "2",
// LocalDateTime.now().minusHours(1), LocalDateTime.now());
// when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
// String apnsPayload = "{\"aps\":{\"alert\":\"
// \",\"mutable-content\":1},\"lcsSelected\":\"LC001\"}";
// when(notificationHubUtil.buildApnsPayload(any())).thenReturn(apnsPayload);
// when(notificationHubUtil.replaceLcsSelected(any(), eq("2"),
// any())).thenReturn(apnsPayload);
// when(properties.getRetryCount()).thenReturn(3);
// when(notificationHubUtil.postMessage(any(), any(), any(),
// any())).thenReturn(mock(NotificationOutcome.class));
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// NotificationHubsExceptionがTransientでリトライ上限到達時にnullが返ることを確認するテストケース */
// @Test
// void sendMessageNotification_025() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "1");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
// "dev001", "1", "1",
// LocalDateTime.now().minusHours(1), LocalDateTime.now());
// when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
// String fcmPayload =
// "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
// when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
// when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
// any())).thenReturn(fcmPayload);
// when(properties.getRetryCount()).thenReturn(1);
// NotificationHubsException transEx = mock(NotificationHubsException.class);
// when(transEx.isTransient()).thenReturn(true);
// when(transEx.httpStatusCode()).thenReturn(503);
// when(notificationHubUtil.postMessage(any(), any(), any(),
// any())).thenThrow(transEx);
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// NotificationHubsExceptionがTransientでない場合nullが返ることを確認するテストケース */
// @Test
// void sendMessageNotification_026() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "1");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
// "dev001", "1", "1",
// LocalDateTime.now().minusHours(1), LocalDateTime.now());
// when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
// String fcmPayload =
// "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
// when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
// when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
// any())).thenReturn(fcmPayload);
// when(properties.getRetryCount()).thenReturn(3);
// NotificationHubsException nonTransEx = mock(NotificationHubsException.class);
// when(nonTransEx.isTransient()).thenReturn(false);
// when(nonTransEx.httpStatusCode()).thenReturn(400);
// when(notificationHubUtil.postMessage(any(), any(), any(),
// any())).thenThrow(nonTransEx);
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// 通知区分=2かつSMS連絡先でSMSが送信されることを確認するテストケース */
// @Test
// void sendMessageNotification_027() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("2", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "1", true);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// when(smsCountryUtil.executeSendSms(any(), any(), any()))
// .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(smsCountryUtil, times(1)).executeSendSms(any(), any(), any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// 通知区分=2かつEmail連絡先でメールが送信されることを確認するテストケース */
// @Test
// void sendMessageNotification_028() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("2", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", true);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// Mail mockMail = mock(Mail.class);
// when(sendGridUtil.generateEmail(any(), any(), any(), any(),
// any())).thenReturn(mockMail);
// Response mockResponse = new Response(200, "ok", new HashMap<>());
// when(sendGridUtil.executeSendEmail(any())).thenReturn(mockResponse);
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(sendGridUtil, times(1)).executeSendEmail(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// 通知区分=3でNotification登録とSMS送信が実行されることを確認するテストケース */
// @Test
// void sendMessageNotification_029() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("3", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "1", true);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// when(smsCountryUtil.executeSendSms(any(), any(), any()))
// .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// SMS送信が失敗した場合エラー処理が実行されることを確認するテストケース */
// @Test
// void sendMessageNotification_030() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("2", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "1", true);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// when(smsCountryUtil.executeSendSms(any(), any(), any()))
// .thenReturn(new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR));
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// メール送信が失敗した場合エラー処理が実行されることを確認するテストケース */
// @Test
// void sendMessageNotification_031() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("2", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", true);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// Mail mockMail = mock(Mail.class);
// when(sendGridUtil.generateEmail(any(), any(), any(), any(),
// any())).thenReturn(mockMail);
// Response mockResponse = new Response(500, "error", new HashMap<>());
// when(sendGridUtil.executeSendEmail(any())).thenReturn(mockResponse);
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// primaryContactFlagがfalseの場合連絡先がスキップされることを確認するテストケース */
// @Test
// void sendMessageNotification_032() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("2", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "1", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(smsCountryUtil, never()).executeSendSms(any(), any(), any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// NotificationHubsExceptionがTransientでリトライ後に成功することを確認するテストケース */
// @Test
// void sendMessageNotification_033() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "1");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
// "dev001", "1", "1",
// LocalDateTime.now().minusHours(1), LocalDateTime.now());
// when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
// String fcmPayload =
// "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
// when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
// when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
// any())).thenReturn(fcmPayload);
// when(properties.getRetryCount()).thenReturn(3);
// NotificationHubsException transEx = mock(NotificationHubsException.class);
// when(transEx.isTransient()).thenReturn(true);
// when(transEx.httpStatusCode()).thenReturn(503);
// when(notificationHubUtil.postMessage(any(), any(), any(), any()))
// .thenThrow(transEx)
// .thenReturn(mock(NotificationOutcome.class));
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// }

// /** クラス：SendMessageNotificationServiceImpl
// 通知リストが100件超の場合複数チャンクに分割されることを確認するテストケース */
// @Test
// void sendMessageNotification_034() throws Exception {
// // Arrange
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// StringBuilder sb = new StringBuilder();
// for (int i = 1; i <= 101; i++) {
// if (i > 1) sb.append(",");
// sb.append("VIN").append(i).append(":user").append(String.format("%03d",
// i)).append(":LC001:1");
// }
// NotificationVinListEntity entity = new NotificationVinListEntity(
// 1, "ME", 100, "VIN001", sb.toString(), 1L, "1", 0, false, null, null);
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(2);
// PersonalInfoListResponseDto dto1 = new PersonalInfoListResponseDto();
// dto1.setResultCode("00001581U000");
// dto1.setPersonalInfoList(new ArrayList<>());
// String dtoJson1 = new ObjectMapper().writeValueAsString(dto1);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(dtoJson1, HttpStatus.OK));
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(personalInfoUtil, times(2)).getPersonalInfoListApiResponse(any(),
// any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// 個人情報APIレスポンスボディがnullの場合nullが返ることを確認するテストケース */
// @Test
// void sendMessageNotification_035() throws Exception {
// // Arrange - L473: resBody == null (response.getBody() returns null)
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// // ResponseEntity with null body but 2xx status
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(ntfBatchExecErrorInfoRepository, times(1)).insert(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// 個人情報リストがnullの場合nullが返ることを確認するテストケース */
// @Test
// void sendMessageNotification_036() throws Exception {
// // Arrange - L482: personalInfoList == null (not empty, but null)
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// // Build a response where personalInfoList is null (not empty list)
// PersonalInfoListResponseDto nullListDto = new PersonalInfoListResponseDto();
// nullListDto.setResultCode("00001581U000");
// nullListDto.setPersonalInfoList(null);
// String resJson = new ObjectMapper().writeValueAsString(nullListDto);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(ntfBatchExecErrorInfoRepository, times(1)).insert(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// 個人情報APIが例外を投げた場合completion.takeのcatchブロックでfuturesがキャンセルされnullが返ることを確認するテストケース
// */
// @Test
// void sendMessageNotification_037() throws Exception {
// // Arrange - L518: for (Future f : futures) in catch block triggered by
// done.get() exception
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// // Make the callable throw an exception so done.get() throws
// ExecutionException
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenThrow(new RuntimeException("API call failed"));
// when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(ntfBatchExecErrorInfoRepository, times(1)).insert(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// getPersonalInfoListの外側catchブロックで例外がスローされることを確認するテストケース */
// @Test
// void sendMessageNotification_038() throws Exception {
// // Arrange - L528: outer catch (Exception e) in getPersonalInfoList
// // Trigger by making getParallelCurrent throw (before executor is created)
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// // Throw exception at the very beginning of getPersonalInfoList to reach L528
// when(properties.getParallelCurrent()).thenThrow(new
// RuntimeException("parallelCurrent error"));

// // Act & Assert - CustomException wraps the RuntimeException at L528-530
// assertThrows(CustomException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// Notification登録APIレスポンスがnullの場合CustomExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_039() throws Exception {
// // Arrange - L645: responseDto == null in executeRegisterNotification
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// // Return "null" as response body so ObjectMapper deserializes to null
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>("null", HttpStatus.OK));

// // Act & Assert - L645-647 throws CustomException which propagates
// assertThrows(CustomException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// createPayloadのswitch文でdefaultケースに到達した場合CustomExceptionが投げられることを確認するテストケース */
// @Test
// void sendMessageNotification_040() throws Exception {
// // Arrange - L752: switch default case in createPayload (platformType != "1"
// and != "2")
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "1");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// // Device with platformType "3" (not FCM "1" or APN "2") to hit default case
// NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
// "dev001", "1", "3",
// LocalDateTime.now().minusHours(1), LocalDateTime.now());
// when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));

// // Act & Assert - default case throws CustomException at L758
// assertThrows(CustomException.class, () ->
// service.sendMessageNotification(req, header));
// }

// /** クラス：SendMessageNotificationServiceImpl
// executePostMessageでretryCount=0の場合whileループに入らずnullが返ることを確認するテストケース */
// @Test
// void sendMessageNotification_041() throws Exception {
// // Arrange - L787: retryCount=0 so while loop is never entered
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("1", "1");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", false);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
// "dev001", "1", "1",
// LocalDateTime.now().minusHours(1), LocalDateTime.now());
// when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
// String fcmPayload =
// "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
// when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
// when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
// any())).thenReturn(fcmPayload);
// // retryCount = 0 -> while (cnt < 0) never enters loop
// when(properties.getRetryCount()).thenReturn(0);
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// // postMessage should never be called since loop is never entered
// verify(notificationHubUtil, never()).postMessage(any(), any(), any(), any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// sendRequestでメール連絡先タイプの場合メール送信が実行されることを確認するテストケース */
// @Test
// void sendMessageNotification_042() throws Exception {
// // Arrange - L903: else if (contact.getContactType().equals(CONTACT_EMAIL))
// branch
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("3", "0");
// NotificationVinListEntity entity = buildVinListEntity("1");
// when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
// when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(0))).thenReturn(1);
// when(properties.getParallelCurrent()).thenReturn(1);
// // Build personalInfo with email contact type (CONTACT_EMAIL = "2") and
// primaryContactFlag = true
// PersonalInfoListResponseDto personalInfoRes = buildPersonalInfoDto("user001",
// "2", true);
// String resJson = new ObjectMapper().writeValueAsString(personalInfoRes);
// when(personalInfoUtil.getPersonalInfoListApiResponse(any(), any()))
// .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
// // Notification registration (notificationType=3 requires it)
// RegisterNotificationResponseDto regRes = new
// RegisterNotificationResponseDto("000000", "ntf001", "ok");
// String regResJson = new ObjectMapper().writeValueAsString(regRes);
// when(batApisUtil.executeRegisterNotification(any()))
// .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
// // Email sending
// Mail mockMail = mock(Mail.class);
// when(sendGridUtil.generateEmail(any(), any(), any(), any(),
// any())).thenReturn(mockMail);
// Response mockResponse = new Response(200, "ok", new HashMap<>());
// when(sendGridUtil.executeSendEmail(any())).thenReturn(mockResponse);
// when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
// any())).thenReturn(1);
// when(notificationVinListRepository.update(eq(1), eq("1"),
// eq(2))).thenReturn(1);
// when(notificationRepository.update("ME", 1)).thenReturn(1);

// // Act
// ResponseDto result = service.sendMessageNotification(req, header);

// // Assert
// assertNotNull(result);
// verify(sendGridUtil, times(1)).executeSendEmail(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// sendRequestメソッドをリフレクションで直接呼び出しメール連絡先タイプの分岐を確認するテストケース */
// @Test
// void sendMessageNotification_043() throws Exception {
// // Arrange - L903: directly invoke sendRequest via reflection for email
// contact type
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("2", "0");
// PersonalInfoList personalInfo = new PersonalInfoList();
// personalInfo.setInternalUserId("user001");
// personalInfo.setUserId("userId001");
// ContactDto emailContact = new ContactDto();
// emailContact.setContactType("2"); // CONTACT_EMAIL
// emailContact.setPrimaryContactFlag(true);
// emailContact.setContact("test@example.com");
// personalInfo.setContactList(List.of(emailContact));

// Mail mockMail = mock(Mail.class);
// when(sendGridUtil.generateEmail(any(), any(), any(), any(),
// any())).thenReturn(mockMail);
// Response mockResponse = new Response(200, "ok", new HashMap<>());
// when(sendGridUtil.executeSendEmail(any())).thenReturn(mockResponse);

// // Act - invoke private sendRequest method via reflection
// Method sendRequestMethod =
// SendMessageNotificationServiceImpl.class.getDeclaredMethod(
// "sendRequest", SendMessageNotificationRequestDto.class,
// RequestHeaderDto.class,
// PersonalInfoList.class, String.class);
// sendRequestMethod.setAccessible(true);
// boolean result = (boolean) sendRequestMethod.invoke(service, req, header,
// personalInfo, "1");

// // Assert
// assertFalse(result);
// verify(sendGridUtil, times(1)).executeSendEmail(any());
// }

// /** クラス：SendMessageNotificationServiceImpl
// sendRequestで連絡先種別が電話でもメールでもない場合スキップされることを確認するテストケース */
// @Test
// void sendMessageNotification_044() throws Exception {
// // Arrange - L903: else if false branch - contactType is neither "1" (phone)
// nor "2" (email)
// RequestHeaderDto header = buildHeader();
// SendMessageNotificationRequestDto req = buildRequest("2", "0");
// PersonalInfoList personalInfo = new PersonalInfoList();
// personalInfo.setInternalUserId("user001");
// personalInfo.setUserId("userId001");
// ContactDto unknownContact = new ContactDto();
// unknownContact.setContactType("9"); // Neither CONTACT_PHONE nor
// CONTACT_EMAIL
// unknownContact.setPrimaryContactFlag(true);
// unknownContact.setContact("unknown");
// personalInfo.setContactList(List.of(unknownContact));

// // Act - invoke private sendRequest method via reflection
// Method sendRequestMethod =
// SendMessageNotificationServiceImpl.class.getDeclaredMethod(
// "sendRequest", SendMessageNotificationRequestDto.class,
// RequestHeaderDto.class,
// PersonalInfoList.class, String.class);
// sendRequestMethod.setAccessible(true);
// boolean result = (boolean) sendRequestMethod.invoke(service, req, header,
// personalInfo, "1");

// // Assert - no SMS or email should be sent
// assertFalse(result);
// verify(smsCountryUtil, never()).executeSendSms(any(), any(), any());
// verify(sendGridUtil, never()).executeSendEmail(any());
// }
// }
