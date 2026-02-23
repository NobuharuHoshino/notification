package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.commons.*;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.models.*;
import com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto.PersonalInfoList;
import com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto.ContactDto;
import com.toyota.tsc.notificationhub.models.SmsContextDto;
import com.toyota.tsc.notificationhub.models.MailContextDto;
import com.toyota.tsc.notificationhub.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SaSendMessageNotificationServiceImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class SaSendMessageNotificationServiceImplTest {

    @InjectMocks
    private SaSendMessageNotificationServiceImpl service;

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
    private JsapUtil jsapUtil;
    @Mock
    private NotificationHubUtil notificationHubUtil;
    @Mock
    private PropertiesUtil properties;

    private RequestHeaderDto buildHeader() {
        RequestHeaderDto header = new RequestHeaderDto();
        header.setCorrelationId("corr-sa-001");
        return header;
    }

    private SendMessageNotificationRequestDto buildRequest(String notificationType, String isPushRequired) {
        SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
        req.setRegistrationSerialNumber(1);
        req.setNotificationType(notificationType);
        req.setIsPushNotificationRequired(isPushRequired);
        req.setScheduledSendData("2024-01-01");
        req.setTitle("Title");
        req.setBodySms("SMS Body");
        req.setBodyText("Text Body");
        req.setBodyHtml("<html>HTML</html>");
        req.setPayload("{\"pushFlg\":\"1\"}");
        req.setNotificationContents(new ArrayList<>());
        return req;
    }

    private NotificationVinListEntity buildVinListEntity(String seqNum) {
        return new NotificationVinListEntity(1, "SA", 100, "VIN001",
                "VIN001:user001:LC001:1", 1L, seqNum, 0, false, null, null);
    }

    private String buildGetUserIdResponseJson(String resultCode, String userId) throws Exception {
        GetUserIdResponseDto dto = new GetUserIdResponseDto(resultCode, userId, "user001");
        return new ObjectMapper().writeValueAsString(dto);
    }

    private String buildTokenResponseJson() throws Exception {
        GetAccessTokenResponseDto dto = new GetAccessTokenResponseDto("test-token", "Bearer", "3600", "scope", "jti");
        return new ObjectMapper().writeValueAsString(dto);
    }

    /** ヘルパー: PersonalInfoListを構築 */
    private PersonalInfoList buildPersonalInfoList(String internalUserId, String userId, List<ContactDto> contacts) {
        PersonalInfoList info = new PersonalInfoList();
        info.setInternalUserId(internalUserId);
        info.setUserId(userId);
        info.setContactList(contacts);
        return info;
    }

    /** ヘルパー: ContactDtoを構築 */
    private ContactDto buildContactDto(String contactType, boolean isPrimary, String contact) {
        ContactDto dto = new ContactDto();
        dto.setContactType(contactType);
        dto.setPrimaryContactFlag(isPrimary);
        dto.setContact(contact);
        return dto;
    }

    /** ヘルパー: NtfInfoEntityを構築 */
    private NtfInfoEntity buildNtfInfoEntity(String internalUserId, String platformType) {
        return new NtfInfoEntity(
                internalUserId, "install-001", "device-token-001", "device-001",
                "1", platformType, LocalDateTime.now(), LocalDateTime.now());
    }

    /** ヘルパー: PersonalInfoListResponseDto（有効な個人情報リスト付き）を構築 */
    private PersonalInfoListResponseDto buildPersonalInfoListResponseDto(List<PersonalInfoList> infoList) {
        PersonalInfoListResponseDto dto = new PersonalInfoListResponseDto();
        dto.setResultCode("00001581U000");
        dto.setPersonalInfoList(infoList);
        return dto;
    }

    /** ヘルパー: SaNotificationSendListDtoリストを構築 */
    private List<SaNotificationSendListDto> buildNotificationSendList(String internalUserId, String userId) {
        SaNotificationSendListDto dto = new SaNotificationSendListDto(
                "VIN001", internalUserId, "LC001", "1", userId);
        return List.of(dto);
    }

    /** クラス：SaSendMessageNotificationServiceImpl requestがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_001() {
        // Arrange
        RequestHeaderDto header = buildHeader();

        // Act & Assert
        assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(null, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl registrationSerialNumberがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_002() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
        req.setNotificationType("1");

        // Act & Assert
        assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl notificationTypeがnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_003() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
        req.setRegistrationSerialNumber(1);

        // Act & Assert
        assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl notificationTypeが空文字の場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_004() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
        req.setRegistrationSerialNumber(1);
        req.setNotificationType("");

        // Act & Assert
        assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl registrationSerialNumberとnotificationTypeが両方nullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_005() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();

        // Act & Assert
        assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl ユーザー情報がnullの場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_006() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        when(notificationVinListRepository.select(1, 0)).thenReturn(null);

        // Act & Assert
        assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl ユーザー情報が空の場合TscApplicationExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_007() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        when(notificationVinListRepository.select(1, 0)).thenReturn(Collections.emptyList());

        // Act & Assert
        assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl SQL接続エラー時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_008() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        SQLException sqlEx = new SQLException("connection error", "08001");
        when(notificationVinListRepository.select(any(), any())).thenThrow(new RuntimeException(sqlEx));

        // Act & Assert
        assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl SQL操作エラー時にCustomSqlExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_009() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        SQLException sqlEx = new SQLException("unique violation", "23505");
        when(notificationVinListRepository.select(any(), any())).thenThrow(new RuntimeException(sqlEx));

        // Act & Assert
        assertThrows(CustomSqlException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl その他SQLエラー時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_010() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        SQLException sqlEx = new SQLException("other", "99999");
        when(notificationVinListRepository.select(any(), any())).thenThrow(new RuntimeException(sqlEx));

        // Act & Assert
        assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl 非SQL例外時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_011() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        when(notificationVinListRepository.select(any(), any())).thenThrow(new RuntimeException("non-sql"));

        // Act & Assert
        assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl historyInsertが0件の場合CustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_012() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(0);

        // Act & Assert
        assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl getUserId失敗時にnullが返ることを確認するテストケース */
    @Test
    void sendMessageNotification_013() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        // GetUserId returns failure result code
        String failJson = buildGetUserIdResponseJson("99999999", null);
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(failJson, HttpStatus.OK));
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SaSendMessageNotificationServiceImpl vinListUpdate(NOTLINKED)が0件の場合CustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_014() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(0);

        // Act & Assert
        assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl トークン取得がnullの場合リターンすることを確認するテストケース */
    @Test
    void sendMessageNotification_015() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        // Token response non-2xx
        when(jsapUtil.executeGetToken())
                .thenReturn(new ResponseEntity<>("{\"access_token\":null}", HttpStatus.INTERNAL_SERVER_ERROR));
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SaSendMessageNotificationServiceImpl 個人情報APIレスポンスがnullの場合エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_016() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));
        when(properties.getParallelCurrent()).thenReturn(1);
        when(jsapUtil.executeGetUserInfo(any())).thenReturn(null);
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        verify(ntfBatchExecErrorInfoRepository, times(1)).insert(any());
    }

    /** クラス：SaSendMessageNotificationServiceImpl 個人情報APIが非2xxの場合nullが返ることを確認するテストケース */
    @Test
    void sendMessageNotification_017() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));
        when(properties.getParallelCurrent()).thenReturn(1);
        when(jsapUtil.executeGetUserInfo(any()))
                .thenReturn(new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SaSendMessageNotificationServiceImpl 個人情報APIレスポンスボディが空の場合nullが返ることを確認するテストケース */
    @Test
    void sendMessageNotification_018() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));
        when(properties.getParallelCurrent()).thenReturn(1);
        when(jsapUtil.executeGetUserInfo(any()))
                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SaSendMessageNotificationServiceImpl 個人情報APIのresultCodeが正常でない場合nullが返ることを確認するテストケース */
    @Test
    void sendMessageNotification_019() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));
        when(properties.getParallelCurrent()).thenReturn(1);
        PersonalInfoListResponseDto errorDto = new PersonalInfoListResponseDto();
        errorDto.setResultCode("99999999");
        errorDto.setPersonalInfoList(new ArrayList<>());
        String resJson = new ObjectMapper().writeValueAsString(errorDto);
        when(jsapUtil.executeGetUserInfo(any()))
                .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SaSendMessageNotificationServiceImpl 個人情報リストが空の場合nullが返ることを確認するテストケース */
    @Test
    void sendMessageNotification_020() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));
        when(properties.getParallelCurrent()).thenReturn(1);
        PersonalInfoListResponseDto emptyDto = new PersonalInfoListResponseDto();
        emptyDto.setResultCode("00001581U000");
        emptyDto.setPersonalInfoList(Collections.emptyList());
        String resJson = new ObjectMapper().writeValueAsString(emptyDto);
        when(jsapUtil.executeGetUserInfo(any()))
                .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SaSendMessageNotificationServiceImpl getPersonalInfoListがnullを返す場合（resultCode不一致）エラー処理が実行されることを確認するテストケース */
    @Test
    void sendMessageNotification_021() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));
        when(properties.getParallelCurrent()).thenReturn(1);
        // resultCode不一致でgetPersonalInfoListがnullを返すケース
        PersonalInfoListResponseDto personalDto = new PersonalInfoListResponseDto();
        personalDto.setResultCode("ERROR_CODE");
        String resJson = new ObjectMapper().writeValueAsString(personalDto);
        when(jsapUtil.executeGetUserInfo(any()))
                .thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
        // getPersonalInfoList returns null -> executeErrorProcess will be called
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SaSendMessageNotificationServiceImpl トークンが空文字の場合リターンすることを確認するテストケース */
    @Test
    void sendMessageNotification_022() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        // Token returns empty access_token
        GetAccessTokenResponseDto emptyToken = new GetAccessTokenResponseDto("", "Bearer", "3600", "scope", "jti");
        String emptyTokenJson = new ObjectMapper().writeValueAsString(emptyToken);
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(emptyTokenJson, HttpStatus.OK));
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SaSendMessageNotificationServiceImpl getUserIdが例外を投げた場合CustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_023() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenThrow(new CustomException("jsap error"));

        // Act & Assert
        assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl getToken例外時にCustomExceptionが投げられることを確認するテストケース */
    @Test
    void sendMessageNotification_024() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        when(jsapUtil.executeGetToken()).thenThrow(new CustomException("token error"));

        // Act & Assert
        assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl TscApplicationExceptionが再スローされることを確認するテストケース */
    @Test
    void sendMessageNotification_025() {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();

        // Act & Assert
        assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
    }

    /** クラス：SaSendMessageNotificationServiceImpl resBodyがnullの場合nullが返ることを確認するテストケース(L533 null branch) */
    @Test
    void sendMessageNotification_026() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any())).thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));
        when(properties.getParallelCurrent()).thenReturn(1);
        // Return 200 with NULL body
        when(jsapUtil.executeGetUserInfo(any())).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    /** クラス：SaSendMessageNotificationServiceImpl personalInfoListがnullの場合nullが返ることを確認するテストケース(L542 null branch) */
    @Test
    void sendMessageNotification_027() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SendMessageNotificationRequestDto req = buildRequest("1", "0");
        NotificationVinListEntity entity = buildVinListEntity("1");
        when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
        String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        when(jsapUtil.executeGetUserId(any(), any())).thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
        when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));
        when(properties.getParallelCurrent()).thenReturn(1);
        // personalInfoList is null in DTO (not set, default null)
        PersonalInfoListResponseDto dto = new PersonalInfoListResponseDto();
        dto.setResultCode("00001581U000");
        // personalInfoList intentionally not set (null)
        String resJson = new ObjectMapper().writeValueAsString(dto);
        when(jsapUtil.executeGetUserInfo(any())).thenReturn(new ResponseEntity<>(resJson, HttpStatus.OK));
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
        when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
    }

    // =========================================================
    // Reflection-based tests for private methods
    // =========================================================

    /**
     * executeNotificationProcess (overloaded) - 正常系: notificationType="1" (NTF only), push not required
     * Register succeeds, push skipped (isPushRequired="0"), primaryContact skipped (TYPE_NTF only)
     */
    @Test
    void executeNotificationProcess_001() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "0");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");

        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalInfoListResDto = buildPersonalInfoListResponseDto(List.of(personalInfo));
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Token mock
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));

        // Register notification mock - success
        RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("000000", "ntf-001", "OK");
        String regJson = new ObjectMapper().writeValueAsString(regDto);
        when(batApisUtil.executeRegisterNotification(any())).thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                PersonalInfoListResponseDto.class,
                List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, userInfo, personalInfoListResDto, notificationSendList);

        // Assert
        assertFalse(result);
    }

    /**
     * executeNotificationProcess (overloaded) - トークンが空文字の場合エラーフラグがtrueになる
     */
    @Test
    void executeNotificationProcess_002() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "0");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");

        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalInfoListResDto = buildPersonalInfoListResponseDto(List.of(personalInfo));
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Token returns empty
        GetAccessTokenResponseDto emptyToken = new GetAccessTokenResponseDto("", "Bearer", "3600", "scope", "jti");
        String emptyTokenJson = new ObjectMapper().writeValueAsString(emptyToken);
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(emptyTokenJson, HttpStatus.OK));

        // Error process mocks
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                PersonalInfoListResponseDto.class,
                List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, userInfo, personalInfoListResDto, notificationSendList);

        // Assert
        assertTrue(result);
    }

    /**
     * executeNotificationProcess (overloaded) - トークンがnullの場合エラーフラグがtrueになる (token == null 分岐)
     */
    @Test
    void executeNotificationProcess_002b() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "0");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");

        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalInfoListResDto = buildPersonalInfoListResponseDto(List.of(personalInfo));
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Token returns null (non-2xx response causes getToken to return null)
        when(jsapUtil.executeGetToken()).thenReturn(
                new ResponseEntity<>("{\"access_token\":null,\"token_type\":\"Bearer\",\"expires_in\":\"3600\",\"scope\":\"sc\",\"jti\":\"jti\"}", HttpStatus.INTERNAL_SERVER_ERROR));

        // Error process mocks
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                PersonalInfoListResponseDto.class,
                List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, userInfo, personalInfoListResDto, notificationSendList);

        // Assert
        assertTrue(result);
    }

    /**
     * executeNotificationProcess (overloaded) - Register通知エラー時エラーフラグがtrueになる
     */
    @Test
    void executeNotificationProcess_003() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "0");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");

        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalInfoListResDto = buildPersonalInfoListResponseDto(List.of(personalInfo));
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Token mock
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));

        // Register notification mock - failure (returnCode != "000000")
        RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("999999", "ntf-001", "Error");
        String regJson = new ObjectMapper().writeValueAsString(regDto);
        when(batApisUtil.executeRegisterNotification(any())).thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

        // Error process mocks
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                PersonalInfoListResponseDto.class,
                List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, userInfo, personalInfoListResDto, notificationSendList);

        // Assert
        assertTrue(result);
    }

    /**
     * executeNotificationProcess (overloaded) - Pushエラー: デバイス情報が空でエラーフラグがtrueになる
     */
    @Test
    void executeNotificationProcess_004() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "1"); // isPushRequired=1
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");

        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalInfoListResDto = buildPersonalInfoListResponseDto(List.of(personalInfo));
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Token mock
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));

        // Register notification mock - success
        RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("000000", "ntf-001", "OK");
        String regJson = new ObjectMapper().writeValueAsString(regDto);
        when(batApisUtil.executeRegisterNotification(any())).thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

        // ntfInfoRepository returns empty list -> deviceData null -> push error
        when(ntfInfoRepository.selectAllByInternalUserId(any())).thenReturn(Collections.emptyList());

        // Error process mocks
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                PersonalInfoListResponseDto.class,
                List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, userInfo, personalInfoListResDto, notificationSendList);

        // Assert
        assertTrue(result);
    }

    /**
     * executeNotificationProcess (overloaded) - PrimaryContactエラー: notificationType="3", sendMessage失敗
     */
    @Test
    void executeNotificationProcess_005() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("3", "0"); // TYPE_NTF_AND_MAILSMS, push not required
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");

        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalInfoListResDto = buildPersonalInfoListResponseDto(List.of(personalInfo));
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Token mock
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));

        // Register notification mock - success
        RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("000000", "ntf-001", "OK");
        String regJson = new ObjectMapper().writeValueAsString(regDto);
        when(batApisUtil.executeRegisterNotification(any())).thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

        // SMS context mock
        when(jsapUtil.createSmsContext(any())).thenReturn(new SmsContextDto("sms-body"));

        // executeSendMessage fails
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("999999", "Error");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // Error process mocks
        when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                PersonalInfoListResponseDto.class,
                List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, userInfo, personalInfoListResDto, notificationSendList);

        // Assert
        assertTrue(result);
    }

    /**
     * executeNotificationProcess (overloaded) - 正常系: notificationType="3", push=1, primaryContact (phone) 成功
     */
    @Test
    void executeNotificationProcess_006() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("3", "1"); // NTF_AND_MAILSMS, push required
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");

        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalInfoListResDto = buildPersonalInfoListResponseDto(List.of(personalInfo));
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Token mock
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));

        // Register notification mock - success
        RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("000000", "ntf-001", "OK");
        String regJson = new ObjectMapper().writeValueAsString(regDto);
        when(batApisUtil.executeRegisterNotification(any())).thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

        // Device data mock - FCM
        NtfInfoEntity device = buildNtfInfoEntity("user001", "1");
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn("fcm-payload");
        when(notificationHubUtil.replaceLcsSelected(any(), eq("1"), any())).thenReturn("final-payload");

        // Push response mock - success
        PushRequestResponseDto pushDto = new PushRequestResponseDto("000000", "OK");
        String pushJson = new ObjectMapper().writeValueAsString(pushDto);
        when(jsapUtil.executePushRequest(any(), any(), any())).thenReturn(new ResponseEntity<>(pushJson, HttpStatus.OK));

        // SMS context mock
        when(jsapUtil.createSmsContext(any())).thenReturn(new SmsContextDto("sms-body"));

        // executeSendMessage success
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("000000", "OK");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                PersonalInfoListResponseDto.class,
                List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, userInfo, personalInfoListResDto, notificationSendList);

        // Assert
        assertFalse(result);
    }

    /**
     * executeRegisterNotification - notificationType="2" (TYPE_MAILSMS) はregister不要なのでfalseを返す
     */
    @Test
    void executeRegisterNotification_001() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("2", "0"); // TYPE_MAILSMS only
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("2", true, "test@example.com");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeRegisterNotification",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo);

        // Assert
        assertFalse(result);
    }

    /**
     * executeRegisterNotification - notificationType="1" (TYPE_NTF), register成功でfalseを返す
     */
    @Test
    void executeRegisterNotification_002() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // Register notification mock - success
        RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("000000", "ntf-001", "OK");
        String regJson = new ObjectMapper().writeValueAsString(regDto);
        when(batApisUtil.executeRegisterNotification(any())).thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeRegisterNotification",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo);

        // Assert
        assertFalse(result);
    }

    /**
     * executeRegisterNotification - notificationType="3" (TYPE_NTF_AND_MAILSMS), register失敗でtrueを返す
     */
    @Test
    void executeRegisterNotification_003() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("3", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // Register notification mock - failure
        RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("999999", "ntf-001", "Error");
        String regJson = new ObjectMapper().writeValueAsString(regDto);
        when(batApisUtil.executeRegisterNotification(any())).thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeRegisterNotification",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo);

        // Assert
        assertTrue(result);
    }

    /**
     * executeRegisterNotification - registerNotification が null を返した場合 CustomException がスローされる
     */
    @Test
    void executeRegisterNotification_004() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // Register notification mock - returns response with invalid JSON -> mapper throws
        when(batApisUtil.executeRegisterNotification(any())).thenReturn(new ResponseEntity<>("invalid-json", HttpStatus.OK));

        // Act & Assert
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeRegisterNotification",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, request, header, personalInfo));
        assertInstanceOf(CustomException.class, ex.getCause());
    }

    /**
     * executePushNotification - isPushRequired="0" の場合、push処理をスキップしてfalseを返す
     */
    @Test
    void executePushNotification_001() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "0"); // push not required
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        NotificationVinListEntity userInfo = buildVinListEntity("1");
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executePushNotification",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                NotificationVinListEntity.class,
                List.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, userInfo, notificationSendList, "test-token");

        // Assert
        assertFalse(result);
    }

    /**
     * executePushNotification - APNプラットフォーム (platformType="2") の場合ペイロード構築成功でfalseを返す
     */
    @Test
    void executePushNotification_002() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "1"); // push required
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        NotificationVinListEntity userInfo = buildVinListEntity("1");
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Device data mock - APN
        NtfInfoEntity device = buildNtfInfoEntity("user001", "2");
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        when(notificationHubUtil.buildApnsPayload(any())).thenReturn("apns-payload");
        when(notificationHubUtil.replaceLcsSelected(any(), eq("2"), any())).thenReturn("final-apns-payload");

        // Push response mock - success
        PushRequestResponseDto pushDto = new PushRequestResponseDto("000000", "OK");
        String pushJson = new ObjectMapper().writeValueAsString(pushDto);
        when(jsapUtil.executePushRequest(any(), any(), any())).thenReturn(new ResponseEntity<>(pushJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executePushNotification",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                NotificationVinListEntity.class,
                List.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, userInfo, notificationSendList, "test-token");

        // Assert
        assertFalse(result);
    }

    /**
     * executePushNotification - JSAP Push APIが非成功コードを返す場合でもfalseを返す（ログのみ記録）
     */
    @Test
    void executePushNotification_003() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "1"); // push required
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        NotificationVinListEntity userInfo = buildVinListEntity("1");
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Device data mock - FCM
        NtfInfoEntity device = buildNtfInfoEntity("user001", "1");
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn("fcm-payload");
        when(notificationHubUtil.replaceLcsSelected(any(), eq("1"), any())).thenReturn("final-payload");

        // Push response mock - failure (non-success code)
        PushRequestResponseDto pushDto = new PushRequestResponseDto("999999", "Error");
        String pushJson = new ObjectMapper().writeValueAsString(pushDto);
        when(jsapUtil.executePushRequest(any(), any(), any())).thenReturn(new ResponseEntity<>(pushJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executePushNotification",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                NotificationVinListEntity.class,
                List.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, userInfo, notificationSendList, "test-token");

        // Assert - push failure logs but returns false (not error)
        assertFalse(result);
    }

    /**
     * executePushNotification - デバイスなし (deviceData=null) の場合trueを返す
     */
    @Test
    void executePushNotification_004() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "1"); // push required
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        NotificationVinListEntity userInfo = buildVinListEntity("1");
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Device data mock - empty list -> null
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(Collections.emptyList());

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executePushNotification",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                NotificationVinListEntity.class,
                List.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, userInfo, notificationSendList, "test-token");

        // Assert
        assertTrue(result);
    }

    /**
     * executePrimaryContact - notificationType="1" (TYPE_NTF only) の場合スキップしてfalseを返す
     */
    @Test
    void executePrimaryContact_001() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("2", true, "test@example.com");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executePrimaryContact",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, "test-token");

        // Assert
        assertFalse(result);
    }

    /**
     * executePrimaryContact - notificationType="2" (TYPE_MAILSMS), sendRequest成功でfalseを返す
     */
    @Test
    void executePrimaryContact_002() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("2", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("2", true, "test@example.com");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // Mail context mock
        when(jsapUtil.createMailContext(any(), any())).thenReturn(List.of(new MailContextDto("text/plain", "body")));

        // sendMessage success
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("000000", "OK");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executePrimaryContact",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, "test-token");

        // Assert
        assertFalse(result);
    }

    /**
     * executePrimaryContact - notificationType="3" (TYPE_NTF_AND_MAILSMS), 電話番号送信失敗でtrueを返す
     */
    @Test
    void executePrimaryContact_003() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("3", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // SMS context mock
        when(jsapUtil.createSmsContext(any())).thenReturn(new SmsContextDto("sms-body"));

        // sendMessage failure
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("999999", "Error");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executePrimaryContact",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, "test-token");

        // Assert
        assertTrue(result);
    }

    /**
     * sendRequest - primaryContactFlag=false の場合スキップしてfalseを返す
     */
    @Test
    void sendRequest_001() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("2", "0");
        RequestHeaderDto header = buildHeader();
        // non-primary contact
        ContactDto contact = buildContactDto("2", false, "test@example.com");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "sendRequest",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, "test-token");

        // Assert - non-primary contact skipped, returns false
        assertFalse(result);
        verify(jsapUtil, never()).executeSendMessage(any(), any(), any(), any(), any(), any());
    }

    /**
     * sendRequest - contactType="2" (email) のプライマリ連絡先に送信成功でfalseを返す
     */
    @Test
    void sendRequest_002() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("2", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("2", true, "test@example.com");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // Mail context mock
        when(jsapUtil.createMailContext(any(), any())).thenReturn(List.of(new MailContextDto("text/plain", "body")));

        // sendMessage success
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("000000", "OK");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "sendRequest",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, "test-token");

        // Assert
        assertFalse(result);
    }

    /**
     * sendRequest - contactType="1" (SMS) のプライマリ連絡先に送信成功でfalseを返す
     */
    @Test
    void sendRequest_003() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("2", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // SMS context mock
        when(jsapUtil.createSmsContext(any())).thenReturn(new SmsContextDto("sms-body"));

        // sendMessage success
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("000000", "OK");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "sendRequest",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, personalInfo, "test-token");

        // Assert
        assertFalse(result);
    }

    /**
     * sendRequest - 未知のcontactType="3" の場合 CustomException がスローされる
     */
    @Test
    void sendRequest_004() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("2", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("3", true, "unknown");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));

        // Act & Assert
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "sendRequest",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                PersonalInfoList.class,
                String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, request, header, personalInfo, "test-token"));
        assertInstanceOf(CustomException.class, ex.getCause());
    }

    /**
     * executeSendMessage - 成功応答の場合falseを返す
     */
    @Test
    void executeSendMessage_001() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("2", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("2", true, "test@example.com");

        // sendMessage success
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("000000", "OK");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeSendMessage",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                ContactDto.class,
                String.class,
                boolean.class,
                Object.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, contact, "jsapUser001", true, new Object(), "test-token");

        // Assert
        assertFalse(result);
    }

    /**
     * executeSendMessage - 失敗応答の場合trueを返す
     */
    @Test
    void executeSendMessage_002() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("2", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("2", true, "test@example.com");

        // sendMessage failure
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("999999", "Error");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeSendMessage",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                ContactDto.class,
                String.class,
                boolean.class,
                Object.class,
                String.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, contact, "jsapUser001", true, new Object(), "test-token");

        // Assert
        assertTrue(result);
    }

    /**
     * executeSendMessage - 例外発生時 CustomException がスローされる
     */
    @Test
    void executeSendMessage_003() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("2", "0");
        RequestHeaderDto header = buildHeader();
        ContactDto contact = buildContactDto("2", true, "test@example.com");

        // sendMessage throws exception
        when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("network error"));

        // Act & Assert
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeSendMessage",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                ContactDto.class,
                String.class,
                boolean.class,
                Object.class,
                String.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, request, header, contact, "jsapUser001", true, new Object(), "test-token"));
        assertInstanceOf(CustomException.class, ex.getCause());
    }

    /**
     * createPayload - FCMプラットフォーム (platformType="1") の場合FCMペイロードを返す
     */
    @Test
    void createPayload_001() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "1");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");
        NtfInfoEntity deviceData = buildNtfInfoEntity("user001", "1"); // FCM

        when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn("fcm-payload");

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "createPayload",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                NtfInfoEntity.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, request, header, userInfo, deviceData);

        // Assert
        assertEquals("fcm-payload", result);
    }

    /**
     * createPayload - APNプラットフォーム (platformType="2") の場合APNSペイロードを返す
     */
    @Test
    void createPayload_002() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "1");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");
        NtfInfoEntity deviceData = buildNtfInfoEntity("user001", "2"); // APN

        when(notificationHubUtil.buildApnsPayload(any())).thenReturn("apns-payload");

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "createPayload",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                NtfInfoEntity.class);
        method.setAccessible(true);
        String result = (String) method.invoke(service, request, header, userInfo, deviceData);

        // Assert
        assertEquals("apns-payload", result);
    }

    /**
     * createPayload - 不明なプラットフォームタイプの場合 CustomException がスローされる
     */
    @Test
    void createPayload_003() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "1");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");
        NtfInfoEntity deviceData = buildNtfInfoEntity("user001", "99"); // Unknown

        // Act & Assert
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "createPayload",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                NtfInfoEntity.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, request, header, userInfo, deviceData));
        assertInstanceOf(CustomException.class, ex.getCause());
    }

    /**
     * getLatestDeviceData - デバイスリストが空の場合nullを返す
     */
    @Test
    void getLatestDeviceData_001() throws Exception {
        // Arrange
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(Collections.emptyList());

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "getLatestDeviceData", String.class);
        method.setAccessible(true);
        NtfInfoEntity result = (NtfInfoEntity) method.invoke(service, "user001");

        // Assert
        assertNull(result);
    }

    /**
     * getLatestDeviceData - 複数デバイスがある場合、最新のupdatedAtのものを返す
     */
    @Test
    void getLatestDeviceData_002() throws Exception {
        // Arrange
        NtfInfoEntity device1 = new NtfInfoEntity(
                "user001", "install-001", "token-001", "device-001",
                "1", "1", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(2));
        NtfInfoEntity device2 = new NtfInfoEntity(
                "user001", "install-002", "token-002", "device-002",
                "1", "2", LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(1));
        NtfInfoEntity device3 = new NtfInfoEntity(
                "user001", "install-003", "token-003", "device-003",
                "1", "1", LocalDateTime.now(), LocalDateTime.now());

        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device1, device2, device3));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "getLatestDeviceData", String.class);
        method.setAccessible(true);
        NtfInfoEntity result = (NtfInfoEntity) method.invoke(service, "user001");

        // Assert - should return device3 (most recent updatedAt)
        assertNotNull(result);
        assertEquals("install-003", result.getInstallationId());
    }

    /**
     * getLatestDeviceData - 単一デバイスがある場合、そのデバイスを返す
     */
    @Test
    void getLatestDeviceData_003() throws Exception {
        // Arrange
        NtfInfoEntity device = buildNtfInfoEntity("user001", "1");
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "getLatestDeviceData", String.class);
        method.setAccessible(true);
        NtfInfoEntity result = (NtfInfoEntity) method.invoke(service, "user001");

        // Assert
        assertNotNull(result);
        assertEquals(device.getInstallationId(), result.getInstallationId());
    }

    /**
     * getAllDeviceData - ntfInfoRepositoryからデバイスリストを取得できる
     */
    @Test
    void getAllDeviceData_001() throws Exception {
        // Arrange
        NtfInfoEntity device = buildNtfInfoEntity("user001", "1");
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "getAllDeviceData", String.class);
        method.setAccessible(true);
        List<NtfInfoEntity> result = (List<NtfInfoEntity>) method.invoke(service, "user001");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * getAllDeviceData - ntfInfoRepositoryが空リストを返す場合、空リストを返す
     */
    @Test
    void getAllDeviceData_002() throws Exception {
        // Arrange
        when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(Collections.emptyList());

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "getAllDeviceData", String.class);
        method.setAccessible(true);
        List<NtfInfoEntity> result = (List<NtfInfoEntity>) method.invoke(service, "user001");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * executeNotificationProcess (overloaded) - 複数personalInfoがある場合、すべてのユーザーを処理する
     */
    @Test
    void executeNotificationProcess_007() throws Exception {
        // Arrange - notificationType="1", push not required, two users
        SendMessageNotificationRequestDto request = buildRequest("1", "0");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");

        ContactDto contact1 = buildContactDto("1", true, "09012345678");
        PersonalInfoList info1 = buildPersonalInfoList("user001", "jsapUser001", List.of(contact1));
        ContactDto contact2 = buildContactDto("1", true, "09087654321");
        PersonalInfoList info2 = buildPersonalInfoList("user002", "jsapUser002", List.of(contact2));
        PersonalInfoListResponseDto personalInfoListResDto = buildPersonalInfoListResponseDto(List.of(info1, info2));

        SaNotificationSendListDto sendDto1 = new SaNotificationSendListDto("VIN001", "user001", "LC001", "1", "jsapUser001");
        SaNotificationSendListDto sendDto2 = new SaNotificationSendListDto("VIN002", "user002", "LC002", "1", "jsapUser002");
        List<SaNotificationSendListDto> notificationSendList = List.of(sendDto1, sendDto2);

        // Token mock
        String tokenJson = buildTokenResponseJson();
        when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));

        // Register notification mock - success for both
        RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("000000", "ntf-001", "OK");
        String regJson = new ObjectMapper().writeValueAsString(regDto);
        when(batApisUtil.executeRegisterNotification(any())).thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                PersonalInfoListResponseDto.class,
                List.class);
        method.setAccessible(true);
        Boolean result = (Boolean) method.invoke(service, request, header, userInfo, personalInfoListResDto, notificationSendList);

        // Assert
        assertFalse(result);
        verify(batApisUtil, times(2)).executeRegisterNotification(any());
    }

    /**
     * executeNotificationProcess (overloaded) - 例外発生時 CustomException がスローされる
     */
    @Test
    void executeNotificationProcess_008() throws Exception {
        // Arrange
        SendMessageNotificationRequestDto request = buildRequest("1", "0");
        RequestHeaderDto header = buildHeader();
        NotificationVinListEntity userInfo = buildVinListEntity("1");

        ContactDto contact = buildContactDto("1", true, "09012345678");
        PersonalInfoList personalInfo = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalInfoListResDto = buildPersonalInfoListResponseDto(List.of(personalInfo));
        List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001", "jsapUser001");

        // Token throws exception
        when(jsapUtil.executeGetToken()).thenThrow(new RuntimeException("token failure"));

        // Act & Assert
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "executeNotificationProcess",
                SendMessageNotificationRequestDto.class,
                RequestHeaderDto.class,
                NotificationVinListEntity.class,
                PersonalInfoListResponseDto.class,
                List.class);
        method.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                () -> method.invoke(service, request, header, userInfo, personalInfoListResDto, notificationSendList));
        assertInstanceOf(CustomException.class, ex.getCause());
    }

    // =========================================================
    // Additional tests for remaining branch coverage
    // =========================================================

    /**
     * 公開API経由: getPersonalInfoListが有効データを返し、executeNotificationProcess(5-arg)がerrorFlag=falseを返す場合
     * L319 non-null path, L332 false path, L542 false paths, L557 loop completion, L564 non-null path をカバー
     */
    @Test
    void sendMessageNotification_028() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        // notificationType="2" (MAILSMS only) → executeRegisterNotification skips
        // isPushNotificationRequired="0" → executePushNotification skips
        SendMessageNotificationRequestDto req = buildRequest("2", "0");

        NotificationVinListEntity entity = buildVinListEntity("1");
        lenient().when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        lenient().when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);

        // convertInternalUserIdToUserId success
        String userIdJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        lenient().when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(userIdJson, HttpStatus.OK));

        // updateNotificationVinList(NOTLINKED) success
        lenient().when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);

        // getToken success
        String tokenJson = buildTokenResponseJson();
        lenient().when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));

        // getPersonalInfoList returns valid data
        lenient().when(properties.getParallelCurrent()).thenReturn(1);
        ContactDto contact = buildContactDto("2", true, "test@example.com");
        PersonalInfoList infoItem = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalDto = buildPersonalInfoListResponseDto(List.of(infoItem));
        String personalJson = new ObjectMapper().writeValueAsString(personalDto);
        lenient().when(jsapUtil.executeGetUserInfo(any()))
                .thenReturn(new ResponseEntity<>(personalJson, HttpStatus.OK));

        // executePrimaryContact → sendRequest → executeSendMessage success (MAIL context)
        lenient().when(jsapUtil.createMailContext(any(), any()))
                .thenReturn(List.of(new MailContextDto("text/plain", "body")));
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("000000", "OK");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        lenient().when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // updateNotificationVinList(LINKED) success
        lenient().when(notificationVinListRepository.update(eq(1), eq("1"), eq(2))).thenReturn(1);

        // ntfBatchExecHistoryRepository.updateStatus
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);

        // notificationRepository.update
        lenient().when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        // Verify updateStatus was called with STATUS_DONE="1" (errorFlag=false path)
        verify(ntfBatchExecHistoryRepository, timeout(5000).atLeastOnce())
                .updateStatus(eq(1), eq(1), eq("1"));
    }

    /**
     * 公開API経由: getPersonalInfoListが有効データを返し、executeNotificationProcess(5-arg)がerrorFlag=trueを返す場合
     * L332 true path をカバー
     * executePrimaryContactでエラーが発生してerrorFlag=trueになるケース
     */
    @Test
    void sendMessageNotification_029() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        // notificationType="2" (MAILSMS only), isPushNotificationRequired="0"
        SendMessageNotificationRequestDto req = buildRequest("2", "0");

        NotificationVinListEntity entity = buildVinListEntity("1");
        lenient().when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
        lenient().when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);

        // convertInternalUserIdToUserId success
        String userIdJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
        lenient().when(jsapUtil.executeGetUserId(any(), any()))
                .thenReturn(new ResponseEntity<>(userIdJson, HttpStatus.OK));

        // updateNotificationVinList(NOTLINKED) success
        lenient().when(notificationVinListRepository.update(eq(1), eq("1"), eq(0))).thenReturn(1);

        // getToken success
        String tokenJson = buildTokenResponseJson();
        lenient().when(jsapUtil.executeGetToken()).thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));

        // getPersonalInfoList returns valid data
        lenient().when(properties.getParallelCurrent()).thenReturn(1);
        ContactDto contact = buildContactDto("2", true, "test@example.com");
        PersonalInfoList infoItem = buildPersonalInfoList("user001", "jsapUser001", List.of(contact));
        PersonalInfoListResponseDto personalDto = buildPersonalInfoListResponseDto(List.of(infoItem));
        String personalJson = new ObjectMapper().writeValueAsString(personalDto);
        lenient().when(jsapUtil.executeGetUserInfo(any()))
                .thenReturn(new ResponseEntity<>(personalJson, HttpStatus.OK));

        // executePrimaryContact → sendRequest → executeSendMessage FAILURE → errorFlag=true
        lenient().when(jsapUtil.createMailContext(any(), any()))
                .thenReturn(List.of(new MailContextDto("text/plain", "body")));
        SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("999999", "Error");
        String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
        lenient().when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

        // error process mocks
        lenient().when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

        // updateNotificationVinList(LINKED) success
        lenient().when(notificationVinListRepository.update(eq(1), eq("1"), eq(2))).thenReturn(1);

        // ntfBatchExecHistoryRepository.updateStatus
        lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);

        // notificationRepository.update
        lenient().when(notificationRepository.update("ME", 1)).thenReturn(1);

        // Act
        ResponseDto result = service.sendMessageNotification(req, header);

        // Assert
        assertNotNull(result);
        // Verify updateStatus was called with STATUS_ERR="2" (errorFlag=true path)
        verify(ntfBatchExecHistoryRepository, timeout(5000).atLeastOnce())
                .updateStatus(eq(1), eq(1), eq("2"));
    }

    /**
     * getPersonalInfoList(reflection) - done.get()で例外が発生した場合、futures.cancel(true)でキャンセルしてnullを返す
     * L577 for-each両分岐カバー
     */
    @Test
    void getPersonalInfoList_exception_001() throws Exception {
        // Arrange
        RequestHeaderDto header = buildHeader();
        SaNotificationSendListDto sendDto = new SaNotificationSendListDto("VIN001", "user001", "LC001", "1", "jsapUser001");
        List<SaNotificationSendListDto> notificationSendList = List.of(sendDto);

        lenient().when(properties.getParallelCurrent()).thenReturn(1);
        // executeGetUserInfo throws exception -> callable throws -> done.get() throws ExecutionException
        lenient().when(jsapUtil.executeGetUserInfo(any()))
                .thenThrow(new RuntimeException("API connection failed"));

        // Act
        Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                "getPersonalInfoList",
                RequestHeaderDto.class,
                List.class);
        method.setAccessible(true);
        PersonalInfoListResponseDto result = (PersonalInfoListResponseDto) method.invoke(service, header, notificationSendList);

        // Assert - should return null after catching exception from done.get()
        assertNull(result);
    }
}
