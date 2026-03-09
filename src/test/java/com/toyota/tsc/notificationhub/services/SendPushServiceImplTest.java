package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
import com.windowsazure.messaging.NotificationHubsException;
import com.windowsazure.messaging.NotificationOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class SendPushServiceImplTest {

    @InjectMocks
    private SendPushServiceImpl sut;

    @Mock
    private NtfInfoRepositoryIF ntfInfoRepository;
    @Mock
    private NotificationHubUtil notificationHubUtil;
    @Mock
    private PropertiesUtil properties;

    // -----------------------------------------------------------------------
    // sendPush（正常系）
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl FCMデバイスへのプッシュ送信が成功することを確認するテストケース */
    @Test
    void sendPush_001() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-001");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-001");

        NtfInfoEntity device = buildDevice("inst-001", "brd-1", "1", LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("iu-001"))
                .thenReturn(List.of(device));
        when(properties.getRetryCount()).thenReturn(1);
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("{\"fcm\":\"payload\"}");
        NotificationOutcome outcome = mock(NotificationOutcome.class);
        when(notificationHubUtil.postMessage(eq("inst-001"), anyString(), eq("brd-1"), eq("1")))
                .thenReturn(outcome);

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act
            ResponseDto result = sut.sendPush(request, header);

            // Assert
            assertNotNull(result);
            assertEquals("SP_SUCCESS", result.getResultCode());
        }
    }

    /** クラス：SendPushServiceImpl APNデバイスへのプッシュ送信が成功することを確認するテストケース */
    @Test
    void sendPush_002() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-002");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-002");

        NtfInfoEntity device = buildDevice("inst-002", "brd-1", "2", LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("iu-002"))
                .thenReturn(List.of(device));
        when(properties.getRetryCount()).thenReturn(1);
        when(notificationHubUtil.buildApnsPayload(anyString())).thenReturn("{\"aps\":{}}");
        NotificationOutcome outcome = mock(NotificationOutcome.class);
        when(notificationHubUtil.postMessage(eq("inst-002"), anyString(), eq("brd-1"), eq("2")))
                .thenReturn(outcome);

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act
            ResponseDto result = sut.sendPush(request, header);

            // Assert
            assertNotNull(result);
            assertEquals("SP_SUCCESS", result.getResultCode());
        }
    }

    // -----------------------------------------------------------------------
    // sendPush（バリデーション失敗）
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl requestがnullの場合TscApplicationExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_003() {
        // Arrange
        RequestHeaderDto header = buildHeader("corr-003");

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(null, header));
        }
    }

    /** クラス：SendPushServiceImpl internalUserIdが空の場合TscApplicationExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_004() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-004");

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    /** クラス：SendPushServiceImpl internalUserIdがnullの場合TscApplicationExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_005() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId(null);
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-005");

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    /** クラス：SendPushServiceImpl bodyがnullの場合TscApplicationExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_006() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-006");
        request.setBody(null);
        RequestHeaderDto header = buildHeader("corr-006");

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    /** クラス：SendPushServiceImpl bodyが空の場合TscApplicationExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_007() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-007");
        request.setBody("");
        RequestHeaderDto header = buildHeader("corr-007");

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    // -----------------------------------------------------------------------
    // sendPush（デバイス情報なし）
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl デバイス情報が空リストの場合TscApplicationExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_008() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-008");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-008");
        when(ntfInfoRepository.selectAllByInternalUserId("iu-008"))
                .thenReturn(Collections.emptyList());

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    // -----------------------------------------------------------------------
    // sendPush（例外ハンドリング）
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl TscApplicationExceptionが再スローされることを確認するテストケース */
    @Test
    void sendPush_009() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-009");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-009");
        when(ntfInfoRepository.selectAllByInternalUserId("iu-009"))
                .thenThrow(new TscApplicationException("TEST_CODE"));

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            TscApplicationException ex = assertThrows(TscApplicationException.class,
                    () -> sut.sendPush(request, header));
            assertEquals("TEST_CODE", ex.getResultCode());
        }
    }

    /** クラス：SendPushServiceImpl TscNotificationHubsExceptionが再スローされることを確認するテストケース */
    @Test
    void sendPush_010() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-010");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-010");

        NtfInfoEntity device = buildDevice("inst-010", "brd-1", "1", LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("iu-010"))
                .thenReturn(List.of(device));
        when(properties.getRetryCount()).thenReturn(1);
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("{\"fcm\":\"payload\"}");
        NotificationHubsException nhEx = mock(NotificationHubsException.class);
        when(nhEx.isTransient()).thenReturn(false);
        when(nhEx.httpStatusCode()).thenReturn(500);
        when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(nhEx);

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(TscNotificationHubsException.class, () -> sut.sendPush(request, header));
        }
    }

    /** クラス：SendPushServiceImpl SQL接続エラーの場合CustomExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_011() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-011");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-011");
        SQLException sqlEx = new SQLException("Connection failed", "08001", 0);
        when(ntfInfoRepository.selectAllByInternalUserId("iu-011"))
                .thenThrow(new RuntimeException(sqlEx));

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(CustomException.class, () -> sut.sendPush(request, header));
        }
    }

    /** クラス：SendPushServiceImpl SQL操作エラーの場合CustomSqlExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_012() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-012");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-012");
        SQLException sqlEx = new SQLException("Unique constraint", "23505");
        when(ntfInfoRepository.selectAllByInternalUserId("iu-012"))
                .thenThrow(new RuntimeException(sqlEx));

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(CustomSqlException.class, () -> sut.sendPush(request, header));
        }
    }

    /** クラス：SendPushServiceImpl その他SQLエラーの場合CustomExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_013() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-013");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-013");
        SQLException sqlEx = new SQLException("Other SQL error", "99999", 9999);
        when(ntfInfoRepository.selectAllByInternalUserId("iu-013"))
                .thenThrow(new RuntimeException(sqlEx));

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(CustomException.class, () -> sut.sendPush(request, header));
        }
    }

    /** クラス：SendPushServiceImpl 予期せぬRuntimeExceptionの場合CustomExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_014() {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-014");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-014");
        when(ntfInfoRepository.selectAllByInternalUserId("iu-014"))
                .thenThrow(new RuntimeException("Unexpected error"));

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(CustomException.class, () -> sut.sendPush(request, header));
        }
    }

    // -----------------------------------------------------------------------
    // sendPush（NotificationHubsException リトライ系）
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl Transient例外でリトライ回数内に成功することを確認するテストケース */
    @Test
    void sendPush_015() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-015");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-015");

        NtfInfoEntity device = buildDevice("inst-015", "brd-1", "1", LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("iu-015"))
                .thenReturn(List.of(device));
        when(properties.getRetryCount()).thenReturn(2);
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("{\"fcm\":\"payload\"}");

        NotificationHubsException nhEx = mock(NotificationHubsException.class);
        when(nhEx.isTransient()).thenReturn(true);
        when(nhEx.httpStatusCode()).thenReturn(503);
        NotificationOutcome outcome = mock(NotificationOutcome.class);
        when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(nhEx)
                .thenReturn(outcome);

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act
            ResponseDto result = sut.sendPush(request, header);

            // Assert
            assertNotNull(result);
        }
    }

    /** クラス：SendPushServiceImpl Transient例外でリトライ回数を超えた場合TscNotificationHubsExceptionがスローされることを確認するテストケース */
    @Test
    void sendPush_016() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-016");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-016");

        NtfInfoEntity device = buildDevice("inst-016", "brd-1", "1", LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("iu-016"))
                .thenReturn(List.of(device));
        when(properties.getRetryCount()).thenReturn(1);
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("{\"fcm\":\"payload\"}");

        NotificationHubsException nhEx = mock(NotificationHubsException.class);
        when(nhEx.isTransient()).thenReturn(true);
        when(nhEx.httpStatusCode()).thenReturn(503);
        when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(nhEx);

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act & Assert
            assertThrows(TscNotificationHubsException.class, () -> sut.sendPush(request, header));
        }
    }

    // -----------------------------------------------------------------------
    // private: createPayload
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl FCMプラットフォームでペイロードが生成されることを確認するテストケース */
    @Test
    void createPayload_001() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-cp1");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-cp1");
        NtfInfoEntity device = buildDevice("inst-cp1", "brd-1", "1", LocalDateTime.now());
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("{\"fcm\":\"payload\"}");

        Method m = getPrivateMethod("createPayload", SendPushRequestDto.class, RequestHeaderDto.class, NtfInfoEntity.class);

        // Act
        String result = (String) m.invoke(sut, request, header, device);

        // Assert
        assertNotNull(result);
        verify(notificationHubUtil).buildFcmV1Payload(anyString());
    }

    /** クラス：SendPushServiceImpl APNプラットフォームでペイロードが生成されることを確認するテストケース */
    @Test
    void createPayload_002() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-cp2");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-cp2");
        NtfInfoEntity device = buildDevice("inst-cp2", "brd-1", "2", LocalDateTime.now());
        when(notificationHubUtil.buildApnsPayload(anyString())).thenReturn("{\"aps\":{}}");

        Method m = getPrivateMethod("createPayload", SendPushRequestDto.class, RequestHeaderDto.class, NtfInfoEntity.class);

        // Act
        String result = (String) m.invoke(sut, request, header, device);

        // Assert
        assertNotNull(result);
        verify(notificationHubUtil).buildApnsPayload(anyString());
    }

    /** クラス：SendPushServiceImpl 不明なプラットフォームの場合CustomExceptionがスローされることを確認するテストケース */
    @Test
    void createPayload_003() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-cp3");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-cp3");
        NtfInfoEntity device = buildDevice("inst-cp3", "brd-1", "9", LocalDateTime.now());

        Method m = getPrivateMethod("createPayload", SendPushRequestDto.class, RequestHeaderDto.class, NtfInfoEntity.class);

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");

            // Act & Assert
            assertThrows(CustomException.class, () -> invokeUnwrap(m, sut, request, header, device));
        }
    }

    /** クラス：SendPushServiceImpl ペイロード生成で例外が発生した場合CustomExceptionがスローされることを確認するテストケース */
    @Test
    void createPayload_004() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-cp4");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-cp4");
        NtfInfoEntity device = buildDevice("inst-cp4", "brd-1", "1", LocalDateTime.now());
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenThrow(new RuntimeException("build error"));

        Method m = getPrivateMethod("createPayload", SendPushRequestDto.class, RequestHeaderDto.class, NtfInfoEntity.class);

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");

            // Act & Assert
            assertThrows(CustomException.class, () -> invokeUnwrap(m, sut, request, header, device));
        }
    }

    // -----------------------------------------------------------------------
    // private: validateRequired
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl requestがnullの場合「requestBody」が返されることを確認するテストケース */
    @Test
    void validateRequired_001() throws Exception {
        // Arrange
        Method m = getPrivateMethod("validateRequired", SendPushRequestDto.class);

        // Act
        String result = (String) m.invoke(sut, (Object) null);

        // Assert
        assertEquals("requestBody", result);
    }

    /** クラス：SendPushServiceImpl 全必須項目が設定済みの場合nullが返されることを確認するテストケース */
    @Test
    void validateRequired_002() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-vr2");
        request.setBody("{\"key\":\"value\"}");
        Method m = getPrivateMethod("validateRequired", SendPushRequestDto.class);

        // Act
        String result = (String) m.invoke(sut, request);

        // Assert
        assertNull(result);
    }

    /** クラス：SendPushServiceImpl internalUserIdがnullの場合エラーメッセージが返されることを確認するテストケース */
    @Test
    void validateRequired_003() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId(null);
        request.setBody("{\"key\":\"value\"}");
        Method m = getPrivateMethod("validateRequired", SendPushRequestDto.class);

        // Act
        String result = (String) m.invoke(sut, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("internalUserId"));
    }

    /** クラス：SendPushServiceImpl internalUserIdが空の場合エラーメッセージが返されることを確認するテストケース */
    @Test
    void validateRequired_004() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("");
        request.setBody("{\"key\":\"value\"}");
        Method m = getPrivateMethod("validateRequired", SendPushRequestDto.class);

        // Act
        String result = (String) m.invoke(sut, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("internalUserId"));
    }

    /** クラス：SendPushServiceImpl bodyがnullの場合エラーメッセージが返されることを確認するテストケース */
    @Test
    void validateRequired_005() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-vr5");
        request.setBody(null);
        Method m = getPrivateMethod("validateRequired", SendPushRequestDto.class);

        // Act
        String result = (String) m.invoke(sut, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("body"));
    }

    /** クラス：SendPushServiceImpl bodyが空の場合エラーメッセージが返されることを確認するテストケース */
    @Test
    void validateRequired_006() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-vr6");
        request.setBody("");
        Method m = getPrivateMethod("validateRequired", SendPushRequestDto.class);

        // Act
        String result = (String) m.invoke(sut, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("body"));
    }

    /** クラス：SendPushServiceImpl 複数項目が不足している場合カンマ区切りで返されることを確認するテストケース */
    @Test
    void validateRequired_007() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId(null);
        request.setBody(null);
        Method m = getPrivateMethod("validateRequired", SendPushRequestDto.class);

        // Act
        String result = (String) m.invoke(sut, request);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("internalUserId"));
        assertTrue(result.contains("body"));
    }

    // -----------------------------------------------------------------------
    // private: getLastData
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl 複数デバイスから最新データが返されることを確認するテストケース */
    @Test
    void getLastData_001() throws Exception {
        // Arrange
        NtfInfoEntity old = buildDevice("inst-old", "brd-1", "1", LocalDateTime.now().minusDays(1));
        NtfInfoEntity latest = buildDevice("inst-latest", "brd-1", "1", LocalDateTime.now());
        Method m = getPrivateMethod("getLastData", List.class);

        // Act
        NtfInfoEntity result = (NtfInfoEntity) m.invoke(sut, List.of(old, latest));

        // Assert
        assertNotNull(result);
        assertEquals("inst-latest", result.getInstallationId());
    }

    /** クラス：SendPushServiceImpl 単一デバイスがそのまま返されることを確認するテストケース */
    @Test
    void getLastData_002() throws Exception {
        // Arrange
        NtfInfoEntity device = buildDevice("inst-single", "brd-1", "1", LocalDateTime.now());
        Method m = getPrivateMethod("getLastData", List.class);

        // Act
        NtfInfoEntity result = (NtfInfoEntity) m.invoke(sut, List.of(device));

        // Assert
        assertNotNull(result);
        assertEquals("inst-single", result.getInstallationId());
    }

    /** クラス：SendPushServiceImpl 空リストの場合nullが返されることを確認するテストケース */
    @Test
    void getLastData_003() throws Exception {
        // Arrange
        Method m = getPrivateMethod("getLastData", List.class);

        // Act
        NtfInfoEntity result = (NtfInfoEntity) m.invoke(sut, Collections.emptyList());

        // Assert
        assertNull(result);
    }

    // -----------------------------------------------------------------------
    // private: getAllDeviceData
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl 全デバイス情報が取得されることを確認するテストケース */
    @Test
    void getAllDeviceData_001() throws Exception {
        // Arrange
        NtfInfoEntity device = buildDevice("inst-all", "brd-1", "1", LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("iu-all"))
                .thenReturn(List.of(device));
        Method m = getPrivateMethod("getAllDeviceData", String.class);

        // Act
        @SuppressWarnings("unchecked")
        List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(sut, "iu-all");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /** クラス：SendPushServiceImpl retryCountが0の場合にwhileループが実行されずnullが返ることを確認するテストケース */
    @Test
    void sendPush_017() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-017");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-017");

        NtfInfoEntity device = buildDevice("inst-017", "brd-1", "1", LocalDateTime.now());
        when(ntfInfoRepository.selectAllByInternalUserId("iu-017"))
                .thenReturn(List.of(device));
        when(properties.getRetryCount()).thenReturn(0);

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");
            common.when(() -> CommonUtil.getResultCode(anyString())).thenAnswer(inv -> inv.getArgument(0));
            common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

            // Act
            ResponseDto result = sut.sendPush(request, header);

            // Assert
            assertNotNull(result);
            assertEquals("SP_SUCCESS", result.getResultCode());
        }
    }

    // -----------------------------------------------------------------------
    // private: executePostMessage
    // -----------------------------------------------------------------------

    /** クラス：SendPushServiceImpl createPayloadが例外をスローした場合executePostMessageのcatch(Exception)で再スローされることを確認するテストケース */
    @Test
    void executePostMessage_001() throws Exception {
        // Arrange
        SendPushRequestDto request = new SendPushRequestDto();
        request.setInternalUserId("iu-ep1");
        request.setBody("{\"key\":\"value\"}");
        RequestHeaderDto header = buildHeader("corr-ep1");
        NtfInfoEntity device = buildDevice("inst-ep1", "brd-1", "1", LocalDateTime.now());

        when(notificationHubUtil.buildFcmV1Payload(anyString()))
                .thenThrow(new RuntimeException("build error"));
        when(properties.getRetryCount()).thenReturn(1);

        Method m = getPrivateMethod("executePostMessage",
                SendPushRequestDto.class, RequestHeaderDto.class, NtfInfoEntity.class);

        try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
            common.when(() -> CommonUtil.getLogsMessage(anyString(), any(Object[].class))).thenReturn("msg");

            // Act & Assert（createPayloadがCustomExceptionをスロー → catch(Exception e){throw e;}で再スロー）
            assertThrows(CustomException.class, () -> invokeUnwrap(m, sut, request, header, device));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private RequestHeaderDto buildHeader(String correlationId) {
        RequestHeaderDto header = new RequestHeaderDto();
        header.setCorrelationId(correlationId);
        header.setApiKey("test-api-key");
        header.setContentType("application/json");
        header.setConnection("keep-alive");
        header.setAcceptEncoding("gzip");
        header.setUserAccessKey("test-user-key");
        header.setXSmartgbook("test-smartgbook");
        return header;
    }

    private NtfInfoEntity buildDevice(String installationId, String brdCd, String platformType, LocalDateTime updatedAt) {
        return new NtfInfoEntity(
                "internal-user-id",
                installationId,
                "device-token",
                "device-id",
                brdCd,
                platformType,
                LocalDateTime.now(),
                updatedAt);
    }

    private Method getPrivateMethod(String name, Class<?>... types) throws Exception {
        Method m = SendPushServiceImpl.class.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m;
    }

    private void invokeUnwrap(Method m, Object target, Object... args) throws Exception {
        try {
            m.invoke(target, args);
        } catch (Exception e) {
            if (e.getCause() != null) {
                if (e.getCause() instanceof Exception ex)
                    throw ex;
                if (e.getCause() instanceof Error err)
                    throw err;
            }
            throw e;
        }
    }
}
