
package com.toyota.tsc.notificationhub.controllers;

import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.services.RegistNotificationDeviceInfoServiceIF;
import com.toyota.tsc.notificationhub.services.SendMessageNotificationServiceIF;
import com.toyota.tsc.notificationhub.services.SendPrimaryContactServiceIF;
import com.toyota.tsc.notificationhub.services.SendPushServiceIF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * クラス：NotificationController のテスト
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
class NotificationControllerTest {

    @Mock
    private RegistNotificationDeviceInfoServiceIF registService;
    @Mock
    private SendPushServiceIF pushService;
    @Mock
    private SendPrimaryContactServiceIF primaryContactService;
    @Mock
    private SendMessageNotificationServiceIF messageNotificationService;

    private NotificationController target;

    @BeforeEach
    void setUp() {
        target = new NotificationController(registService, pushService,
                primaryContactService, messageNotificationService);
    }

    // ----------------------------------------------------------------------
    // registNotificationDeviceInfo
    // ----------------------------------------------------------------------

    /** クラス：NotificationController 正常系（全ヘッダ指定）で200 OKとService呼び出しを確認するテストケース */
    @Test
    void registNotificationDeviceInfo_001() {
        // 準備
        String apiKey = "apiKey";
        String contentType = "application/json";
        String connection = "keep-alive";
        String acceptEncoding = "gzip";
        String correlationId = "corr-001";
        String userAccessKey = "uak";
        String xSmartgbook = "xsgb";
        RegistNotificationDeviceInfoRequestDto body = mock(RegistNotificationDeviceInfoRequestDto.class);

        ResponseDto expected = mock(ResponseDto.class);
        when(registService.registDeviceInfo(eq(body),
                any(RequestHeaderDto.class))).thenReturn(expected);

        ArgumentCaptor<RequestHeaderDto> headerCaptor = ArgumentCaptor.forClass(RequestHeaderDto.class);

        // 実行
        ResponseEntity<ResponseDto> actual = target.registNotificationDeviceInfo(
                apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey,
                xSmartgbook, body);

        // 確認
        assertEquals(200, actual.getStatusCode().value());
        assertSame(expected, actual.getBody());

        verify(registService, times(1)).registDeviceInfo(eq(body),
                headerCaptor.capture());
        RequestHeaderDto header = headerCaptor.getValue();

        assertEquals(apiKey, readField(header, "apiKey"));
        assertEquals(contentType, readField(header, "contentType"));
        assertEquals(connection, readField(header, "connection"));
        assertEquals(acceptEncoding, readField(header, "acceptEncoding"));
        assertEquals(correlationId, readField(header, "correlationId"));
        assertEquals(userAccessKey, readField(header, "userAccessKey"));
        assertEquals(xSmartgbook, readField(header, "xSmartgbook"));
        verifyNoMoreInteractions(registService);
    }

    /**
     * クラス：NotificationController 準正常系（任意ヘッダnull）でもServiceへnullが渡ることを確認するテストケース
     */
    @Test
    void registNotificationDeviceInfo_002() {
        // 準備
        String apiKey = null;
        String contentType = "application/json";
        String connection = null;
        String acceptEncoding = null;
        String correlationId = "corr-002";
        String userAccessKey = null;
        String xSmartgbook = null;
        RegistNotificationDeviceInfoRequestDto body = mock(RegistNotificationDeviceInfoRequestDto.class);

        ResponseDto expected = mock(ResponseDto.class);
        when(registService.registDeviceInfo(eq(body),
                any(RequestHeaderDto.class))).thenReturn(expected);

        ArgumentCaptor<RequestHeaderDto> headerCaptor = ArgumentCaptor.forClass(RequestHeaderDto.class);

        // 実行
        ResponseEntity<ResponseDto> actual = target.registNotificationDeviceInfo(
                apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey,
                xSmartgbook, body);

        // 確認
        assertEquals(200, actual.getStatusCode().value());
        assertSame(expected, actual.getBody());

        verify(registService, times(1)).registDeviceInfo(eq(body),
                headerCaptor.capture());
        RequestHeaderDto header = headerCaptor.getValue();

        assertNull(readField(header, "apiKey"));
        assertEquals(contentType, readField(header, "contentType"));
        assertNull(readField(header, "connection"));
        assertNull(readField(header, "acceptEncoding"));
        assertEquals(correlationId, readField(header, "correlationId"));
        assertNull(readField(header, "userAccessKey"));
        assertNull(readField(header, "xSmartgbook"));
        verifyNoMoreInteractions(registService);
    }

    /** クラス：NotificationController 異常系（Service例外throw）で例外が伝播することを確認するテストケース */
    @Test
    void registNotificationDeviceInfo_003() {
        // 準備
        String apiKey = "apiKey";
        String contentType = "application/json";
        String connection = null;
        String acceptEncoding = null;
        String correlationId = "corr-003";
        String userAccessKey = null;
        String xSmartgbook = null;
        RegistNotificationDeviceInfoRequestDto body = mock(RegistNotificationDeviceInfoRequestDto.class);

        RuntimeException expectedEx = new RuntimeException("boom");
        when(registService.registDeviceInfo(eq(body),
                any(RequestHeaderDto.class))).thenThrow(expectedEx);

        // 実行
        RuntimeException actualEx = assertThrows(RuntimeException.class,
                () -> target.registNotificationDeviceInfo(
                        apiKey, contentType, connection, acceptEncoding, correlationId,
                        userAccessKey, xSmartgbook, body));

        // 確認
        assertSame(expectedEx, actualEx);
        verify(registService, times(1)).registDeviceInfo(eq(body),
                any(RequestHeaderDto.class));
        verifyNoMoreInteractions(registService);
    }

    /**
     * クラス：NotificationController 異常系（Service戻り値null）で200
     * OKかつbodyがnullになることを確認するテストケース
     */
    @Test
    void registNotificationDeviceInfo_004() {
        // 準備
        String apiKey = "apiKey";
        String contentType = "application/json";
        String connection = null;
        String acceptEncoding = null;
        String correlationId = "corr-004";
        String userAccessKey = null;
        String xSmartgbook = null;
        RegistNotificationDeviceInfoRequestDto body = mock(RegistNotificationDeviceInfoRequestDto.class);

        when(registService.registDeviceInfo(eq(body),
                any(RequestHeaderDto.class))).thenReturn(null);

        // 実行
        ResponseEntity<ResponseDto> actual = target.registNotificationDeviceInfo(
                apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey,
                xSmartgbook, body);

        // 確認
        assertEquals(200, actual.getStatusCode().value());
        assertNull(actual.getBody());
        verify(registService, times(1)).registDeviceInfo(eq(body),
                any(RequestHeaderDto.class));
        verifyNoMoreInteractions(registService);
    }

    // ----------------------------------------------------------------------
    // sendPush
    // ----------------------------------------------------------------------

    /** クラス：NotificationController 正常系（全ヘッダ指定）で200 OKとService呼び出しを確認するテストケース */
    @Test
    void sendPush_001() {
        // 準備
        String apiKey = "apiKey";
        String contentType = "application/json";
        String connection = "keep-alive";
        String acceptEncoding = "gzip";
        String correlationId = "corr-101";
        String userAccessKey = "uak";
        String xSmartgbook = "xsgb";
        SendPushRequestDto body = mock(SendPushRequestDto.class);

        ResponseDto expected = mock(ResponseDto.class);
        when(pushService.sendPush(eq(body),
                any(RequestHeaderDto.class))).thenReturn(expected);

        ArgumentCaptor<RequestHeaderDto> headerCaptor = ArgumentCaptor.forClass(RequestHeaderDto.class);

        // 実行
        ResponseEntity<ResponseDto> actual = target.sendPush(
                apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey,
                xSmartgbook, body);

        // 確認
        assertEquals(200, actual.getStatusCode().value());
        assertSame(expected, actual.getBody());

        verify(pushService, times(1)).sendPush(eq(body), headerCaptor.capture());
        RequestHeaderDto header = headerCaptor.getValue();

        assertEquals(apiKey, readField(header, "apiKey"));
        assertEquals(contentType, readField(header, "contentType"));
        assertEquals(connection, readField(header, "connection"));
        assertEquals(acceptEncoding, readField(header, "acceptEncoding"));
        assertEquals(correlationId, readField(header, "correlationId"));
        assertEquals(userAccessKey, readField(header, "userAccessKey"));
        assertEquals(xSmartgbook, readField(header, "xSmartgbook"));
        verifyNoMoreInteractions(pushService);
    }

    /**
     * クラス：NotificationController 準正常系（任意ヘッダnull）でもServiceへnullが渡ることを確認するテストケース
     */
    @Test
    void sendPush_002() {
        // 準備
        String apiKey = null;
        String contentType = "application/json";
        String connection = null;
        String acceptEncoding = null;
        String correlationId = "corr-102";
        String userAccessKey = null;
        String xSmartgbook = null;
        SendPushRequestDto body = mock(SendPushRequestDto.class);

        ResponseDto expected = mock(ResponseDto.class);
        when(pushService.sendPush(eq(body),
                any(RequestHeaderDto.class))).thenReturn(expected);

        ArgumentCaptor<RequestHeaderDto> headerCaptor = ArgumentCaptor.forClass(RequestHeaderDto.class);

        // 実行
        ResponseEntity<ResponseDto> actual = target.sendPush(
                apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey,
                xSmartgbook, body);

        // 確認
        assertEquals(200, actual.getStatusCode().value());
        assertSame(expected, actual.getBody());

        verify(pushService, times(1)).sendPush(eq(body), headerCaptor.capture());
        RequestHeaderDto header = headerCaptor.getValue();

        assertNull(readField(header, "apiKey"));
        assertEquals(contentType, readField(header, "contentType"));
        assertNull(readField(header, "connection"));
        assertNull(readField(header, "acceptEncoding"));
        assertEquals(correlationId, readField(header, "correlationId"));
        assertNull(readField(header, "userAccessKey"));
        assertNull(readField(header, "xSmartgbook"));
        verifyNoMoreInteractions(pushService);
    }

    /** クラス：NotificationController 異常系（Service例外throw）で例外が伝播することを確認するテストケース */
    @Test
    void sendPush_003() {
        // 準備
        String apiKey = "apiKey";
        String contentType = "application/json";
        String connection = null;
        String acceptEncoding = null;
        String correlationId = "corr-103";
        String userAccessKey = null;
        String xSmartgbook = null;
        SendPushRequestDto body = mock(SendPushRequestDto.class);

        RuntimeException expectedEx = new RuntimeException("boom");
        when(pushService.sendPush(eq(body),
                any(RequestHeaderDto.class))).thenThrow(expectedEx);

        // 実行
        RuntimeException actualEx = assertThrows(RuntimeException.class,
                () -> target.sendPush(
                        apiKey, contentType, connection, acceptEncoding, correlationId,
                        userAccessKey, xSmartgbook, body));

        // 確認
        assertSame(expectedEx, actualEx);
        verify(pushService, times(1)).sendPush(eq(body),
                any(RequestHeaderDto.class));
        verifyNoMoreInteractions(pushService);
    }

    /**
     * クラス：NotificationController 異常系（Service戻り値null）で200
     * OKかつbodyがnullになることを確認するテストケース
     */
    @Test
    void sendPush_004() {
        // 準備
        String apiKey = "apiKey";
        String contentType = "application/json";
        String connection = null;
        String acceptEncoding = null;
        String correlationId = "corr-104";
        String userAccessKey = null;
        String xSmartgbook = null;
        SendPushRequestDto body = mock(SendPushRequestDto.class);

        when(pushService.sendPush(eq(body),
                any(RequestHeaderDto.class))).thenReturn(null);

        // 実行
        ResponseEntity<ResponseDto> actual = target.sendPush(
                apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey,
                xSmartgbook, body);

        // 確認
        assertEquals(200, actual.getStatusCode().value());
        assertNull(actual.getBody());
        verify(pushService, times(1)).sendPush(eq(body),
                any(RequestHeaderDto.class));
        verifyNoMoreInteractions(pushService);
    }

    // ----------------------------------------------------------------------
    // sendPrimaryContact
    // ----------------------------------------------------------------------

    /** クラス：NotificationController 正常系（全ヘッダ指定）で200 OKとService呼び出しを確認するテストケース */
    @Test
    void sendPrimaryContact_001() {
        // 準備
        String apiKey = "apiKey";
        String contentType = "application/json";
        String connection = "keep-alive";
        String acceptEncoding = "gzip";
        String correlationId = "corr-201";
        String userAccessKey = "uak";
        String xSmartgbook = "xsgb";
        SendPrimaryContactRequestDto body = mock(SendPrimaryContactRequestDto.class);

        ResponseDto expected = mock(ResponseDto.class);
        when(primaryContactService.sendPrimaryContact(eq(body),
                any(RequestHeaderDto.class)))
                .thenReturn(expected);

        ArgumentCaptor<RequestHeaderDto> headerCaptor = ArgumentCaptor.forClass(RequestHeaderDto.class);

        // 実行
        ResponseEntity<ResponseDto> actual = target.sendPrimaryContact(
                apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey,
                xSmartgbook, body);

        // 確認
        assertEquals(200, actual.getStatusCode().value());
        assertSame(expected, actual.getBody());

        verify(primaryContactService, times(1)).sendPrimaryContact(eq(body),
                headerCaptor.capture());
        RequestHeaderDto header = headerCaptor.getValue();

        assertEquals(apiKey, readField(header, "apiKey"));
        assertEquals(contentType, readField(header, "contentType"));
        assertEquals(connection, readField(header, "connection"));
        assertEquals(acceptEncoding, readField(header, "acceptEncoding"));
        assertEquals(correlationId, readField(header, "correlationId"));
        assertEquals(userAccessKey, readField(header, "userAccessKey"));
        assertEquals(xSmartgbook, readField(header, "xSmartgbook"));
        verifyNoMoreInteractions(primaryContactService);
    }

    /**
     * クラス：NotificationController 準正常系（任意ヘッダnull）でもServiceへnullが渡ることを確認するテストケース
     */
    @Test
    void sendPrimaryContact_002() {
        // 準備
        String apiKey = null;
        String contentType = "application/json";
        String connection = null;
        String acceptEncoding = null;
        String correlationId = "corr-202";
        String userAccessKey = null;
        String xSmartgbook = null;
        SendPrimaryContactRequestDto body = mock(SendPrimaryContactRequestDto.class);

        ResponseDto expected = mock(ResponseDto.class);
        when(primaryContactService.sendPrimaryContact(eq(body),
                any(RequestHeaderDto.class)))
                .thenReturn(expected);

        ArgumentCaptor<RequestHeaderDto> headerCaptor = ArgumentCaptor.forClass(RequestHeaderDto.class);

        // 実行
        ResponseEntity<ResponseDto> actual = target.sendPrimaryContact(
                apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey,
                xSmartgbook, body);

        // 確認
        assertEquals(200, actual.getStatusCode().value());
        assertSame(expected, actual.getBody());

        verify(primaryContactService, times(1)).sendPrimaryContact(eq(body),
                headerCaptor.capture());
        RequestHeaderDto header = headerCaptor.getValue();

        assertNull(readField(header, "apiKey"));
        assertEquals(contentType, readField(header, "contentType"));
        assertNull(readField(header, "connection"));
        assertNull(readField(header, "acceptEncoding"));
        assertEquals(correlationId, readField(header, "correlationId"));
        assertNull(readField(header, "userAccessKey"));
        assertNull(readField(header, "xSmartgbook"));
        verifyNoMoreInteractions(primaryContactService);
    }

    /** クラス：NotificationController 異常系（Service例外throw）で例外が伝播することを確認するテストケース */
    @Test
    void sendPrimaryContact_003() {
        // 準備
        String apiKey = "apiKey";
        String contentType = "application/json";
        String connection = null;
        String acceptEncoding = null;
        String correlationId = "corr-203";
        String userAccessKey = null;
        String xSmartgbook = null;
        SendPrimaryContactRequestDto body = mock(SendPrimaryContactRequestDto.class);

        RuntimeException expectedEx = new RuntimeException("boom");
        when(primaryContactService.sendPrimaryContact(eq(body),
                any(RequestHeaderDto.class)))
                .thenThrow(expectedEx);

        // 実行
        RuntimeException actualEx = assertThrows(RuntimeException.class,
                () -> target.sendPrimaryContact(
                        apiKey, contentType, connection, acceptEncoding, correlationId,
                        userAccessKey, xSmartgbook, body));

        // 確認
        assertSame(expectedEx, actualEx);
        verify(primaryContactService, times(1)).sendPrimaryContact(eq(body),
                any(RequestHeaderDto.class));
        verifyNoMoreInteractions(primaryContactService);
    }

    /**
     * クラス：NotificationController 異常系（Service戻り値null）で200
     * OKかつbodyがnullになることを確認するテストケース
     */
    @Test
    void sendPrimaryContact_004() {
        // 準備
        String apiKey = "apiKey";
        String contentType = "application/json";
        String connection = null;
        String acceptEncoding = null;
        String correlationId = "corr-204";
        String userAccessKey = null;
        String xSmartgbook = null;
        SendPrimaryContactRequestDto body = mock(SendPrimaryContactRequestDto.class);

        when(primaryContactService.sendPrimaryContact(eq(body),
                any(RequestHeaderDto.class))).thenReturn(null);

        // 実行
        ResponseEntity<ResponseDto> actual = target.sendPrimaryContact(
                apiKey, contentType, connection, acceptEncoding, correlationId,
                userAccessKey,
                xSmartgbook, body);

        // 確認
        assertEquals(200, actual.getStatusCode().value());
        assertNull(actual.getBody());
        verify(primaryContactService, times(1)).sendPrimaryContact(eq(body),
                any(RequestHeaderDto.class));
        verifyNoMoreInteractions(primaryContactService);
    }

    // ----------------------------------------------------------------------
    // helper
    // ----------------------------------------------------------------------

    private static Object readField(Object target, String fieldName) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Failed to read field: " + fieldName, e);
        }
    }
}
