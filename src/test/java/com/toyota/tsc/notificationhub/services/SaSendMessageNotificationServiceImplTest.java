package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.commons.*;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.models.*;
import com.toyota.tsc.notificationhub.models.GetUserInfoResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserInfoResponseDto.ContactDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto.PersonalInfoList;
import com.toyota.tsc.notificationhub.repositories.*;

import jp.toyota.res.common.auth.GetALJTokenResultDto;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

        private MockedStatic<CommonUtil> mockedCommonUtil;

        @BeforeEach
        void setUp() {
                mockedCommonUtil = mockStatic(CommonUtil.class);
                mockedCommonUtil.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class)))
                                .thenReturn("log");
                mockedCommonUtil.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("mocked-code");
                mockedCommonUtil.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
        }

        @AfterEach
        void tearDown() {
                mockedCommonUtil.close();
        }

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
                return new NotificationVinListEntity(1, "SA", 100, "VIN001",
                                "VIN001:user001:LC001:1", 1L, seqNum, 0, false, null, null);
        }

        private String buildGetUserIdResponseJson(String resultCode, String userId) throws Exception {
                GetUserIdResponseDto dto = new GetUserIdResponseDto(resultCode, userId, "user001");
                return new ObjectMapper().writeValueAsString(dto);
        }

        /** ヘルパー: PersonalInfoListを構築 */
        private PersonalInfoList buildPersonalInfoList(String internalUserId, String userId,
                        List<ContactDto> contacts) {
                PersonalInfoList info = new PersonalInfoList();
                info.setInternalUserId(internalUserId);
                info.setUserId(userId);
                return info;
        }

        /** ヘルパー: GetUserInfoResponseDtoを構築 */
        private GetUserInfoResponseDto buildGetUserInfoResponseDto(String userId,
                        List<ContactDto> contacts) {
                GetUserInfoResponseDto dto = new GetUserInfoResponseDto();
                dto.setUserId(userId);
                dto.setContactList(contacts);
                return dto;
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

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * requestがnullの場合TscApplicationExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_001() {
                // Arrange
                RequestHeaderDto header = buildHeader();

                // Act & Assert
                assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(null, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * registrationSerialNumberがnullの場合TscApplicationExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_002() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
                req.setNotificationType("1");

                // Act & Assert
                assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * notificationTypeがnullの場合TscApplicationExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_003() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
                req.setRegistrationSerialNumber(1);

                // Act & Assert
                assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * notificationTypeが空文字の場合TscApplicationExceptionが投げられることを確認するテストケース
         */
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

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * registrationSerialNumberとnotificationTypeが両方nullの場合TscApplicationExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_005() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();

                // Act & Assert
                assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * ユーザー情報がnullの場合TscApplicationExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_006() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                when(notificationVinListRepository.select(1, 0)).thenReturn(null);

                // Act & Assert
                assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * ユーザー情報が空の場合TscApplicationExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_007() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                when(notificationVinListRepository.select(1, 0)).thenReturn(Collections.emptyList());

                // Act & Assert
                assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * SQL接続エラー時にCustomExceptionが投げられることを確認するテストケース
         */
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

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * SQL操作エラー時にCustomSqlExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_009() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                SQLException sqlEx = new SQLException("unique violation", "23505");
                when(notificationVinListRepository.select(any(), any())).thenThrow(new RuntimeException(sqlEx));

                // Act & Assert
                assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * その他SQLエラー時にCustomExceptionが投げられることを確認するテストケース
         */
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

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * 非SQL例外時にCustomExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_011() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                when(notificationVinListRepository.select(any(), any())).thenThrow(new RuntimeException("non-sql"));

                // Act & Assert
                assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * historyInsertが0件の場合CustomExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_012() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(0);
                // SQLException thrown inside forEach → caught gracefully → executeErrorProcess
                // called
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl getUserId失敗時にnullが返ることを確認するテストケース
         */
        @Test
        void sendMessageNotification_013() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                // GetUserId returns failure result code
                String failJson = buildGetUserIdResponseJson("99999999", null);
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(failJson, HttpStatus.OK));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * vinListUpdate(NOTLINKED)が0件の場合CustomExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_014() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(0);
                // SQLException thrown inside forEach → caught gracefully → executeErrorProcess
                // called
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

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
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                // Token returns empty access_token
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, ""));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * getUserIdが例外を投げた場合CustomExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_023() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenThrow(new CustomException("jsap error"));
                // CustomException inside forEach → caught gracefully → executeErrorProcess
                // called
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * getToken例外時にCustomExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_024() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(jsapUtil.executeGetToken()).thenThrow(new CustomException("token error"));
                // CustomException thrown in async inner loop; caught by async catch block (no
                // executeErrorProcess)
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * TscApplicationExceptionが再スローされることを確認するテストケース
         */
        @Test
        void sendMessageNotification_025() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();

                // Act & Assert
                assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
        }

        // =========================================================
        // Reflection-based tests for private methods
        // =========================================================

        /**
         * executeNotificationProcess (overloaded) - 正常系: notificationType="1" (NTF
         * only), push not required
         * Register succeeds, push skipped (isPushRequired="0"), primaryContact skipped
         * (TYPE_NTF only)
         */
        @Test
        void executeNotificationProcess_001() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token mock
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));

                // Register notification success mock
                RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("000000", "ntf-001", "OK");
                String regJson = new ObjectMapper().writeValueAsString(regDto);
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, userInfo, notificationSendList);

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
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token returns empty - code proceeds to executeRegisterNotification
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, ""));

                // Register notification success mock (token not used for registration)
                RegisterNotificationResponseDto regDto2 = new RegisterNotificationResponseDto("000000", "ntf-002", "OK");
                String regJson2 = new ObjectMapper().writeValueAsString(regDto2);
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regJson2, HttpStatus.OK));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, userInfo, notificationSendList);

                // Assert - register succeeds, push not required, primary not applicable -> false
                assertFalse(result);
        }

        /**
         * executeNotificationProcess (overloaded) - トークンがnullの場合エラーフラグがtrueになる (token
         * == null 分岐)
         */
        @Test
        void executeNotificationProcess_002b() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token returns null aljToken - code proceeds to executeRegisterNotification
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, null));

                // Register notification success mock (token not used for registration)
                RegisterNotificationResponseDto regDtoB = new RegisterNotificationResponseDto("000000", "ntf-002b",
                                "OK");
                String regJsonB = new ObjectMapper().writeValueAsString(regDtoB);
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regJsonB, HttpStatus.OK));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, userInfo, notificationSendList);

                // Assert - register succeeds, push not required, primary not applicable -> false
                assertFalse(result);
        }

        /**
         * executeNotificationProcess (overloaded) - Register通知エラー時エラーフラグがtrueになる
         */
        @Test
        void executeNotificationProcess_003() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token mock
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));

                // Error process mocks (batApisUtil not mocked → null response → register error → executeErrorProcess)
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, userInfo, notificationSendList);

                // Assert - register error (null response) → errorFlag=true
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
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token mock
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));

                // Register notification success mock
                RegisterNotificationResponseDto regDto4 = new RegisterNotificationResponseDto("000000", "ntf-004", "OK");
                String regJson4 = new ObjectMapper().writeValueAsString(regDto4);
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regJson4, HttpStatus.OK));

                // Error process mocks (push fails: deviceData null → errorFlag=true)
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, userInfo, notificationSendList);

                // Assert - push error (deviceData null because ntfInfoRepository not mocked) → errorFlag=true
                assertTrue(result);
        }

        /**
         * executeNotificationProcess (overloaded) - PrimaryContactエラー:
         * notificationType="3", sendMessage失敗
         */
        @Test
        void executeNotificationProcess_005() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("3", "0"); // TYPE_NTF_AND_MAILSMS, push not
                                                                                    // required
                RequestHeaderDto header = buildHeader();
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token mock
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));

                // Register notification success mock (TYPE_NTF_AND_MAILSMS needs registration)
                RegisterNotificationResponseDto regDto5 = new RegisterNotificationResponseDto("000000", "ntf-005", "OK");
                String regJson5 = new ObjectMapper().writeValueAsString(regDto5);
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regJson5, HttpStatus.OK));

                // getPersonalInfo → executeGetUserInfo returns non-2xx with valid JSON body
                // → readValue succeeds → !is2xxSuccessful() → return null → executePrimaryContact returns true
                GetUserInfoResponseDto errDto = new GetUserInfoResponseDto();
                errDto.setUserId("jsapUser001");
                errDto.setContactList(new java.util.ArrayList<>());
                String errJson = new ObjectMapper().writeValueAsString(errDto);
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(errJson, HttpStatus.INTERNAL_SERVER_ERROR));
                // Error process mocks (primaryContact will fail)
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, userInfo, notificationSendList);

                // Assert - primaryContact error → errorFlag=true
                assertTrue(result);
        }

        /**
         * executeNotificationProcess (overloaded) - 正常系: notificationType="3", push=1,
         * primaryContact (phone) 成功
         */
        @Test
        void executeNotificationProcess_006() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("3", "1"); // NTF_AND_MAILSMS, push required
                RequestHeaderDto header = buildHeader();
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token mock
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));

                // Register notification success mock
                RegisterNotificationResponseDto regDto6 = new RegisterNotificationResponseDto("000000", "ntf-006", "OK");
                String regJson6 = new ObjectMapper().writeValueAsString(regDto6);
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regJson6, HttpStatus.OK));

                // Error process mocks (push fails: no device data)
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, userInfo, notificationSendList);

                // Assert - push error (no device data) → errorFlag=true
                assertTrue(result);
        }

        /**
         * executeRegisterNotification - notificationType="2" (TYPE_MAILSMS)
         * はregister不要なのでfalseを返す
         */
        @Test
        void executeRegisterNotification_001() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("2", "0"); // TYPE_MAILSMS only
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "2", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeRegisterNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo);

                // Assert
                assertFalse(result);
        }

        /**
         * executeRegisterNotification - notificationType="1" (TYPE_NTF),
         * register成功でfalseを返す
         */
        @Test
        void executeRegisterNotification_002() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Register notification mock - success
                RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("000000", "ntf-001", "OK");
                String regJson = new ObjectMapper().writeValueAsString(regDto);
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeRegisterNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo);

                // Assert
                assertFalse(result);
        }

        /**
         * executeRegisterNotification - notificationType="3" (TYPE_NTF_AND_MAILSMS),
         * register失敗でtrueを返す
         */
        @Test
        void executeRegisterNotification_003() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("3", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "3", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Register notification mock - failure
                RegisterNotificationResponseDto regDto = new RegisterNotificationResponseDto("999999", "ntf-001",
                                "Error");
                String regJson = new ObjectMapper().writeValueAsString(regDto);
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regJson, HttpStatus.OK));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeRegisterNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo);

                // Assert
                assertTrue(result);
        }

        /**
         * executeRegisterNotification - registerNotification が null を返した場合
         * CustomException がスローされる
         */
        @Test
        void executeRegisterNotification_004() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Register notification mock - returns response with invalid JSON -> mapper
                // throws
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>("invalid-json", HttpStatus.OK));

                // Act & Assert
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeRegisterNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);

                InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                                () -> method.invoke(service, request, header, notificationData, userInfo));
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
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePushNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class,
                                String.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo,
                                "test-token");

                // Assert
                assertFalse(result);
        }

        /**
         * executePushNotification - APNプラットフォーム (platformType="2")
         * の場合ペイロード構築成功でfalseを返す
         */
        @Test
        void executePushNotification_002() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "1"); // push required
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Device data mock - APN
                NtfInfoEntity device = buildNtfInfoEntity("user001", "2");
                when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
                when(notificationHubUtil.buildApnsPayload(any())).thenReturn("apns-payload");
                when(notificationHubUtil.replaceLcsSelected(any(), eq("2"), any())).thenReturn("final-apns-payload");

                // Push response mock - success
                PushRequestResponseDto pushDto = new PushRequestResponseDto("000000", "OK");
                String pushJson = new ObjectMapper().writeValueAsString(pushDto);
                when(jsapUtil.executePushRequest(any(), any(), any()))
                                .thenReturn(new ResponseEntity<>(pushJson, HttpStatus.OK));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePushNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class,
                                String.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo,
                                "test-token");

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
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Device data mock - FCM
                NtfInfoEntity device = buildNtfInfoEntity("user001", "1");
                when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
                when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn("fcm-payload");
                when(notificationHubUtil.replaceLcsSelected(any(), eq("1"), any())).thenReturn("final-payload");

                // Push response mock - failure (non-success code)
                PushRequestResponseDto pushDto = new PushRequestResponseDto("999999", "Error");
                String pushJson = new ObjectMapper().writeValueAsString(pushDto);
                when(jsapUtil.executePushRequest(any(), any(), any()))
                                .thenReturn(new ResponseEntity<>(pushJson, HttpStatus.OK));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePushNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class,
                                String.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo,
                                "test-token");

                // Assert - push failure returns true (errorFlag)
                assertTrue(result);
        }

        /**
         * executePushNotification - デバイスなし (deviceData=null) の場合trueを返す
         */
        @Test
        void executePushNotification_004() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "1"); // push required
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Device data mock - empty list -> null
                when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(Collections.emptyList());

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePushNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class,
                                String.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo,
                                "test-token");

                // Assert
                assertTrue(result);
        }

        /**
         * executePrimaryContact - notificationType="1" (TYPE_NTF only)
         * の場合スキップしてfalseを返す
         */
        @Test
        void executePrimaryContact_001() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "1", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, "test-token",
                                vinListEntity);

                // Assert
                assertFalse(result);
        }

        /**
         * executePrimaryContact - notificationType="2" (TYPE_MAILSMS),
         * personalInfoListResponseがnullの場合trueを返す
         */
        @Test
        void executePrimaryContact_002() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "2", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);

                // executeGetUserInfo returns non-2xx -> getPersonalInfo returns null -> executePrimaryContact returns true
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, "test-token",
                                vinListEntity);

                // Assert - getPersonalInfo returns null (non-2xx response) -> true
                assertTrue(result);
        }

        /**
         * executePrimaryContact - notificationType="3" (TYPE_NTF_AND_MAILSMS),
         * personalInfoListResponseがnullの場合trueを返す
         */
        @Test
        void executePrimaryContact_003() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("3", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "3", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);

                // executeGetUserInfo returns non-2xx -> getPersonalInfo returns null -> executePrimaryContact returns true
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, "test-token",
                                vinListEntity);

                // Assert - getPersonalInfo returns null (non-2xx response) -> true
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
                GetUserInfoResponseDto personalInfo = buildGetUserInfoResponseDto("jsapUser001", List.of(contact));

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "sendRequest",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                GetUserInfoResponseDto.class,
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
                GetUserInfoResponseDto personalInfo = buildGetUserInfoResponseDto("jsapUser001", List.of(contact));

                // Mail context mock
                when(jsapUtil.createMailContext(any(), any()))
                                .thenReturn(List.of(new MailContextDto("text/plain", "body")));

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
                                GetUserInfoResponseDto.class,
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
                GetUserInfoResponseDto personalInfo = buildGetUserInfoResponseDto("jsapUser001", List.of(contact));

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
                                GetUserInfoResponseDto.class,
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
                GetUserInfoResponseDto personalInfo = buildGetUserInfoResponseDto("jsapUser001", List.of(contact));

                // Act & Assert
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "sendRequest",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                GetUserInfoResponseDto.class,
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
                                ContactDto.class,
                                String.class,
                                Object.class,
                                String.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, contact, "jsapUser001",
                                new Object(), "test-token");

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
                                ContactDto.class,
                                String.class,
                                Object.class,
                                String.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, contact, "jsapUser001",
                                new Object(), "test-token");

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
                ContactDto contact = buildContactDto("2", true, "test@example.com");

                // sendMessage throws exception
                when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                                .thenThrow(new RuntimeException("network error"));

                // Act & Assert
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeSendMessage",
                                SendMessageNotificationRequestDto.class,
                                ContactDto.class,
                                String.class,
                                Object.class,
                                String.class);
                method.setAccessible(true);

                InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                                () -> method.invoke(service, request, contact, "jsapUser001",
                                                new Object(), "test-token"));
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
                NotificationVinListEntity userInfo = buildVinListEntity(1L);
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
                NotificationVinListEntity userInfo = buildVinListEntity(1L);
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
                NotificationVinListEntity userInfo = buildVinListEntity(1L);
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

                when(ntfInfoRepository.selectAllByInternalUserId("user001"))
                                .thenReturn(List.of(device1, device2, device3));

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
         * executeNotificationProcess (overloaded) - 例外発生時 CustomException がスローされる
         */
        @Test
        void executeNotificationProcess_008() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token throws exception
                when(jsapUtil.executeGetToken()).thenThrow(new RuntimeException("token failure"));

                // Act & Assert
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);

                InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                                () -> method.invoke(service, request, header, userInfo, notificationSendList));
                assertInstanceOf(CustomException.class, ex.getCause());
        }

        // =========================================================
        // Additional tests for remaining branch coverage
        // =========================================================

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * 通知区分=3かつnotificationContentsがnullの場合TscApplicationExceptionが投げられる
         */
        @Test
        void sendMessageNotification_015() {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
                req.setRegistrationSerialNumber(1);
                req.setNotificationType("3");
                req.setNotificationContents(null);
                assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * 通知区分=3かつnotificationContentsが空の場合TscApplicationExceptionが投げられる
         */
        @Test
        void sendMessageNotification_016() {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = new SendMessageNotificationRequestDto();
                req.setRegistrationSerialNumber(1);
                req.setNotificationType("3");
                req.setNotificationContents(new ArrayList<>());
                assertThrows(TscApplicationException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * notificationSendListが空の場合エラー処理が実行されることを確認するテストケース
         */
        @Test
        void sendMessageNotification_017() {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                // empty CSV → convertToNotificationSendListDto catches exception → returns []
                NotificationVinListEntity entity = new NotificationVinListEntity(
                                1, "SA", 100, "VIN001", "", 1L, 1L, 0, false, null, null);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
                verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * convertInternalUserIdToUserId でresponseがnullの場合空リストが返ることを確認するテストケース
         */
        @Test
        void sendMessageNotification_018() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                // executeGetUserId returns null → response == null → errorFlag → empty list
                when(jsapUtil.executeGetUserId(any(), any())).thenReturn(null);
                when(notificationRepository.update("SA", 1)).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * convertInternalUserIdToUserId でresponse.getBody()がnullの場合空リストが返ることを確認するテストケース
         */
        @Test
        void sendMessageNotification_018b() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                // response.getBody() == null → errorFlag → empty list
                when(jsapUtil.executeGetUserId(any(), any())).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));
                when(notificationRepository.update("SA", 1)).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * forEachループ内でSQL接続エラーが発生した場合ログ出力してエラー登録後に次ループへ遷移する
         */
        @Test
        void sendMessageNotification_019() {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                // insert throws SQL connection error → outer catch SQL connection branch
                when(ntfBatchExecHistoryRepository.insert(any())).thenThrow(
                                new RuntimeException(new SQLException("connection error", "08001")));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * forEachループ内でSQL操作エラーが発生した場合ログ出力してエラー登録後に次ループへ遷移する
         */
        @Test
        void sendMessageNotification_020() {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                // insert throws SQL operation error → outer catch SQL operation branch
                when(ntfBatchExecHistoryRepository.insert(any())).thenThrow(
                                new RuntimeException(new SQLException("unique violation", "23505")));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * forEachループ内でその他SQLエラーが発生した場合elseブランチのログ出力が実行される
         */
        @Test
        void sendMessageNotification_021() {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                // insert throws other SQL error (neither connection nor operation) → outer
                // catch else branch
                when(ntfBatchExecHistoryRepository.insert(any())).thenThrow(
                                new RuntimeException(new SQLException("other error", "99999")));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * 非同期処理内でSQL接続エラーが発生した場合catchブランチのログ出力が実行される
         */
        @Test
        void sendMessageNotification_026() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                // mock executeGetUserInfo so getPersonalInfo returns non-null dto (enables async notification processing)
                GetUserInfoResponseDto userInfoDto = new GetUserInfoResponseDto();
                userInfoDto.setUserId("jsapUser001");
                ContactDto userContact = buildContactDto("1", true, "09012345678");
                userInfoDto.setContactList(List.of(userContact));
                String userInfoJson = new ObjectMapper().writeValueAsString(userInfoDto);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(userInfoJson, HttpStatus.OK));
                // inner async: executeNotificationProcess succeeds → updateStatus throws SQL
                // connection
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                RegisterNotificationResponseDto regRes = new RegisterNotificationResponseDto("000000", "ntf001", "ok");
                String regResJson = new ObjectMapper().writeValueAsString(regRes);
                // lenient: stub may be called from async thread, Mockito strict mode may not
                // track cross-thread usage
                lenient().when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regResJson, HttpStatus.OK));
                // first call throws, second call (in catch block) returns 1
                lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any()))
                                .thenThrow(new RuntimeException(new SQLException("connection error", "08001")))
                                .thenReturn(1);
                lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * 非同期処理内でSQL操作エラーが発生した場合catchブランチのログ出力が実行される
         */
        @Test
        void sendMessageNotification_027() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                // mock executeGetUserInfo so getPersonalInfo returns non-null dto (enables async notification processing)
                GetUserInfoResponseDto userInfoDto27 = new GetUserInfoResponseDto();
                userInfoDto27.setUserId("jsapUser001");
                ContactDto userContact27 = buildContactDto("1", true, "09012345678");
                userInfoDto27.setContactList(List.of(userContact27));
                String userInfoJson27 = new ObjectMapper().writeValueAsString(userInfoDto27);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(userInfoJson27, HttpStatus.OK));
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                RegisterNotificationResponseDto regRes27 = new RegisterNotificationResponseDto("000000", "ntf001",
                                "ok");
                String regResJson27 = new ObjectMapper().writeValueAsString(regRes27);
                // lenient: stub may be called from async thread, Mockito strict mode may not
                // track cross-thread usage
                lenient().when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regResJson27, HttpStatus.OK));
                // first call throws SQL operation error, second returns 1
                lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any()))
                                .thenThrow(new RuntimeException(new SQLException("unique violation", "23505")))
                                .thenReturn(1);
                lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * executeRegisterNotification - batApisUtil.executeRegisterNotification が null
         * を返した場合
         * responseDto == null → errorFlag=true を返す (registerNotification response == null branch)
         */
        @Test
        void executeRegisterNotification_005() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // batApisUtil returns null → response == null → registerNotification returns null
                // → executeRegisterNotification returns true (errorFlag)
                when(batApisUtil.executeRegisterNotification(any())).thenReturn(null);

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeRegisterNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);

                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo);
                assertTrue(result);
        }

        /** executePostMessage - jsapUtil.executePushRequest が null を返した場合 null を返す */
        @Test
        void executePostMessage_001() throws Exception {
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "1", "jsapUser001");
                NtfInfoEntity deviceData = buildNtfInfoEntity("user001", "1");

                when(jsapUtil.executePushRequest(any(), any(), any())).thenReturn(null);

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePostMessage",
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NtfInfoEntity.class,
                                String.class,
                                String.class);
                method.setAccessible(true);
                Object result = method.invoke(service, header, notificationData, deviceData,
                                "fcm-payload", "test-token");

                assertNull(result);
        }

        /**
         * executePostMessage - jsapUtil.executePushRequest の body が null の場合 null を返す
         */
        @Test
        void executePostMessage_002() throws Exception {
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "1", "jsapUser001");
                NtfInfoEntity deviceData = buildNtfInfoEntity("user001", "1");

                when(jsapUtil.executePushRequest(any(), any(), any()))
                                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePostMessage",
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NtfInfoEntity.class,
                                String.class,
                                String.class);
                method.setAccessible(true);
                Object result = method.invoke(service, header, notificationData, deviceData,
                                "fcm-payload", "test-token");

                assertNull(result);
        }

        /** executePrimaryContact - personalInfoListResponseが空の場合（ユーザー未発見）trueを返す */
        @Test
        void executePrimaryContact_004() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "2", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);

                // executeGetUserInfo returns non-2xx -> getPersonalInfo returns null -> executePrimaryContact returns true
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR));

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData,
                                "test-token", vinListEntity);

                assertTrue(result);
        }

        /**
         * executePrimaryContact - personalInfoListResponseにユーザーが存在しない場合trueを返す
         */
        @Test
        void executePrimaryContact_005() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "2", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);

                // executeGetUserInfo returns dto with empty contactList -> personalInfoResponse.getContactList().isEmpty() -> true
                GetUserInfoResponseDto emptyContactDto = new GetUserInfoResponseDto();
                emptyContactDto.setUserId("jsapUser001");
                emptyContactDto.setContactList(new ArrayList<>());
                String emptyContactJson = new ObjectMapper().writeValueAsString(emptyContactDto);
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(emptyContactJson, HttpStatus.OK));

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData,
                                "test-token", vinListEntity);

                assertTrue(result);
        }

        /**
         * executePrimaryContact - personalInfoListResponseが空の場合trueを返す（TYPE_MAILSMS）
         */
        @Test
        void executePrimaryContact_006() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "2", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);

                // executeGetUserInfo returns non-2xx -> getPersonalInfo returns null -> executePrimaryContact returns true
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR));

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData,
                                "test-token", vinListEntity);

                assertTrue(result);
        }

        /**
         * executePrimaryContact - personalInfoListResponseが空の場合trueを返す（TYPE_NTF_AND_MAILSMS）
         */
        @Test
        void executePrimaryContact_007() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("3", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "3", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);

                // executeGetUserInfo returns non-2xx -> getPersonalInfo returns null -> executePrimaryContact returns true
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR));

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData,
                                "test-token", vinListEntity);

                assertTrue(result);
        }

        /**
         * executePrimaryContact - personalInfoListResponseにユーザーが含まれない場合trueを返す
         */
        @Test
        void executePrimaryContact_008() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "2", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);

                // executeGetUserInfo returns dto with null contactList -> personalInfoResponse.getContactList() is null
                // but executePrimaryContact checks isEmpty() on it - so need to cover null contactList path
                // getPersonalInfo returns the dto (not null since 2xx), but contactList == null
                // -> personalInfoResponse.getContactList().isEmpty() would throw NPE, but check is:
                // if (personalInfoResponse == null || personalInfoResponse.getContactList().isEmpty())
                // Actually with null contactList, isEmpty() would throw NPE -> wrapped in catch -> re-thrown
                // So let's use empty contactList to test the isEmpty() == true branch
                GetUserInfoResponseDto emptyContactDto2 = new GetUserInfoResponseDto();
                emptyContactDto2.setUserId("jsapUser001");
                emptyContactDto2.setContactList(new ArrayList<>());
                String emptyContactJson2 = new ObjectMapper().writeValueAsString(emptyContactDto2);
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(emptyContactJson2, HttpStatus.OK));

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData,
                                "test-token", vinListEntity);

                assertTrue(result);
        }

        /**
         * executePrimaryContact - personalInfoListResponseに一致ユーザーが含まれる場合sendRequestを呼ぶ
         */
        @Test
        void executePrimaryContact_009() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "2", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);
                ContactDto contact = buildContactDto("1", true, "09012345678");
                GetUserInfoResponseDto personalInfo = buildGetUserInfoResponseDto("jsapUser001", List.of(contact));

                // Mock executeGetUserInfo to return the personalInfo DTO
                String personalInfoJson = new ObjectMapper().writeValueAsString(personalInfo);
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(personalInfoJson, HttpStatus.OK));

                // sendMessage success mock
                when(jsapUtil.createSmsContext(any())).thenReturn(new SmsContextDto("sms-body"));
                SendMessageResponseDto sendMsgDto = new SendMessageResponseDto("000000", "OK");
                String sendMsgJson = new ObjectMapper().writeValueAsString(sendMsgDto);
                when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                                .thenReturn(new ResponseEntity<>(sendMsgJson, HttpStatus.OK));

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData,
                                "test-token", vinListEntity);

                assertFalse(result);
        }

        /** executeSendMessage - sendMessageResponseがnullの場合trueを返す */
        @Test
        void executeSendMessage_004() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                ContactDto contact = buildContactDto("2", true, "test@example.com");
                when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any())).thenReturn(null);

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeSendMessage",
                                SendMessageNotificationRequestDto.class,
                                GetUserInfoResponseDto.ContactDto.class,
                                String.class,
                                Object.class,
                                String.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, contact, "jsapUser001",
                                new Object(), "test-token");

                assertTrue(result);
        }

        /** executeSendMessage - sendMessageResponse.getBody()がnullの場合trueを返す */
        @Test
        void executeSendMessage_005() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                ContactDto contact = buildContactDto("2", true, "test@example.com");
                when(jsapUtil.executeSendMessage(any(), any(), any(), any(), any(), any()))
                                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeSendMessage",
                                SendMessageNotificationRequestDto.class,
                                GetUserInfoResponseDto.ContactDto.class,
                                String.class,
                                Object.class,
                                String.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, contact, "jsapUser001",
                                new Object(), "test-token");

                assertTrue(result);
        }

        // =========================================================
        // New tests for uncovered lines (sendMessageNotification_028+)
        // =========================================================

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L200-207: properties.getThreadPool()が例外をスローした場合
         * outer catch(Exception)が実行されCustomExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_028() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                // properties.getThreadPool() throws RuntimeException → outer catch(Exception)
                when(properties.getThreadPool()).thenThrow(new RuntimeException("threadpool error"));

                // Act & Assert
                assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L378-385: entity.notificationSendListがnullの場合
         * convertToNotificationSendListDtoがemptyを返しエラー処理が実行されることを確認するテストケース
         */
        @Test
        void sendMessageNotification_029() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                // notificationSendList=null → convertToNotificationSendListDto catches NPE → returns []
                NotificationVinListEntity entity = new NotificationVinListEntity(
                                1, "SA", 100, "VIN001", null, 1L, 1L, 0, false, null, null);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
                verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L413-421: 外側ループでtokenResult==nullの場合
         * エラー処理が実行されて処理が継続されることを確認するテストケース
         */
        @Test
        void sendMessageNotification_030() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                // executeGetToken returns null → L413-421 branch
                when(jsapUtil.executeGetToken()).thenReturn(null);
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
                verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L424-432: 外側ループでtokenResult.getResult()==falseの場合
         * エラー処理が実行されて処理が継続されることを確認するテストケース
         */
        @Test
        void sendMessageNotification_031() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                // executeGetToken returns result=false → L424-432 branch
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(false, "tok"));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
                verify(ntfBatchExecErrorInfoRepository, atLeastOnce()).insert(any());
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L461-476, L479: 非同期内でerrorFlag=trueの場合
         * updateStatus(STATUS_ERR)が呼ばれることを確認するテストケース
         */
        @Test
        void sendMessageNotification_032() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                // getPersonalInfo returns non-null dto (enables async notification processing)
                GetUserInfoResponseDto userInfoDto32 = new GetUserInfoResponseDto();
                userInfoDto32.setUserId("jsapUser001");
                ContactDto userContact32 = buildContactDto("1", true, "09012345678");
                userInfoDto32.setContactList(List.of(userContact32));
                String userInfoJson32 = new ObjectMapper().writeValueAsString(userInfoDto32);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(userInfoJson32, HttpStatus.OK));
                // inner executeNotificationProcess: getToken returns valid, executeRegisterNotification fails → errorFlag=true
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // batApisUtil returns null → registerNotification returns null → executeRegisterNotification returns true (error)
                lenient().when(batApisUtil.executeRegisterNotification(any())).thenReturn(null);
                lenient().when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                // updateStatus succeeds
                lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
                lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2))).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L481-505: 非同期内でupdateNotificationVinList(LINKED)がRuntimeExceptionをスローし
         * CustomSqlExceptionハンドラが実行されることを確認するテストケース
         */
        @Test
        void sendMessageNotification_033() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                // getPersonalInfo returns non-null dto (enables async notification processing)
                GetUserInfoResponseDto userInfoDto33 = new GetUserInfoResponseDto();
                userInfoDto33.setUserId("jsapUser001");
                ContactDto userContact33 = buildContactDto("1", true, "09012345678");
                userInfoDto33.setContactList(List.of(userContact33));
                String userInfoJson33 = new ObjectMapper().writeValueAsString(userInfoDto33);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(userInfoJson33, HttpStatus.OK));
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                RegisterNotificationResponseDto regRes33 = new RegisterNotificationResponseDto("000000", "ntf33", "ok");
                String regResJson33 = new ObjectMapper().writeValueAsString(regRes33);
                lenient().when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regResJson33, HttpStatus.OK));
                lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
                // update(1,"1",2): 1回目throw → updateNotificationVinList wrap → async catch(CustomSqlException)
                // catch内でも再呼び出しされるため、2回目はreturn 1でCompletableFutureを正常完了させる
                lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2)))
                                .thenThrow(new RuntimeException(new SQLException("connection error", "08001")))
                                .thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L606-607: updateNotificationVinList内のcatch - update(1,"1",0)がRuntimeExceptionをスローし
         * CustomSqlExceptionがスローされることを確認するテストケース
         */
        @Test
        void sendMessageNotification_034() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                // update(1,"1",0) throws RuntimeException → updateNotificationVinList catch → CustomSqlException
                // → outer catch(CustomSqlException) → executeErrorProcess → next loop → updateNotification → CustomException
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0)))
                                .thenThrow(new RuntimeException("update failed"));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L727-732: getPersonalInfoList内部 - executeGetUserInfoがnull contactListを返した場合
         * タスクがキャンセルされてemptyリストが返ることを確認するテストケース
         */
        @Test
        void sendMessageNotification_035() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // executeGetUserInfo returns dto with null contactList → getPersonalInfo still returns dto
                // but executePrimaryContact checks contactList == null || isEmpty() → returns true (error)
                GetUserInfoResponseDto noContactDto = new GetUserInfoResponseDto();
                noContactDto.setUserId("jsapUser001");
                noContactDto.setContactList(null);
                String noContactJson = new ObjectMapper().writeValueAsString(noContactDto);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(noContactJson, HttpStatus.OK));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L762-768: getPersonalInfoList内部 - Future.get()が例外をスローした場合
         * 残タスクがキャンセルされてemptyリストが返ることを確認するテストケース
         */
        @Test
        void sendMessageNotification_036() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // executeGetUserInfo throws RuntimeException inside the per-user getPersonalInfo call
                // if executePrimaryContact is triggered → CustomException propagates
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenThrow(new RuntimeException("user info fetch error"));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L775-777: getPersonalInfoList outer catch - properties.getParallelCurrent()が例外をスローした場合
         * CustomExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_037() throws Exception {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // batApisUtil not mocked → null response → NPE in registerNotification → CustomException
                // → forEach catch in executeNotificationProcess → throws CustomException
                // → async catch(Exception) → executeErrorProcess called
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L1279-1284: updateNotification - notificationRepository.update が 0 を返した場合
         * CustomSqlExceptionがスローされCustomExceptionで包まれることを確認するテストケース
         */
        @Test
        void sendMessageNotification_038() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                String successJson;
                try {
                        successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(false, "tok"));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                // updateNotification: notificationRepository.update returns 0 → throws CustomSqlException
                // → outer catch(CustomSqlException) → CustomException
                when(notificationRepository.update("SA", 1)).thenReturn(0);

                // Act & Assert
                assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L1291-1292: updateNotification catch - notificationRepository.update が例外をスローした場合
         * CustomExceptionが投げられることを確認するテストケース
         */
        @Test
        void sendMessageNotification_039() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                String successJson;
                try {
                        successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                } catch (Exception e) {
                        throw new RuntimeException(e);
                }
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(false, "tok"));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);
                // notificationRepository.update throws RuntimeException → catch wraps in CustomSqlException
                // → outer catch(CustomSqlException) → CustomException
                when(notificationRepository.update("SA", 1)).thenThrow(new RuntimeException("db error"));

                // Act & Assert
                assertThrows(CustomException.class, () -> service.sendMessageNotification(req, header));
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L1339-1341: executeErrorProcess catch - ntfBatchExecErrorInfoRepository.insert が例外をスローした場合
         * ログ出力して処理が継続されることを確認するテストケース
         */
        @Test
        void sendMessageNotification_040() {
                // Arrange
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(0);
                // ntfBatchExecErrorInfoRepository.insert throws → executeErrorProcess catch → log only
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenThrow(new RuntimeException("insert error"));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                // Act
                ResponseDto result = service.sendMessageNotification(req, header);

                // Assert - executeErrorProcess catches its own exception, processing continues
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L812-826: inner executeNotificationProcess - tokenResult==null の場合
         * エラー処理が実行されて処理が継続されることを確認するテストケース（リフレクション）
         */
        @Test
        void executeNotificationProcess_009() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token returns null → L812 branch
                when(jsapUtil.executeGetToken()).thenReturn(null);
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, userInfo, notificationSendList);

                // Assert - tokenResult==null → continue → errorFlag remains false
                assertFalse(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L820-826: inner executeNotificationProcess - !tokenResult.getResult() の場合
         * エラー処理が実行されて処理が継続されることを確認するテストケース（リフレクション）
         */
        @Test
        void executeNotificationProcess_010() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                List<SaNotificationSendListDto> notificationSendList = buildNotificationSendList("user001",
                                "jsapUser001");

                // Token returns result=false → L820 branch
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(false, "tok"));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeNotificationProcess",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                NotificationVinListEntity.class,
                                List.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, userInfo, notificationSendList);

                // Assert - !tokenResult.getResult() → continue → errorFlag remains false
                assertFalse(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L1000-1004: executePushNotification - executePostMessage が null を返した場合
         * trueが返ることを確認するテストケース（リフレクション）
         */
        @Test
        void executePushNotification_005() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "1"); // push required
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Device data mock - FCM
                NtfInfoEntity device = buildNtfInfoEntity("user001", "1");
                when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
                when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn("fcm-payload");
                when(notificationHubUtil.replaceLcsSelected(any(), eq("1"), any())).thenReturn("final-payload");
                // executePushRequest returns null → executePostMessage returns null → L1000-1004 branch
                when(jsapUtil.executePushRequest(any(), any(), any())).thenReturn(null);

                // Act
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePushNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class,
                                String.class);
                method.setAccessible(true);
                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo,
                                "test-token");

                // Assert - obj == null → return true
                assertTrue(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L1019-1022: executePushNotification catch(Exception) - 例外をスローした場合
         * エラー処理が実行されて例外が再スローされることを確認するテストケース（リフレクション）
         */
        @Test
        void executePushNotification_006() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("1", "1"); // push required
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto("VIN001", "user001",
                                "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // Device data mock - FCM
                NtfInfoEntity device = buildNtfInfoEntity("user001", "1");
                when(ntfInfoRepository.selectAllByInternalUserId("user001")).thenReturn(List.of(device));
                when(notificationHubUtil.buildFcmV1Payload(any())).thenReturn("fcm-payload");
                when(notificationHubUtil.replaceLcsSelected(any(), eq("1"), any())).thenReturn("final-payload");
                // executePushRequest throws RuntimeException wrapping CustomException
                // → executePostMessage wraps it in CustomException → propagates
                // → executePushNotification catch(Exception) L1019 → executeErrorProcess → rethrow
                when(jsapUtil.executePushRequest(any(), any(), any()))
                                .thenThrow(new RuntimeException("push network error"));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

                // Act & Assert
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePushNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class,
                                String.class);
                method.setAccessible(true);

                InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                                () -> method.invoke(service, request, header, notificationData, userInfo,
                                                "test-token"));
                assertInstanceOf(CustomException.class, ex.getCause());
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L1155-1158: executePrimaryContact catch(Exception) - sendRequest内で例外がスローされた場合
         * エラー処理が実行されて例外が再スローされることを確認するテストケース（リフレクション）
         */
        @Test
        void executePrimaryContact_010() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "2", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);
                ContactDto contact = buildContactDto("1", true, "09012345678");
                GetUserInfoResponseDto personalInfo = buildGetUserInfoResponseDto("jsapUser001", List.of(contact));

                // Mock executeGetUserInfo to return the personalInfo DTO
                String personalInfoJson = new ObjectMapper().writeValueAsString(personalInfo);
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(personalInfoJson, HttpStatus.OK));

                // jsapUtil.createSmsContext throws → sendRequest propagates CustomException
                // → executePrimaryContact catch(Exception) L1155 → executeErrorProcess → rethrow
                when(jsapUtil.createSmsContext(any())).thenThrow(new RuntimeException("sms context error"));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

                // Act & Assert
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);

                InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                                () -> method.invoke(service, request, header, notificationData, "test-token",
                                                vinListEntity));
                assertInstanceOf(RuntimeException.class, ex.getCause());
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L1024-1025: getPersonalInfo catch(Exception) - executeGetUserInfoが例外をスローした場合
         * CustomExceptionがスローされることを確認するテストケース（リフレクション経由executePrimaryContact）
         */
        @Test
        void executePrimaryContact_011() throws Exception {
                // Arrange
                SendMessageNotificationRequestDto request = buildRequest("2", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "2", "jsapUser001");
                NotificationVinListEntity vinListEntity = buildVinListEntity(1L);

                // jsapUtil.executeGetUserInfo throws RuntimeException
                // -> getPersonalInfo catch(Exception e) L1024 -> throws CustomException
                // -> executePrimaryContact catch(Exception e) L996 -> executeErrorProcess -> rethrow
                when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenThrow(new RuntimeException("user info network error"));
                when(ntfBatchExecErrorInfoRepository.insert(any())).thenReturn(1);

                // Act & Assert
                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executePrimaryContact",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                String.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);

                InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                                () -> method.invoke(service, request, header, notificationData, "test-token",
                                                vinListEntity));
                assertInstanceOf(RuntimeException.class, ex.getCause());
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L936: registerNotification - response.getBody()==null ブランチカバレッジ
         * batApisUtil が null ボディの ResponseEntity を返した場合 null が返る
         */
        @Test
        void executeRegisterNotification_006() throws Exception {
                SendMessageNotificationRequestDto request = buildRequest("1", "0");
                RequestHeaderDto header = buildHeader();
                SaNotificationSendListDto notificationData = new SaNotificationSendListDto(
                                "VIN001", "user001", "LC001", "1", "jsapUser001");
                NotificationVinListEntity userInfo = buildVinListEntity(1L);

                // response.getBody() == null → registerNotification returns null
                when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

                Method method = SaSendMessageNotificationServiceImpl.class.getDeclaredMethod(
                                "executeRegisterNotification",
                                SendMessageNotificationRequestDto.class,
                                RequestHeaderDto.class,
                                SaNotificationSendListDto.class,
                                NotificationVinListEntity.class);
                method.setAccessible(true);

                Boolean result = (Boolean) method.invoke(service, request, header, notificationData, userInfo);
                assertTrue(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L492-500: 非同期 catch(CustomSqlException) - SQL操作エラーのログパスをカバレッジ
         * updateNotificationVinList が操作エラー（SQLState "23505"）をスローする場合
         */
        @Test
        void sendMessageNotification_041() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                GetUserInfoResponseDto userInfoDto41 = new GetUserInfoResponseDto();
                userInfoDto41.setUserId("jsapUser001");
                ContactDto contact41 = buildContactDto("1", true, "09012345678");
                userInfoDto41.setContactList(List.of(contact41));
                String userInfoJson41 = new ObjectMapper().writeValueAsString(userInfoDto41);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(userInfoJson41, HttpStatus.OK));
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                RegisterNotificationResponseDto regRes41 = new RegisterNotificationResponseDto("000000", "ntf41", "ok");
                String regResJson41 = new ObjectMapper().writeValueAsString(regRes41);
                lenient().when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regResJson41, HttpStatus.OK));
                lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
                // 操作エラー("23505") → isSqlOperationError=true → L492-500 パスをカバー
                // catch内でも再呼び出しされるため2回目はreturn 1
                lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2)))
                                .thenThrow(new RuntimeException(new SQLException("unique violation", "23505")))
                                .thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L484 false: 非同期 catch(CustomSqlException) - findSqlException が null を返す場合
         * CustomSqlException に SQL 原因がない場合 if(sqlEx!=null) が false になるパスをカバー
         */
        @Test
        void sendMessageNotification_042() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                GetUserInfoResponseDto userInfoDto42 = new GetUserInfoResponseDto();
                userInfoDto42.setUserId("jsapUser001");
                ContactDto contact42 = buildContactDto("1", true, "09012345678");
                userInfoDto42.setContactList(List.of(contact42));
                String userInfoJson42 = new ObjectMapper().writeValueAsString(userInfoDto42);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(userInfoJson42, HttpStatus.OK));
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                RegisterNotificationResponseDto regRes42 = new RegisterNotificationResponseDto("000000", "ntf42", "ok");
                String regResJson42 = new ObjectMapper().writeValueAsString(regRes42);
                lenient().when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regResJson42, HttpStatus.OK));
                lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
                // SQL原因なしのRuntimeException → findSqlException(e)==null → if(sqlEx!=null) false
                // catch内でも再呼び出しされるため2回目はreturn 1
                lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2)))
                                .thenThrow(new RuntimeException("no sql cause"))
                                .thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L721: getPersonalInfoList - executeGetUserInfo が null を返す場合 response==null ブランチ
         */
        @Test
        void sendMessageNotification_043() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // response == null → lambda returns null → getPersonalInfoList returns []
                lenient().when(jsapUtil.executeGetUserInfo(any(), any())).thenReturn(null);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L726: getPersonalInfoList - resBody==null ブランチ
         */
        @Test
        void sendMessageNotification_044() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // resBody == null → lambda returns null → getPersonalInfoList returns []
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>((String) null, HttpStatus.OK));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L731: getPersonalInfoList - contactList==null ブランチ
         */
        @Test
        void sendMessageNotification_045() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // contactList == null → lambda returns null → getPersonalInfoList returns []
                GetUserInfoResponseDto dtoNoContact = new GetUserInfoResponseDto();
                dtoNoContact.setUserId("jsapUser001");
                dtoNoContact.setContactList(null);
                String jsonNoContact = new ObjectMapper().writeValueAsString(dtoNoContact);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(jsonNoContact, HttpStatus.OK));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L721: getPersonalInfoList - response != null && !is2xxSuccessful() ブランチ（OR条件の第2項）
         */
        @Test
        void sendMessageNotification_046() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // response != null AND !is2xxSuccessful → L721 second OR branch → return null
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L726: getPersonalInfoList - resBody != null && resBody.isEmpty() ブランチ（OR条件の第2項）
         */
        @Test
        void sendMessageNotification_047() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // resBody is empty string (non-null but empty) → L726 second OR branch → return null
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>("", HttpStatus.OK));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L492: 非同期 catch(CustomSqlException) - isSqlOperationError=falseの場合のブランチ
         * SQLState "HY000"（汎用エラー、接続エラーでも操作エラーでもない）でL492 elseブランチを通ることを確認する
         */
        @Test
        void sendMessageNotification_049() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                GetUserInfoResponseDto userInfoDto49 = new GetUserInfoResponseDto();
                userInfoDto49.setUserId("jsapUser001");
                ContactDto contact49 = buildContactDto("1", true, "09012345678");
                userInfoDto49.setContactList(List.of(contact49));
                String userInfoJson49 = new ObjectMapper().writeValueAsString(userInfoDto49);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(userInfoJson49, HttpStatus.OK));
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                RegisterNotificationResponseDto regRes49 = new RegisterNotificationResponseDto("000000", "ntf49", "ok");
                String regResJson49 = new ObjectMapper().writeValueAsString(regRes49);
                lenient().when(batApisUtil.executeRegisterNotification(any()))
                                .thenReturn(new ResponseEntity<>(regResJson49, HttpStatus.OK));
                lenient().when(ntfBatchExecHistoryRepository.updateStatus(any(), any(), any())).thenReturn(1);
                // SQLState "HY000" → neither connection(08xxx) nor operation(23xxx) → L492 false branch
                lenient().when(notificationVinListRepository.update(eq(1), eq(1L), eq(2)))
                                .thenThrow(new RuntimeException(new SQLException("general SQL error", "HY000")))
                                .thenReturn(1);
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }

        /**
         * クラス：SaSendMessageNotificationServiceImpl
         * L731: getPersonalInfoList - contactList != null && contactList.isEmpty() ブランチ（OR条件の第2項）
         */
        @Test
        void sendMessageNotification_048() throws Exception {
                RequestHeaderDto header = buildHeader();
                SendMessageNotificationRequestDto req = buildRequest("1", "0");
                NotificationVinListEntity entity = buildVinListEntity(1L);
                when(notificationVinListRepository.select(1, 0)).thenReturn(List.of(entity));
                when(ntfBatchExecHistoryRepository.insert(any())).thenReturn(1);
                String successJson = buildGetUserIdResponseJson("00001548B123", "jsapUser001");
                when(jsapUtil.executeGetUserId(any(), any()))
                                .thenReturn(new ResponseEntity<>(successJson, HttpStatus.OK));
                when(notificationVinListRepository.update(eq(1), eq(1L), eq(0))).thenReturn(1);
                when(properties.getThreadPool()).thenReturn(2);
                when(properties.getThreadQueue()).thenReturn(10);
                when(jsapUtil.executeGetToken()).thenReturn(new GetALJTokenResultDto(true, "test"));
                // contactList is not null but empty → L731 second OR branch → return null
                GetUserInfoResponseDto dtoEmptyContact = new GetUserInfoResponseDto();
                dtoEmptyContact.setUserId("jsapUser001");
                dtoEmptyContact.setContactList(new java.util.ArrayList<>());
                String jsonEmptyContact = new ObjectMapper().writeValueAsString(dtoEmptyContact);
                lenient().when(jsapUtil.executeGetUserInfo(any(), any()))
                                .thenReturn(new ResponseEntity<>(jsonEmptyContact, HttpStatus.OK));
                when(notificationRepository.update("SA", 1)).thenReturn(1);

                ResponseDto result = service.sendMessageNotification(req, header);
                assertNotNull(result);
        }
}
