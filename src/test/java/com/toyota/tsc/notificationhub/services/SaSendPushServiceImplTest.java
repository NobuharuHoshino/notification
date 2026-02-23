
package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.JsapUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.repositories.SaNtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.SaNtfInfoRepositoryIF;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * クラス：SaSendPushServiceImpl sendPush等の処理結果を確認するテストケース
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SaSendPushServiceImplTest {

    @Mock
    private SaNtfInfoRepositoryIF saNtfInfoRepository;

    @Mock
    private NotificationHubUtil notificationHubUtil;

    @Mock
    private JsapUtil jsapUtil;

    /** executeGetToken が返すトークンJSON */
    private static final String TOKEN_JSON =
            "{\"access_token\":\"tok\",\"token_type\":\"Bearer\","
            + "\"expires_in\":\"3600\",\"scope\":\"sc\",\"jti\":\"jti\"}";

    // --------------------
    // sendPush (public)
    // --------------------

    /** クラス：SaSendPushServiceImpl 正常系（FCM）で成功レスポンスが返ることを確認するテストケース */
    @Test
    void sendPush_001() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(saNtfInfoRepository.select("iu")).thenReturn(userData);
        when(userData.getPlatformType()).thenReturn("1"); // FCM

        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("payload");
        when(jsapUtil.executeGetToken()).thenReturn(ResponseEntity.ok(TOKEN_JSON));
        when(jsapUtil.executeGetUserId("iu", "corr"))
                .thenReturn(ResponseEntity.ok("{\"resultCode\":\"00001548B123\",\"userId\":\"u\"}"));
        when(jsapUtil.executePushRequest("u", "payload", "tok"))
                .thenReturn(ResponseEntity.ok("{\"resultCode\":\"000000\"}"));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_OK");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);

            // Act
            ResponseDto res = sut.sendPush(request, header);

            // Assert
            assertNotNull(res);
            assertEquals("RC_OK", res.getResultCode());
        }
    }

    /**
     * クラス：SaSendPushServiceImpl
     * 異常系（request=null）でTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void sendPush_002() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = null;
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_BAD");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            // Act + Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    /**
     * クラス：SaSendPushServiceImpl 異常系（ユーザデータ未取得）でTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void sendPush_003() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        when(saNtfInfoRepository.select("iu")).thenReturn(null);

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            // Act + Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    /**
     * クラス：SaSendPushServiceImpl 異常系（payload生成例外）でTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void sendPush_004() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(saNtfInfoRepository.select("iu")).thenReturn(userData);
        when(userData.getPlatformType()).thenReturn("1");

        // buildFcmV1Payload throws → createPayload catches and throws TscApplicationException
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenThrow(new RuntimeException("boom"));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            // Act + Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    /** クラス：SaSendPushServiceImpl SQL接続系例外時にCustomExceptionとなることを確認するテストケース */
    @Test
    void sendPush_005() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SQLException sqlEx = new SQLException("conn", "08S01");
        when(saNtfInfoRepository.select("iu")).thenThrow(new RuntimeException(sqlEx));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class);
                MockedStatic<ExtractSqlExceptionUtil> exu = mockStatic(ExtractSqlExceptionUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            exu.when(() -> ExtractSqlExceptionUtil.findSqlException(any())).thenReturn(sqlEx);
            exu.when(() -> ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)).thenReturn(true);
            exu.when(() -> ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)).thenReturn(false);

            // Act + Assert
            assertThrows(CustomException.class, () -> sut.sendPush(request, header));
        }
    }

    /** クラス：SaSendPushServiceImpl SQL操作系例外時にCustomSqlExceptionとなることを確認するテストケース */
    @Test
    void sendPush_006() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SQLException sqlEx = new SQLException("op", "23505");
        when(saNtfInfoRepository.select("iu")).thenThrow(new RuntimeException(sqlEx));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class);
                MockedStatic<ExtractSqlExceptionUtil> exu = mockStatic(ExtractSqlExceptionUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            exu.when(() -> ExtractSqlExceptionUtil.findSqlException(any())).thenReturn(sqlEx);
            exu.when(() -> ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)).thenReturn(false);
            exu.when(() -> ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)).thenReturn(true);

            // Act + Assert
            assertThrows(CustomSqlException.class, () -> sut.sendPush(request, header));
        }
    }

    // --------------------
    // private methods (reflection)
    // --------------------

    /**
     * クラス：SaSendPushServiceImpl validateRequired
     * request=nullでrequestBodyが返ることを確認するテストケース
     */
    @Test
    void validateRequired_001() throws Exception {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);
        Method m = SaSendPushServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPushRequestDto.class);
        m.setAccessible(true);

        // Act
        String res = (String) m.invoke(sut, (Object) null);

        // Assert
        assertEquals("requestBody", res);
    }

    /**
     * クラス：SaSendPushServiceImpl validateRequired
     * internalUserIdが空文字でinternalUserIdが返ることを確認するテストケース
     */
    @Test
    void validateRequired_002() throws Exception {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);
        Method m = SaSendPushServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPushRequestDto.class);
        m.setAccessible(true);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        when(request.getInternalUserId()).thenReturn("");
        when(request.getBody()).thenReturn("bodyStr");

        // Act
        String res = (String) m.invoke(sut, request);

        // Assert
        assertEquals("internalUserId", res);
    }

    /** クラス：SaSendPushServiceImpl validateRequired body不足でbodyが返ることを確認するテストケース */
    @Test
    void validateRequired_003() throws Exception {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);
        Method m = SaSendPushServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPushRequestDto.class);
        m.setAccessible(true);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn(""); // 空文字 → body missing

        // Act
        String res = (String) m.invoke(sut, request);

        // Assert
        assertEquals("body", res);
    }

    /**
     * クラス：SaSendPushServiceImpl createPayload
     * APNSの場合にAPNS用payloadが返ることを確認するテストケース
     */
    @Test
    void createPayload_001() throws Exception {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);
        Method m = SaSendPushServiceImpl.class.getDeclaredMethod("createPayload",
                SendPushRequestDto.class, RequestHeaderDto.class, SaNtfInfoEntity.class);
        m.setAccessible(true);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        when(request.getBody()).thenReturn("bodyStr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(userData.getPlatformType()).thenReturn("2"); // APN
        when(notificationHubUtil.buildApnsPayload(anyString())).thenReturn("apns");

        // Act
        String payload = (String) m.invoke(sut, request, mock(RequestHeaderDto.class), userData);

        // Assert
        assertEquals("apns", payload);
    }

    /**
     * クラス：SaSendPushServiceImpl createPayload FCMの場合にFCM用payloadが返ることを確認するテストケース
     */
    @Test
    void createPayload_002() throws Exception {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);
        Method m = SaSendPushServiceImpl.class.getDeclaredMethod("createPayload",
                SendPushRequestDto.class, RequestHeaderDto.class, SaNtfInfoEntity.class);
        m.setAccessible(true);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        when(request.getBody()).thenReturn("bodyStr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(userData.getPlatformType()).thenReturn("1"); // FCM
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("fcm");

        // Act
        String payload = (String) m.invoke(sut, request, mock(RequestHeaderDto.class), userData);

        // Assert
        assertEquals("fcm", payload);
    }

    /** クラス：SaSendPushServiceImpl getData repositoryの結果が返ることを確認するテストケース */
    @Test
    void getData_001() throws Exception {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);
        Method m = SaSendPushServiceImpl.class.getDeclaredMethod("getData", String.class);
        m.setAccessible(true);

        SaNtfInfoEntity e = mock(SaNtfInfoEntity.class);
        when(saNtfInfoRepository.select("iu-x")).thenReturn(e);

        // Act
        SaNtfInfoEntity res = (SaNtfInfoEntity) m.invoke(sut, "iu-x");

        // Assert
        assertSame(e, res);
        verify(saNtfInfoRepository, times(1)).select("iu-x");
    }

    /*
     * =====================================================================
     * 追加分（分岐網羅100%向け）
     * =====================================================================
     */

    /**
     * クラス：SaSendPushServiceImpl validateRequired
     * internalUserIdがnullの場合にinternalUserIdが返ることを確認するテストケース
     */
    @Test
    void validateRequired_004() throws Exception {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);
        Method m = SaSendPushServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPushRequestDto.class);
        m.setAccessible(true);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        when(request.getInternalUserId()).thenReturn(null); // null側
        when(request.getBody()).thenReturn("bodyStr");

        // Act
        String res = (String) m.invoke(sut, request);

        // Assert
        assertEquals("internalUserId", res);
    }

    /**
     * クラス：SaSendPushServiceImpl validateRequired
     * bodyがnullの場合にbodyが返ることを確認するテストケース
     */
    @Test
    void validateRequired_005() throws Exception {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);
        Method m = SaSendPushServiceImpl.class.getDeclaredMethod("validateRequired",
                SendPushRequestDto.class);
        m.setAccessible(true);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn(null); // null側

        // Act
        String res = (String) m.invoke(sut, request);

        // Assert
        assertEquals("body", res);
    }

    /**
     * クラス：SaSendPushServiceImpl createPayload
     * platformTypeが不正値の場合にTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void createPayload_003() throws Exception {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);
        Method m = SaSendPushServiceImpl.class.getDeclaredMethod(
                "createPayload", SendPushRequestDto.class, RequestHeaderDto.class,
                SaNtfInfoEntity.class);
        m.setAccessible(true);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(userData.getPlatformType()).thenReturn("9"); // default分岐

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);

            // Act + Assert（InvocationTargetException の cause を投げ直す）
            assertThrows(TscApplicationException.class, () -> {
                try {
                    m.invoke(sut, request, header, userData);
                } catch (InvocationTargetException ex) {
                    throw (RuntimeException) ex.getCause();
                }
            });
        }
    }

    /**
     * クラス：SaSendPushServiceImpl
     * 異常系（getUserId結果不正）でTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void sendPush_007() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(saNtfInfoRepository.select("iu")).thenReturn(userData);
        when(userData.getPlatformType()).thenReturn("1");
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("payload");

        when(jsapUtil.executeGetToken()).thenReturn(ResponseEntity.ok(TOKEN_JSON));
        // getUserIdのresultCodeを成功以外にする
        when(jsapUtil.executeGetUserId("iu", "corr"))
                .thenReturn(ResponseEntity.ok("{\"resultCode\":\"NG\",\"userId\":\"u\"}"));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            // Act + Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    /**
     * クラス：SaSendPushServiceImpl
     * 異常系（pushRequest結果不正）でTscNotificationHubsExceptionとなることを確認するテストケース
     */
    @Test
    void sendPush_008() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(saNtfInfoRepository.select("iu")).thenReturn(userData);
        when(userData.getPlatformType()).thenReturn("1");
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("payload");

        when(jsapUtil.executeGetToken()).thenReturn(ResponseEntity.ok(TOKEN_JSON));
        when(jsapUtil.executeGetUserId("iu", "corr"))
                .thenReturn(ResponseEntity.ok("{\"resultCode\":\"00001548B123\",\"userId\":\"u\"}"));
        // pushRequestを失敗にする
        when(jsapUtil.executePushRequest("u", "payload", "tok"))
                .thenReturn(ResponseEntity.ok("{\"resultCode\":\"NG\"}"));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            // Act + Assert
            assertThrows(TscNotificationHubsException.class, () -> sut.sendPush(request, header));
        }
    }

    /**
     * クラス：SaSendPushServiceImpl
     * 異常系（executeGetToken例外）でCustomExceptionとなることを確認するテストケース
     */
    @Test
    void sendPush_009() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(saNtfInfoRepository.select("iu")).thenReturn(userData);
        when(userData.getPlatformType()).thenReturn("1");
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("payload");

        // executeGetToken throws RuntimeException（SQLなし）→ catch(Exception) → else → CustomException
        when(jsapUtil.executeGetToken()).thenThrow(new RuntimeException("token-fail"));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            // Act + Assert
            assertThrows(CustomException.class, () -> sut.sendPush(request, header));
        }
    }

    /**
     * クラス：SaSendPushServiceImpl
     * SQL例外が存在するが接続/操作どちらでもない場合にCustomExceptionとなることを確認するテストケース
     */
    @Test
    void sendPush_010() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SQLException sqlEx = new SQLException("other", "99999");
        when(saNtfInfoRepository.select("iu")).thenThrow(new RuntimeException(sqlEx));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class);
                MockedStatic<ExtractSqlExceptionUtil> exu = mockStatic(ExtractSqlExceptionUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            exu.when(() -> ExtractSqlExceptionUtil.findSqlException(any())).thenReturn(sqlEx);
            exu.when(() -> ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)).thenReturn(false);
            exu.when(() -> ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)).thenReturn(false);

            // Act + Assert
            assertThrows(CustomException.class, () -> sut.sendPush(request, header));
        }
    }

    /**
     * クラス：SaSendPushServiceImpl
     * 異常系（JSAPトークンが空）でTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void sendPush_011() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(saNtfInfoRepository.select("iu")).thenReturn(userData);
        when(userData.getPlatformType()).thenReturn("1");
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("payload");

        // access_token が空文字 → RESULT_TOKENFOUND_ERROR
        when(jsapUtil.executeGetToken()).thenReturn(
                ResponseEntity.ok("{\"access_token\":\"\",\"token_type\":\"Bearer\","
                        + "\"expires_in\":\"3600\",\"scope\":\"sc\",\"jti\":\"jti\"}"));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            // Act + Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

    /**
     * クラス：SaSendPushServiceImpl
     * 異常系（JSAPトークンがnull）でTscApplicationExceptionとなることを確認するテストケース
     */
    @Test
    void sendPush_012() {
        // Arrange
        SaSendPushServiceImpl sut = new SaSendPushServiceImpl(saNtfInfoRepository,
                notificationHubUtil, jsapUtil);

        SendPushRequestDto request = mock(SendPushRequestDto.class);
        RequestHeaderDto header = mock(RequestHeaderDto.class);
        when(header.getCorrelationId()).thenReturn("corr");
        when(request.getInternalUserId()).thenReturn("iu");
        when(request.getBody()).thenReturn("bodyStr");

        SaNtfInfoEntity userData = mock(SaNtfInfoEntity.class);
        when(saNtfInfoRepository.select("iu")).thenReturn(userData);
        when(userData.getPlatformType()).thenReturn("1");
        when(notificationHubUtil.buildFcmV1Payload(anyString())).thenReturn("payload");

        // access_token が null → token == null → RESULT_TOKENFOUND_ERROR
        when(jsapUtil.executeGetToken()).thenReturn(
                ResponseEntity.ok("{\"access_token\":null,\"token_type\":\"Bearer\","
                        + "\"expires_in\":\"3600\",\"scope\":\"sc\",\"jti\":\"jti\"}"));

        try (MockedStatic<CommonUtil> cu = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> lu = mockStatic(LogUtil.class)) {

            cu.when(() -> CommonUtil.getSaMessage(anyString(),
                    org.mockito.Mockito.<Object[]>any())).thenReturn("msg");
            cu.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cu.when(() -> CommonUtil.getResultCode(anyString())).thenReturn("RC_ERR");
            lu.when(() -> LogUtil.info(any(), anyString())).thenAnswer(inv -> null);
            lu.when(() -> LogUtil.error(any(), anyString())).thenAnswer(inv -> null);

            // Act + Assert
            assertThrows(TscApplicationException.class, () -> sut.sendPush(request, header));
        }
    }

}
