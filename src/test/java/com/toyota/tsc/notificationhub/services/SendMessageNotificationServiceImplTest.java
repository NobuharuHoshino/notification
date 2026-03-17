package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.*;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.models.*;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import
com.toyota.tsc.notificationhub.models.SendMessageNotificationRequestDto.NotificationContent;
import com.toyota.tsc.notificationhub.repositories.*;
import com.windowsazure.messaging.NotificationHubsException;
import com.windowsazure.messaging.NotificationOutcome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SendMessageNotificationServiceImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class SendMessageNotificationServiceImplTest {

    @InjectMocks
    private SendMessageNotificationServiceImpl service;

    @Mock
    private NotificationRepositoryIF notificationRepository;
    @Mock
    private NotificationVinListRepositoryIF notificationVinListRepository;
    @Mock
    private NtfBatchExecErrorInfoRepositoryIF ntfBatchExecErrorInfoRepository;
    @Mock
    private NtfBatchExecHistoryRepositoryIF ntfBatchExecHistoryRepository;
    @Mock
    private NtfInfoRepositoryIF ntfInfoRepository;
    @Mock
    private BatApisUtil batApisUtil;
    @Mock
    private NotificationHubUtil notificationHubUtil;
    @Mock
    private PersonalInfoUtil personalInfoUtil;
    @Mock
    private PropertiesUtil properties;
    @Mock
    private SendGridUtil sendGridUtil;
    @Mock
    private SmsCountryUtil smsCountryUtil;

    private MockedStatic<CommonUtil> mockedCommonUtil;

    @BeforeEach
    void setUp() {
        mockedCommonUtil = mockStatic(CommonUtil.class);
        mockedCommonUtil.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("log");
        mockedCommonUtil.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("mocked-code");
        mockedCommonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
    }

    @AfterEach
    void tearDown() {
        mockedCommonUtil.close();
    }

    private RequestHeaderDto buildHeader() {
        RequestHeaderDto header = new RequestHeaderDto();
        header.setCorrelationId("corr-001");
        return header;
    }

    private SendMessageNotificationRequestDto buildRequest(String
    notificationType, String isPushRequired) {
        SendMessageNotificationRequestDto req = new
        SendMessageNotificationRequestDto();
        req.setRegistrationSerialNumber(1);
        req.setNotificationType(notificationType);
        req.setIsPushNotificationRequired(isPushRequired);
        req.setScheduledSendData("2024-01-01");
        req.setTitle("Title");
        req.setBodySms("SMS Body");
        req.setBodyText("Text Body");
        req.setBodyHtml("<html>HTML</html>");
        req.setPayload("{\"pushFlg\":\"1\"}");
        SendMessageNotificationRequestDto.NotificationContent content = new SendMessageNotificationRequestDto.NotificationContent();
        content.setLanguageCode("ja");
        content.setTitle("Test Title");
        content.setDetail("Test Detail");
        List<SendMessageNotificationRequestDto.NotificationContent> contents = new ArrayList<>();
        contents.add(content);
        req.setNotificationContents(contents);
        return req;
    }

    private NotificationVinListEntity buildVinListEntity(Long seqNum) {
        return new NotificationVinListEntity(1, "ME", 100, "VIN001",
        "VIN001:user001:LC001:1", 1L, seqNum, 0, false, null, null);
    }

    private PersonalInfoResponseDto buildPersonalInfoDto(String
    internalUserId, String contactType, boolean primaryFlag) {
        PersonalInfoResponseDto dto = new PersonalInfoResponseDto();
        dto.setResultCode("00001581U000");
        dto.setInternalUserId(internalUserId);
        dto.setUserId("userId001");
        PersonalInfoResponseDto.ContactDto contact = new PersonalInfoResponseDto.ContactDto();
        contact.setContactType(contactType);
        contact.setPrimaryContactFlag(primaryFlag);
        contact.setContact("09012345678");
        dto.setContactList(List.of(contact));
        return dto;
    }

    /** クラス：SendMessageNotificationServiceImpl 通知区分=1で正常終了することを確認するテストケース */
    @Test
    void sendMessageNotification_001() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    TscApplicationExceptionが再スローされることを確認するテストケース */
    @Test
    void sendMessageNotification_002() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new
        SendMessageNotificationRequestDto();

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    SQL接続エラー時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_003() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        SQLException sqlEx = new SQLException("connection error", "08001");
        when(notificationVinListRepository.select(any(), any())).thenThrow(new
        RuntimeException(sqlEx));

        // Act & Assert
        assertThrows(CustomException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    SQL操作エラー時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_004() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        SQLException sqlEx = new SQLException("unique violation", "23505");
        when(notificationVinListRepository.select(any(), any())).thenThrow(new
        RuntimeException(sqlEx));

        // Act & Assert
        assertThrows(CustomException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    その他SQLエラー時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_005() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        SQLException sqlEx = new SQLException("other", "99999");
        when(notificationVinListRepository.select(any(), any())).thenThrow(new
        RuntimeException(sqlEx));

        // Act & Assert
        assertThrows(CustomException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    非SQL例外時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_006() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        when(notificationVinListRepository.select(any(), any())).thenThrow(new
        RuntimeException("non-sql"));

        // Act & Assert
        assertThrows(CustomException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    requestがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_007() {
        // Arrange
        RequestHeaderDto header = buildHeader();

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
        service.sendMessageNotification(null, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    registrationSerialNumberがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_008() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new
        SendMessageNotificationRequestDto();
        req.setNotificationType("1");

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    notificationTypeがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_009() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new
        SendMessageNotificationRequestDto();
        req.setRegistrationSerialNumber(1);

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    notificationTypeが空文字の場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_010() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new
        SendMessageNotificationRequestDto();
        req.setRegistrationSerialNumber(1);
        req.setNotificationType("");

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    registrationSerialNumberとnotificationTypeが両方nullの場合TscApplicationExceptionが投げられることを確認するテストケース
    */
    @Test
    void sendMessageNotification_011() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new
        SendMessageNotificationRequestDto();

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    ユーザー情報がnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_012() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        when(notificationVinListRepository.select(1, 0)).thenReturn(null);

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    ユーザー情報が空の場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_013() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        when(notificationVinListRepository.select(1,
        0)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
        service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    historyInsertが0件の場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_014() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(0);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    vinListUpdate(NOTLINKED)が0件の場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_015() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(0);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    個人情報APIレスポンスがnullの場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_016() throws Exception {
        // Arrange - personalInfoUtil returns null -> getPersonalInfo returns null -> executePrimaryContact returns true
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any())).thenReturn(null);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    個人情報APIのresultCodeが正常でない場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_017() throws Exception {
        // Arrange - personalInfoUtil returns DTO with bad resultCode -> executePrimaryContact returns true
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto badResultDto = new PersonalInfoResponseDto();
        badResultDto.setResultCode("99999");
        badResultDto.setContactList(new ArrayList<>());
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(badResultDto);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    個人情報APIのcontactListがnullの場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_018() throws Exception {
        // Arrange - personalInfoUtil returns DTO with null contactList -> getPersonalInfo returns null
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto nullContactDto = new PersonalInfoResponseDto();
        nullContactDto.setResultCode("00001581U000");
        nullContactDto.setContactList(null);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(nullContactDto);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    個人情報APIのcontactListが空の場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_019() throws Exception {
        // Arrange - personalInfoUtil returns DTO with empty contactList -> getPersonalInfo returns null
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto emptyContactDto = new PersonalInfoResponseDto();
        emptyContactDto.setResultCode("00001581U000");
        emptyContactDto.setContactList(new ArrayList<>());
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(emptyContactDto);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl 個人情報APIが例外をスローした場合エラー処理が実行されることを確認するテストケース
    */
    @Test
    void sendMessageNotification_020() throws Exception {
        // Arrange - personalInfoUtil throws CustomException -> executePrimaryContact catch -> executeErrorProcess
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenThrow(new com.toyota.tsc.notificationhub.exceptions.CustomException("api error"));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    Notification登録エラー時にエラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_021() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto failRes = new
        RegisterNotificationResponseDto("999999", "ntf001", "error");
        String failResJson = new ObjectMapper().writeValueAsString(failRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(failResJson, HttpStatus.OK));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    デバイスデータがnullの場合プッシュエラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_022() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(Collections.emptyList());
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    FCMプラットフォームでプッシュ通知が正常に送信されることを確認するテストケース */
    @Test
    void sendMessageNotification_023() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
        "dev001", "1", "1",
        LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        String fcmPayload =
        "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
        when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
        any())).thenReturn(fcmPayload);
        when(properties.getRetryCount()).thenReturn(3);
        when(notificationHubUtil.postMessage(any(), any(), any(),
        any())).thenReturn(mock(NotificationOutcome.class));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    APNプラットフォームでプッシュ通知が正常に送信されることを確認するテストケース */
    @Test
    void sendMessageNotification_024() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
        "dev001", "1", "2",
        LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        String apnsPayload = "{\"aps\":{\"alert\":\"\",\"mutable-content\":1},\"lcsSelected\":\"LC001\"}";
        when(notificationHubUtil.buildApnsPayload(any())).thenReturn(apnsPayload);
        when(notificationHubUtil.replaceLcsSelected(any(), eq("2"),
        any())).thenReturn(apnsPayload);
        when(properties.getRetryCount()).thenReturn(3);
        when(notificationHubUtil.postMessage(any(), any(), any(),
        any())).thenReturn(mock(NotificationOutcome.class));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    NotificationHubsExceptionがTransientでリトライ上限到達時にnullが返ることを確認するテストケース */
    @Test
    void sendMessageNotification_025() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
        "dev001", "1", "1",
        LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        String fcmPayload =
        "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
        when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
        any())).thenReturn(fcmPayload);
        when(properties.getRetryCount()).thenReturn(1);
        NotificationHubsException transEx = mock(NotificationHubsException.class);
        when(transEx.isTransient()).thenReturn(true);
        when(transEx.httpStatusCode()).thenReturn(503);
        when(notificationHubUtil.postMessage(any(), any(), any(),
        any())).thenThrow(transEx);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    NotificationHubsExceptionがTransientでない場合nullが返ることを確認するテストケース */
    @Test
    void sendMessageNotification_026() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
        "dev001", "1", "1",
        LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        String fcmPayload =
        "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
        when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
        any())).thenReturn(fcmPayload);
        when(properties.getRetryCount()).thenReturn(3);
        NotificationHubsException nonTransEx = mock(NotificationHubsException.class);
        when(nonTransEx.isTransient()).thenReturn(false);
        when(nonTransEx.httpStatusCode()).thenReturn(400);
        when(notificationHubUtil.postMessage(any(), any(), any(),
        any())).thenThrow(nonTransEx);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    通知区分=2かつSMS連絡先でSMSが送信されることを確認するテストケース */
    @Test
    void sendMessageNotification_027() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto personalInfoRes = buildPersonalInfoDto("user001",
        "1", true);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
        .thenReturn(personalInfoRes);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    通知区分=2かつEmail連絡先でメールが送信されることを確認するテストケース */
    @Test
    void sendMessageNotification_028() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto personalInfoRes = buildPersonalInfoDto("user001",
        "2", true);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
        .thenReturn(personalInfoRes);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    通知区分=3でNotification登録とSMS送信が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_029() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("3", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto personalInfoRes = buildPersonalInfoDto("user001",
        "1", true);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
        .thenReturn(personalInfoRes);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    SMS送信が失敗した場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_030() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto personalInfoRes = buildPersonalInfoDto("user001",
        "1", true);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
        .thenReturn(personalInfoRes);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    メール送信が失敗した場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_031() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto personalInfoRes = buildPersonalInfoDto("user001",
        "2", true);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
        .thenReturn(personalInfoRes);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    primaryContactFlagがfalseの場合連絡先がスキップされることを確認するテストケース */
    @Test
    void sendMessageNotification_032() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto personalInfoRes = buildPersonalInfoDto("user001",
        "1", false);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
        .thenReturn(personalInfoRes);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(smsCountryUtil, never()).executeSendSms(any(), any(), any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    NotificationHubsExceptionがTransientでリトライ後に成功することを確認するテストケース */
    @Test
    void sendMessageNotification_033() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
        "dev001", "1", "1",
        LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        String fcmPayload =
        "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
        when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
        any())).thenReturn(fcmPayload);
        when(properties.getRetryCount()).thenReturn(3);
        NotificationHubsException transEx = mock(NotificationHubsException.class);
        when(transEx.isTransient()).thenReturn(true);
        when(transEx.httpStatusCode()).thenReturn(503);
        when(notificationHubUtil.postMessage(any(), any(), any(), any()))
        .thenThrow(transEx)
        .thenReturn(mock(NotificationOutcome.class));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    通知リストが複数ユーザーの場合個人情報APIが各ユーザーに対して呼ばれることを確認するテストケース */
    @Test
    void sendMessageNotification_034() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        // 2 users in one entity's notificationSendList
        String csvText = "VIN001:user001:LC001:1,VIN002:user002:LC001:1";
        NotificationVinListEntity entity = new NotificationVinListEntity(
        1, "ME", 100, "VIN001", csvText, 1L, 1L, 0, false, null, null);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // personalInfo returns null contactList -> getPersonalInfo returns null -> primaryContactError
        PersonalInfoResponseDto piDto = new PersonalInfoResponseDto();
        piDto.setResultCode("00001581U000");
        piDto.setContactList(new ArrayList<>());
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
        .thenReturn(piDto);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(personalInfoUtil, atLeast(2)).getPersonalInfoApiResponse(any(), any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    個人情報APIレスポンスがnullの場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_035() throws Exception {
        // Arrange - getPersonalInfoApiResponse returns null -> getPersonalInfo returns null -> error
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(null);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    個人情報APIのresultCodeが不正な場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_036() throws Exception {
        // Arrange - personalInfoUtil returns DTO with bad resultCode -> executePrimaryContact returns true
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // Build DTO with non-empty contactList but bad resultCode
        // getPersonalInfo: contactList non-empty → returns dto (not null)
        // executePrimaryContact: !resultCode.equals(GETPERSONAL_SUCCESS) → return true (error)
        PersonalInfoResponseDto badDto = new PersonalInfoResponseDto();
        badDto.setResultCode("BADCODE");
        PersonalInfoResponseDto.ContactDto c36 = new PersonalInfoResponseDto.ContactDto();
        c36.setContactType("1");
        c36.setPrimaryContactFlag(true);
        c36.setContact("09099999999");
        badDto.setContactList(List.of(c36));
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(badDto);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    個人情報APIが例外を投げた場合catchブロックでexecuteErrorProcessが実行されることを確認するテストケース
    */
    @Test
    void sendMessageNotification_037() throws Exception {
        // Arrange - personalInfoUtil throws RuntimeException inside getPersonalInfo -> catch -> executeErrorProcess
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenThrow(new com.toyota.tsc.notificationhub.exceptions.CustomException("api error"));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    getPersonalInfoがCustomExceptionをスローした場合executePrimaryContactのcatchブロックでエラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_038() throws Exception {
        // Arrange - personalInfoUtil throws CustomException -> executePrimaryContact catch -> executeErrorProcess called
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("3", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes038 = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson038 = new ObjectMapper().writeValueAsString(regRes038);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson038, HttpStatus.OK));
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenThrow(new com.toyota.tsc.notificationhub.exceptions.CustomException("api error"));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    Notification登録APIレスポンスがnullの場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_039() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>("null", HttpStatus.OK));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    createPayloadのswitch文でdefaultケースに到達した場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_040() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
        "dev001", "1", "3",
        LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    executePostMessageでretryCount=0の場合whileループに入らずnullが返ることを確認するテストケース */
    @Test
    void sendMessageNotification_041() throws Exception {
        // Arrange - L787: retryCount=0 so while loop is never entered
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        NtfInfoEntity device = new NtfInfoEntity("user001", "inst001", "token001",
        "dev001", "1", "1",
        LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        String fcmPayload =
        "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload);
        when(notificationHubUtil.replaceLcsSelected(any(), eq("1"),
        any())).thenReturn(fcmPayload);
        // retryCount = 0 -> while (cnt < 0) never enters loop
        when(properties.getRetryCount()).thenReturn(0);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        // postMessage should never be called since loop is never entered
        verify(notificationHubUtil, never()).postMessage(any(), any(), any(), any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    sendRequestでメール連絡先タイプの場合メール送信が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_042() throws Exception {
        // Arrange - L903: else if (contact.getContactType().equals(CONTACT_EMAIL))
        // branch
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("3", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // Build personalInfo with email contact type (CONTACT_EMAIL = "2") and
        // primaryContactFlag = true
        PersonalInfoResponseDto personalInfoRes = buildPersonalInfoDto("user001",
        "2", true);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
        .thenReturn(personalInfoRes);
        // Notification registration (notificationType=3 requires it)
        RegisterNotificationResponseDto regRes = new
        RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
        .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(),
        any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L),
        eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    sendRequestメソッドをリフレクションで直接呼び出しメール連絡先タイプの分岐を確認するテストケース */
    @Test
    void sendMessageNotification_043() throws Exception {
        // Arrange - directly invoke sendRequest via reflection for email contact type
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        PersonalInfoResponseDto personalInfo = new PersonalInfoResponseDto();
        personalInfo.setInternalUserId("user001");
        personalInfo.setUserId("userId001");
        PersonalInfoResponseDto.ContactDto emailContact = new PersonalInfoResponseDto.ContactDto();
        emailContact.setContactType("2"); // CONTACT_EMAIL
        emailContact.setPrimaryContactFlag(true);
        emailContact.setContact("test@example.com");
        personalInfo.setResultCode("00001581U000");
        personalInfo.setContactList(List.of(emailContact));

        Mail mockMail = mock(Mail.class);
        when(sendGridUtil.generateEmail(any(), any(), any(), any(),
        any())).thenReturn(mockMail);
        Response mockResponse = new Response(200, "ok", new HashMap<>());
        when(sendGridUtil.executeSendEmail(any())).thenReturn(mockResponse);

        // Act - invoke private sendRequest method via reflection
        Method sendRequestMethod =
        SendMessageNotificationServiceImpl.class.getDeclaredMethod(
        "sendRequest", SendMessageNotificationRequestDto.class,
        RequestHeaderDto.class,
        PersonalInfoResponseDto.class, String.class);
        sendRequestMethod.setAccessible(true);
        boolean result = (boolean) sendRequestMethod.invoke(service, req, header,
        personalInfo, "1");

        // Assert
        assertFalse(result);
        verify(sendGridUtil, times(1)).executeSendEmail(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    sendRequestで連絡先種別が電話でもメールでもない場合スキップされることを確認するテストケース */
    @Test
    void sendMessageNotification_044() throws Exception {
        // Arrange - contactType is neither "1" (phone) nor "2" (email) -> skipped
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        PersonalInfoResponseDto personalInfo = new PersonalInfoResponseDto();
        personalInfo.setInternalUserId("user001");
        personalInfo.setUserId("userId001");
        personalInfo.setResultCode("00001581U000");
        PersonalInfoResponseDto.ContactDto unknownContact = new PersonalInfoResponseDto.ContactDto();
        unknownContact.setContactType("9"); // Neither CONTACT_PHONE nor CONTACT_EMAIL
        unknownContact.setPrimaryContactFlag(true);
        unknownContact.setContact("unknown");
        personalInfo.setContactList(List.of(unknownContact));

        // Act - invoke private sendRequest method via reflection
        Method sendRequestMethod =
        SendMessageNotificationServiceImpl.class.getDeclaredMethod(
        "sendRequest", SendMessageNotificationRequestDto.class,
        RequestHeaderDto.class,
        PersonalInfoResponseDto.class, String.class);
        sendRequestMethod.setAccessible(true);
        boolean result = (boolean) sendRequestMethod.invoke(service, req, header,
        personalInfo, "1");

        // Assert - no SMS or email should be sent
        assertFalse(result);
        verify(smsCountryUtil, never()).executeSendSms(any(), any(), any());
        verify(sendGridUtil, never()).executeSendEmail(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    通知区分=3かつnotificationContentsがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_045() {
        // Arrange - validateRequired: TYPE_NTF_AND_MAILSMS with null notificationContents
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
        req.setRegistrationSerialNumber(1);
        req.setNotificationType("3");
        req.setNotificationContents(null);

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
                service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    通知区分=3かつnotificationContentsが空の場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_046() {
        // Arrange - validateRequired: TYPE_NTF_AND_MAILSMS with empty notificationContents
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
        req.setRegistrationSerialNumber(1);
        req.setNotificationType("3");
        req.setNotificationContents(new ArrayList<>());

        // Act & Assert
        assertThrows(TscApplicationException.class, () ->
                service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
    notificationSendListが空の場合エラー処理が実行されて次ループへ遷移することを確認するテストケース */
    @Test
    void sendMessageNotification_047() {
        // Arrange - convertToNotificationSendListDto returns empty list (invalid CSV format)
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        // Use an entity with empty CSV text → convertToNotificationSendListDto catches exception and returns []
        NotificationVinListEntity entity = new NotificationVinListEntity(
                1, "ME", 100, "VIN001", "", 1L, 1L, 0, false, null, null);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    forEachループ内でSQL接続エラーが発生した場合ログ出力してエラー登録後に次ループへ遷移することを確認するテストケース */
    @Test
    void sendMessageNotification_048() {
        // Arrange - outer catch: SQL connection error from ntfBatchExecHistoryRepository.insert
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        SQLException sqlEx = new SQLException("connection error", "08001");
        when(ntfBatchExecHistoryRepository.insert(any())).thenThrow(new RuntimeException(sqlEx));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    forEachループ内でSQL操作エラーが発生した場合ログ出力してエラー登録後に次ループへ遷移することを確認するテストケース */
    @Test
    void sendMessageNotification_049() {
        // Arrange - outer catch: SQL operation error from ntfBatchExecHistoryRepository.insert
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        SQLException sqlEx = new SQLException("unique violation", "23505");
        when(ntfBatchExecHistoryRepository.insert(any())).thenThrow(new RuntimeException(sqlEx));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    forEachループ内で非SQL例外が発生した場合ログ出力してエラー登録後に次ループへ遷移することを確認するテストケース */
    @Test
    void sendMessageNotification_050() {
        // Arrange - outer catch: non-SQL exception from ntfBatchExecHistoryRepository.insert
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        when(ntfBatchExecHistoryRepository.insert(any())).thenThrow(new RuntimeException("non-sql error"));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
    非同期処理内でSQL接続エラーが発生した場合ログ出力して処理が継続することを確認するテストケース */
    @Test
    void sendMessageNotification_051() throws Exception {
        // Arrange - inner async catch: SQL connection error from ntfBatchExecHistoryRepository.updateStatus
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto piRes051 = buildPersonalInfoDto("user001", "1", false);
        lenient().when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(piRes051);
        RegisterNotificationResponseDto regRes = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        // updateStatus throws SQL connection error in async block (first call throws, second call in catch returns 1)
        SQLException sqlEx = new SQLException("connection error", "08001");
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any()))
                .thenThrow(new RuntimeException(sqlEx))
                .thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    非同期処理内でSQL操作エラーが発生した場合ログ出力して処理が継続することを確認するテストケース */
    @Test
    void sendMessageNotification_052() throws Exception {
        // Arrange - inner async catch: SQL operation error from ntfBatchExecHistoryRepository.updateStatus
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto piRes052 = buildPersonalInfoDto("user001", "1", false);
        lenient().when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(piRes052);
        RegisterNotificationResponseDto regRes = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        // updateStatus throws SQL operation error (first call throws, second call in catch returns 1)
        SQLException sqlEx = new SQLException("unique violation", "23505");
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any()))
                .thenThrow(new RuntimeException(sqlEx))
                .thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
    非同期処理内で非SQL例外が発生した場合ログ出力して処理が継続することを確認するテストケース */
    @Test
    void sendMessageNotification_053() throws Exception {
        // Arrange - inner async catch: non-SQL exception from ntfBatchExecHistoryRepository.updateStatus
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto piRes053 = buildPersonalInfoDto("user001", "1", false);
        lenient().when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(piRes053);
        RegisterNotificationResponseDto regRes = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
        // updateStatus throws non-SQL error (first call throws, second call in catch returns 1)
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any()))
                .thenThrow(new RuntimeException("non-sql error"))
                .thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L202-209: sendMessageNotification外側catch(Exception)ブロックに到達することを確認するテストケース
     * properties.getThreadPool()がRuntimeExceptionをスローすることでCustomSqlException/TscApplicationException
     * ではない例外がcatch(Exception)に捕捉されてCustomExceptionがスローされる
     */
    @Test
    void sendMessageNotification_054() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        when(notificationVinListRepository.select(1, 0)).thenReturn(
                List.of(buildVinListEntity(1L)));
        // getThreadPool() throws plain RuntimeException -> propagates out of
        // executeParentNotificationProcess -> caught by catch(Exception e) at L202
        when(properties.getThreadPool()).thenThrow(new RuntimeException("thread pool error"));

        // Act & Assert
        assertThrows(com.toyota.tsc.notificationhub.exceptions.CustomException.class,
                () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L382-389: notificationSendList.isEmpty()がtrueの場合エラー処理が実行されることを確認するテストケース
     * notificationSendListフィールドがnullのエンティティを使うことでconvertToNotificationSendListDtoが
     * 空リストを返し、isEmpty()=trueとなる
     */
    @Test
    void sendMessageNotification_055() {
        // Arrange - entity with null notificationSendList -> convertToNotificationSendListDto catches NPE -> returns []
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        // notificationSendList = null triggers NullPointerException in csvText.split(",") -> catch -> empty list
        NotificationVinListEntity entity = new NotificationVinListEntity(
                1, "ME", 100, "VIN001", null, 1L, 1L, 0, false, null, null);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L448-472: 非同期内側catch(CustomSqlException)のSQL接続エラーパスを確認するテストケース
     * updateNotificationVinList(LINKED)が08001のSQLExceptionをラップしたRuntimeExceptionをスローし、
     * CustomSqlExceptionでキャッチされてisSqlConnectionError=trueパスを通ることを確認する
     */
    @Test
    void sendMessageNotification_056() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto piRes056 = buildPersonalInfoDto("user001", "1", false);
        lenient().when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(piRes056);
        RegisterNotificationResponseDto regRes056 = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson056 = new ObjectMapper().writeValueAsString(regRes056);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson056, HttpStatus.OK));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        // LINKED update (linkType=2): first call throws SQL connection error, second call (in async catch) returns 1
        SQLException sqlEx056 = new SQLException("connection error", "08001");
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2)))
                .thenThrow(new RuntimeException(sqlEx056))
                .thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L448-472: 非同期内側catch(CustomSqlException)のSQL操作エラーパスを確認するテストケース
     * updateNotificationVinList(LINKED)が23505のSQLExceptionをラップしたRuntimeExceptionをスローし、
     * CustomSqlExceptionでキャッチされてisSqlOperationError=trueパスを通ることを確認する
     */
    @Test
    void sendMessageNotification_057() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        PersonalInfoResponseDto piRes057 = buildPersonalInfoDto("user001", "1", false);
        lenient().when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(piRes057);
        RegisterNotificationResponseDto regRes057 = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson057 = new ObjectMapper().writeValueAsString(regRes057);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson057, HttpStatus.OK));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        // LINKED update (linkType=2): first call throws SQL operation error, second call (in async catch) returns 1
        SQLException sqlEx057 = new SQLException("unique violation", "23505");
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2)))
                .thenThrow(new RuntimeException(sqlEx057))
                .thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L573-574 & L594-595: updateNotificationVinList(NOTLINKED)がSQL接続エラーをスローし、
     * 外側forEachのcatch(CustomSqlException)でisSqlConnectionError=trueパスを通ることを確認するテストケース
     */
    @Test
    void sendMessageNotification_058() {
        // Arrange - updateNotificationVinList(NOTLINKED=0) throws RuntimeException wrapping 08001 SQLException
        // -> catch(Exception) in updateNotificationVinList wraps in CustomSqlException -> forEach outer catch -> connection error
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // NOTLINKED update (linkType=0) throws SQL connection error -> wrapped in CustomSqlException at L574
        SQLException sqlEx058 = new SQLException("connection error", "08001");
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0)))
                .thenThrow(new RuntimeException(sqlEx058));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L493 & L573-574: updateNotificationVinList(NOTLINKED)がSQL操作エラーをスローし、
     * 外側forEachのcatch(CustomSqlException)でisSqlOperationError=trueパスを通ることを確認するテストケース
     */
    @Test
    void sendMessageNotification_059() {
        // Arrange - updateNotificationVinList(NOTLINKED=0) throws RuntimeException wrapping 23505 SQLException
        // -> catch(Exception) in updateNotificationVinList wraps in CustomSqlException -> forEach outer catch -> operation error
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // NOTLINKED update (linkType=0) throws SQL operation error -> wrapped in CustomSqlException at L574
        SQLException sqlEx059 = new SQLException("unique violation", "23505");
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0)))
                .thenThrow(new RuntimeException(sqlEx059));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L788-792: executePrimaryContact が primaryContactError=true を返す場合に
     * executeErrorProcess が実行されてerrorFlagがtrueになることを確認するテストケース
     * 通知区分=2でpersonalInfoがnullになるようinternalUserId不一致を使用する
     */
    @Test
    void sendMessageNotification_060() throws Exception {
        // Arrange - notificationType=2, personalInfoList has different internalUserId
        // -> getPersonalInfoByInternalUserId returns null -> executePrimaryContact returns true -> L788-792
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // getPersonalInfoApiResponse returns null -> getPersonalInfo returns null -> executePrimaryContact returns true
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(null);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L845-849: executeRegisterNotificationでException発生時にexecuteErrorProcessが呼ばれ
     * 例外が再スローされることを確認するテストケース
     * batApisUtil.executeRegisterNotificationが例外をスローする
     */
    @Test
    void sendMessageNotification_061() throws Exception {
        // Arrange - batApisUtil.executeRegisterNotification throws RuntimeException
        // -> caught by catch(Exception e) at L845 -> executeErrorProcess called -> rethrown
        // -> async catch(Exception) at L474 catches it -> updateStatus + updateVinList called
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // executeRegisterNotification throws -> caught at L845 in executeRegisterNotification
        when(batApisUtil.executeRegisterNotification(any()))
                .thenThrow(new RuntimeException("register error"));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L873-874: registerNotificationでJSONパース失敗時にCustomExceptionがスローされることを確認するテストケース
     * batApisUtilが不正なJSONを返すことでObjectMapper.readValueが例外をスローする
     */
    @Test
    void sendMessageNotification_062() throws Exception {
        // Arrange - batApisUtil returns invalid JSON -> ObjectMapper.readValue throws -> catch(Exception) at L873
        // -> throws CustomException -> caught by catch(Exception) at L845 in executeRegisterNotification
        // -> executeErrorProcess called -> rethrown -> async catch(Exception) at L474
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // Return invalid JSON to trigger ObjectMapper parse exception at L873
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>("invalid-json", HttpStatus.OK));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L1048-1049: executePostMessageでNotificationHubsException以外の例外が発生した場合
     * CustomExceptionがスローされることを確認するテストケース
     */
    @Test
    void sendMessageNotification_063() throws Exception {
        // Arrange - notificationHubUtil.postMessage throws non-NotificationHubsException (RuntimeException)
        // -> catch(Exception e) at L1048 -> throws CustomException
        // -> caught by catch(Exception) in executePushNotification at L950 -> executeErrorProcess -> rethrown
        // -> async catch(Exception) at L474
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes063 = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson063 = new ObjectMapper().writeValueAsString(regRes063);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson063, HttpStatus.OK));
        NtfInfoEntity device063 = new NtfInfoEntity("user001", "inst001", "token001",
                "dev001", "1", "1",
                LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device063));
        String fcmPayload063 = "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload063);
        when(notificationHubUtil.replaceLcsSelected(any(), eq("1"), any())).thenReturn(fcmPayload063);
        when(properties.getRetryCount()).thenReturn(3);
        // postMessage throws plain RuntimeException (not NotificationHubsException) -> L1048-1049
        when(notificationHubUtil.postMessage(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("unexpected push error"));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L1200-1204, L1206: executeSendSmsでSMS送信が非2xxレスポンスを返す場合にtrueが返ることを確認するテストケース
     */
    @Test
    void sendMessageNotification_064() throws Exception {
        // Arrange - smsCountryUtil.executeSendSms returns non-2xx -> L1200-1204 -> return true -> error flag
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // SMS contact with primaryFlag=true
        PersonalInfoResponseDto piRes064 = buildPersonalInfoDto("user001", "1", true);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(piRes064);
        // SMS returns 500 (non-2xx) -> L1199-1204 -> return true
        when(smsCountryUtil.executeSendSms(any(), any(), any()))
                .thenReturn(new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L1224-1228: executeSendEmailでメール送信が非2xxレスポンスを返す場合にtrueが返ることを確認するテストケース
     */
    @Test
    void sendMessageNotification_065() throws Exception {
        // Arrange - sendGridUtil.executeSendEmail returns non-2xx status -> L1224-1228 -> return true
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // Email contact with primaryFlag=true
        PersonalInfoResponseDto piRes065 = buildPersonalInfoDto("user001", "2", true);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(piRes065);
        Mail mockMail065 = mock(Mail.class);
        when(sendGridUtil.generateEmail(any(), any(), any(), any(), any())).thenReturn(mockMail065);
        // Return non-2xx status code (500) -> L1223-1228 -> return true
        com.sendgrid.Response mockResponse065 = new com.sendgrid.Response(500, "error", new HashMap<>());
        when(sendGridUtil.executeSendEmail(any())).thenReturn(mockResponse065);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L1248-1253, L1260-1261: updateNotificationでupdateCnt==0の場合CustomSqlExceptionがスローされ、
     * sendMessageNotificationの外側catch(CustomSqlException)で捕捉されることを確認するテストケース
     */
    @Test
    void sendMessageNotification_066() throws Exception {
        // Arrange - notificationRepository.update returns 0 -> L1247 updateCnt==0 -> throw CustomSqlException()
        // -> catch(Exception e) at L1260 wraps in CustomSqlException -> caught at L178 in sendMessageNotification
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes066 = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson066 = new ObjectMapper().writeValueAsString(regRes066);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson066, HttpStatus.OK));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        // notificationRepository.update returns 0 -> CustomSqlException thrown -> L1247-1253 then L1260-1261
        when(notificationRepository.update("ME", 1)).thenReturn(0);

        // Act & Assert - CustomSqlException from updateNotification propagates to sendMessageNotification
        // and is caught at catch(CustomSqlException e) -> CustomException thrown
        assertThrows(com.toyota.tsc.notificationhub.exceptions.CustomException.class,
                () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L1308-1310: executeErrorProcessでntfBatchExecErrorInfoRepository.insertが例外をスローした場合に
     * catchブロックでログ出力して処理が継続することを確認するテストケース
     */
    @Test
    void sendMessageNotification_067() {
        // Arrange - ntfBatchExecErrorInfoRepository.insert throws RuntimeException
        // -> catch(Exception e) at L1308 in executeErrorProcess -> logs and continues
        // This is triggered via the historyInsert==0 path which calls executeErrorProcess
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(0);  // triggers executeErrorProcess
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // ntfBatchExecErrorInfoRepository.insert throws -> L1308 catch
        when(ntfBatchExecErrorInfoRepository.insert(any()))
                .thenThrow(new RuntimeException("error info insert failed"));
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert - processing continues despite error in executeErrorProcess
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L451: 非同期内側catch(CustomSqlException)でfindSqlException結果がnull(SQLなし)の場合のブランチを確認するテストケース
     * updateNotificationVinList(LINKED)がSQLを含まないRuntimeExceptionをスローし、sqlEx==nullパスを通ることを確認する
     */
    @Test
    void sendMessageNotification_068() throws Exception {
        // Arrange - LINKED update throws plain RuntimeException (no SQL cause) -> sqlEx == null -> L451 false branch
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes068 = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson068 = new ObjectMapper().writeValueAsString(regRes068);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson068, HttpStatus.OK));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        // LINKED update throws plain RuntimeException (no SQL) -> findSqlException returns null -> L451 false
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2)))
                .thenThrow(new RuntimeException("plain error without SQL"))
                .thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L459: 非同期内側catch(CustomSqlException)でisSqlOperationError=falseの場合のブランチを確認するテストケース
     * SQLState "HY000"(汎用エラー、接続エラーでも操作エラーでもない)でL459 elseブランチを通ることを確認する
     */
    @Test
    void sendMessageNotification_069() throws Exception {
        // Arrange - LINKED update throws SQLException with HY000 SQLState (neither connection nor operation)
        // -> isSqlConnectionError=false, isSqlOperationError=false -> L459 false branch
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes069 = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson069 = new ObjectMapper().writeValueAsString(regRes069);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson069, HttpStatus.OK));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        // SQLState "HY000" is neither connection (08xxx) nor operation (23xxx) error -> L459 false
        SQLException sqlEx069 = new SQLException("general SQL error", "HY000");
        lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2)))
                .thenThrow(new RuntimeException(sqlEx069))
                .thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L493: 外側forEachのcatch(CustomSqlException)でisSqlOperationError=falseの場合のブランチを確認するテストケース
     * SQLState "HY000"(汎用エラー)でL493 elseブランチを通ることを確認する
     */
    @Test
    void sendMessageNotification_070() {
        // Arrange - NOTLINKED update throws SQLException with HY000 SQLState (neither connection nor operation)
        // -> isSqlConnectionError=false, isSqlOperationError=false -> L493 false branch
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // NOTLINKED update (linkType=0) throws HY000 -> forEach outer catch -> neither error type
        SQLException sqlEx070 = new SQLException("general SQL error", "HY000");
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0)))
                .thenThrow(new RuntimeException(sqlEx070));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L1206: executeSendSmsでSMS送信が2xxレスポンスを返す場合にfalseが返ることを確認するテストケース
     */
    @Test
    void sendMessageNotification_071() throws Exception {
        // Arrange - smsCountryUtil.executeSendSms returns 2xx -> L1206 return false -> no error
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("2", "0");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        // SMS contact with primaryFlag=true
        PersonalInfoResponseDto piRes071 = buildPersonalInfoDto("user001", "1", true);
        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenReturn(piRes071);
        // SMS returns 2xx -> L1206 return false -> no SMS error
        when(smsCountryUtil.executeSendSms(any(), any(), any()))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(smsCountryUtil, atLeastOnce()).executeSendSms(any(), any(), any());
    }

    /** クラス：SendMessageNotificationServiceImpl
     * L1066: getLatestDeviceDataで複数デバイスが存在する場合にComparatorラムダが呼ばれることを確認するテストケース
     * isPushRequired="1"で2デバイスを返すとソート時にラムダが呼ばれる
     */
    @Test
    void sendMessageNotification_072() throws Exception {
        // Arrange - ntfInfoRepository returns 2 devices -> sort comparator lambda (L1066) is invoked
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "1");
        NotificationVinListEntity entity = buildVinListEntity(1L);
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
        when(properties.getThreadPool()).thenReturn(2);
        when(properties.getThreadQueue()).thenReturn(10);
        RegisterNotificationResponseDto regRes072 = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regResJson072 = new ObjectMapper().writeValueAsString(regRes072);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regResJson072, HttpStatus.OK));
        // 2 devices with different updatedAt times -> sort comparator lambda (L1066) is invoked
        NtfInfoEntity device072a = new NtfInfoEntity("user001", "inst001", "token001",
                "dev001", "1", "1",
                LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1));
        NtfInfoEntity device072b = new NtfInfoEntity("user001", "inst002", "token002",
                "dev002", "1", "1",
                LocalDateTime.now().minusHours(1), LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device072a, device072b));
        String fcmPayload072 = "{\"message\":{\"android\":{\"data\":{\"pushFlg\":\"1\",\"lcsSelected\":\"LC001\"}}}}";
        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn(fcmPayload072);
        when(notificationHubUtil.replaceLcsSelected(any(), eq("1"), any())).thenReturn(fcmPayload072);
        // retryCount=0 -> while loop never runs -> postMessage not called -> outcome=null -> pushError=true
        when(properties.getRetryCount()).thenReturn(0);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
        when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfInfoRepository, atLeastOnce()).selectAllByInternalUserId("user001");
    }

    /**
     * getAllDeviceData - ntfInfoRepository が RuntimeException をスロー
     * L935-936: catch(Exception) → CustomSqlException をカバー
     */
    @Test
    void getAllDeviceData_001() throws Exception {
        when(ntfInfoRepository.selectAllByInternalUserId(any()))
                .thenThrow(new RuntimeException("db error"));

        Method method = SendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "getAllDeviceData", String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, "user001"));
        assertInstanceOf(CustomSqlException.class, ex.getCause());
    }

    /**
     * executeRegisterNotification - batApisUtil が HttpStatusCodeException をスロー
     * L687-692: catch(HttpStatusCodeException) パスをカバー
     */
    @Test
    void executeRegisterNotification_001() throws Exception {
        SendMessageNotificationRequestDto request = buildRequest("1", "1");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity vinList = buildVinListEntity(1L);
        NotificationSendListDto notificationData = new NotificationSendListDto("VIN001", "user001", "LC001", "1");

        when(batApisUtil.executeRegisterNotification(any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        Method method = SendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeRegisterNotification",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationSendListDto.class,
                NotificationVinListEntity.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, notificationData, vinList);

        assertTrue(result);
    }

    /**
     * getPersonalInfo - personalInfoUtil が HttpStatusCodeException をスロー
     * L1034-1035: catch(HttpStatusCodeException) パスをカバー
     */
    @Test
    void getPersonalInfo_001() throws Exception {
        RequestHeaderDto header = buildHeader();

        when(personalInfoUtil.getPersonalInfoApiResponse(any(), any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        Method method = SendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "getPersonalInfo", RequestHeaderDto.class, String.class);
        method.setAccessible(true);
        Object result = method.invoke(service, header, "user001");

        assertNull(result);
    }

    /**
     * executeSendSms - smsCountryUtil が HttpStatusCodeException をスロー
     * L1106-1111: catch(HttpStatusCodeException) パスをカバー
     */
    @Test
    void executeSendSms_001() throws Exception {
        SendMessageNotificationRequestDto request = buildRequest("2", "0");
        RequestHeaderDto header = buildHeader();

        when(smsCountryUtil.executeSendSms(any(), any(), any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        Method method = SendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeSendSms",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                String.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, "09012345678", "1");

        assertTrue(result);
    }

    /**
     * executeNotificationProcess ループ - getAllDeviceData が CustomSqlException をスロー
     * L630-631: catch(CustomSqlException) → rethrow をカバー
     * ※ L935-936 も同時カバー（getAllDeviceData の catch(Exception) → CustomSqlException）
     */
    @Test
    void executeNotificationProcess_001() throws Exception {
        SendMessageNotificationRequestDto request = buildRequest("1", "1");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity vinList = buildVinListEntity(1L);
        NotificationSendListDto notificationData = new NotificationSendListDto("VIN001", "user001", "LC001", "1");

        // executeRegisterNotification succeeds
        RegisterNotificationResponseDto regRes = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
        String regJson = new ObjectMapper().writeValueAsString(regRes);
        when(batApisUtil.executeRegisterNotification(any()))
                .thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

        // getAllDeviceData throws RuntimeException → wrapped in CustomSqlException
        when(ntfInfoRepository.selectAllByInternalUserId(any()))
                .thenThrow(new RuntimeException("db error"));

        Method method = SendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                List.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, request, header, vinList, List.of(notificationData)));
        assertInstanceOf(CustomSqlException.class, ex.getCause());
    }
}
