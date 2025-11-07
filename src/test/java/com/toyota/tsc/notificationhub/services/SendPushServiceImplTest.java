package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
import com.windowsazure.messaging.NotificationHubsException;
import com.windowsazure.messaging.NotificationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * テストクラス：SendPushServiceImplTest
 */
@ExtendWith(MockitoExtension.class)
class SendPushServiceImplTest {

    @InjectMocks
    private SendPushServiceImpl service;

    @Mock
    private NtfInfoRepositoryIF ntfInfoRepository;
    @Mock
    private NotificationHubUtil notificationHubUtil;

    private RequestHeaderDto header;

    @BeforeEach
    void setUp() throws Exception {
        header = new RequestHeaderDto();
        header.setCorrelationId("corr-push-001");
        // リトライ回数を2に設定（一部テストで使用）
        var f = SendPushServiceImpl.class.getDeclaredField("retryCount");
        f.setAccessible(true);
        f.setInt(service, 2);
    }

    // --------- ヘルパー ---------

    private NtfInfoEntity entity(String user, String inst, String token, String dvc, String brd, String platform,
            LocalDateTime updated) {
        return new NtfInfoEntity(user, inst, token, dvc, brd, platform, updated, updated);
    }

    /** handleExceptionのInvocationTargetExceptionラップ解除ヘルパー */
    private void invokeHandleException(Exception ex) throws Throwable {
        var m = SendPushServiceImpl.class.getDeclaredMethod("handleException", Exception.class, RequestHeaderDto.class);
        m.setAccessible(true);
        try {
            m.invoke(service, ex, header);
        } catch (InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }

    // --------- sendPush のテスト ---------

    /** クラス：SendPushServiceImpl 正常系（最新端末へPush成功）を確認するテストケース */
    @Test
    void sendPush_01() throws Exception {
        // 準備
        var list = new ArrayList<NtfInfoEntity>();
        list.add(entity("U1", "inst-1", "t1", "d1", "1", "1", LocalDateTime.now().minusHours(2)));
        list.add(entity("U1", "inst-2", "t2", "d2", "1", "1", LocalDateTime.now().minusHours(1))); // 最新

        when(ntfInfoRepository.selectAllByInternalUserId(eq("U1"))).thenReturn(list);

        NotificationOutcome outcome = mock(NotificationOutcome.class);
        when(outcome.getNotificationId()).thenReturn("noti-123");
        when(notificationHubUtil.postMessage(eq("inst-2"), eq("Hello"), eq("1"), eq("1"))).thenReturn(outcome);

        SendPushRequestDto req = new SendPushRequestDto();
        req.setInternalUserId("U1");
        req.setBody("Hello");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream prev = System.out;
        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> mockedLog = mockStatic(LogUtil.class)) {
            System.setOut(new PrintStream(out));

            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.toJson(any())).thenReturn("{json}");
            mockedCommon.when(() -> CommonUtil.getResultCode(eq("SUCCESS"))).thenReturn("SUCCESS");

            mockedLog.when(() -> LogUtil.info(eq(SendPushServiceImpl.class), anyString()))
                    .thenAnswer(inv -> {
                        System.out.println("INFO:" + inv.getArgument(1));
                        return null;
                    });

            // 実行
            String result = service.sendPush(req, header);

            // 確認
            assertEquals("SUCCESS", result);
            verify(notificationHubUtil, times(1)).postMessage(eq("inst-2"), eq("Hello"), eq("1"), eq("1"));
        } finally {
            System.setOut(prev);
        }
    }

    /** クラス：SendPushServiceImpl 異常系（必須不足でTscApplicationException）を確認するテストケース */
    @Test
    void sendPush_02() {
        // 準備
        SendPushRequestDto req = new SendPushRequestDto(); // 未設定

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認
            assertThrows(TscApplicationException.class, () -> service.sendPush(req, header));
        }
    }

    @Test
    void sendPush_03() throws Exception {
        // 準備
        var list = List.of(entity("U1", "inst-2", "t2", "d2", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId(eq("U1"))).thenReturn(list);

        // 1回目は一時的例外、2回目成功
        NotificationHubsException nhEx = mock(NotificationHubsException.class);
        when(nhEx.isTransient()).thenReturn(true);
        when(nhEx.httpStatusCode()).thenReturn(503);

        NotificationOutcome outcome = mock(NotificationOutcome.class);
        when(outcome.getNotificationId()).thenReturn("noti-456");

        when(notificationHubUtil.postMessage(eq("inst-2"), eq("Hello"), eq("1"), eq("1")))
                .thenThrow(nhEx) // 1回目
                .thenReturn(outcome); // 2回目

        SendPushRequestDto req = new SendPushRequestDto();
        req.setInternalUserId("U1");
        req.setBody("Hello");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream prev = System.out;
        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> mockedLog = mockStatic(LogUtil.class)) {
            System.setOut(new PrintStream(out));

            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.toJson(any())).thenReturn("{json}");
            mockedCommon.when(() -> CommonUtil.getResultCode(eq("SUCCESS"))).thenReturn("SUCCESS");

            mockedLog.when(() -> LogUtil.warn(eq(SendPushServiceImpl.class), anyString()))
                    .thenAnswer(inv -> {
                        System.out.println("WARN:" + inv.getArgument(1));
                        return null;
                    });

            // 実行
            String result = service.sendPush(req, header);

            // 確認
            assertEquals("SUCCESS", result);
            verify(notificationHubUtil, times(2)).postMessage(eq("inst-2"), eq("Hello"), eq("1"), eq("1"));
        } finally {
            System.setOut(prev);
        }
    }

    /** クラス：SendPushServiceImpl 例外系（非一時的例外→RuntimeException）を確認するテストケース */
    @Test
    void sendPush_04() throws Exception {
        // 準備
        var list = List.of(entity("U1", "inst-2", "t2", "d2", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId(eq("U1"))).thenReturn(list);

        NotificationHubsException nhEx = mock(NotificationHubsException.class);
        when(nhEx.isTransient()).thenReturn(false);
        when(nhEx.httpStatusCode()).thenReturn(500);
        when(nhEx.getMessage()).thenReturn("Permanent failure");

        when(notificationHubUtil.postMessage(eq("inst-2"), eq("Hello"), eq("1"), eq("1"))).thenThrow(nhEx);

        SendPushRequestDto req = new SendPushRequestDto();
        req.setInternalUserId("U1");
        req.setBody("Hello");

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認（内部TscNotificationHubsException→handleExceptionでRuntimeException）
            assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
        }
    }

    /** クラス：SendPushServiceImpl 例外系（一時的例外がリトライ上限→RuntimeException）を確認するテストケース */
    @Test
    void sendPush_05() throws Exception {
        // 準備
        var list = List.of(entity("U1", "inst-2", "t2", "d2", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId(eq("U1"))).thenReturn(list);

        NotificationHubsException nhEx = mock(NotificationHubsException.class);
        when(nhEx.isTransient()).thenReturn(true);
        when(nhEx.httpStatusCode()).thenReturn(503);
        when(nhEx.getMessage()).thenReturn("Temporary failure");

        // 常に一時的例外を投げる（retryCount=2で上限到達）
        when(notificationHubUtil.postMessage(eq("inst-2"), eq("Hello"), eq("1"), eq("1"))).thenThrow(nhEx);

        SendPushRequestDto req = new SendPushRequestDto();
        req.setInternalUserId("U1");
        req.setBody("Hello");

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認
            assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
        }
    }

    // --------- privateメソッドのテスト ---------

    /** クラス：SendPushServiceImpl privateメソッドextractTrackingIdの抽出を確認するテストケース */
    @Test
    void extractTrackingId_01() throws Exception {
        // 準備
        NotificationHubsException ex = mock(NotificationHubsException.class);
        when(ex.getMessage()).thenReturn("Azure error... Tracking ID: track-123   end");

        var m = SendPushServiceImpl.class.getDeclaredMethod("extractTrackingId", NotificationHubsException.class);
        m.setAccessible(true);

        // 実行
        String tid = (String) m.invoke(service, ex);

        // 確認
        assertEquals("track-123", tid);
    }

    /** クラス：SendPushServiceImpl privateメソッドgetLastDataの選択を確認するテストケース */
    @Test
    void getLastData_01() throws Exception {
        // 準備
        var list = new ArrayList<NtfInfoEntity>();
        list.add(entity("U1", "i1", "t1", "d1", "1", "1", LocalDateTime.now().minusDays(1)));
        list.add(entity("U1", "i2", "t2", "d2", "1", "1", LocalDateTime.now())); // 最新

        var m = SendPushServiceImpl.class.getDeclaredMethod("getLastData", List.class);
        m.setAccessible(true);

        // 実行
        NtfInfoEntity last = (NtfInfoEntity) m.invoke(service, list);

        // 確認
        assertNotNull(last);
        assertEquals("i2", last.getInstallationId());
    }

    /** クラス：SendPushServiceImpl privateメソッドgetAllDeviceDataの取得を確認するテストケース */
    @Test
    void getAllDeviceData_01() throws Exception {
        // 準備
        var list = List.of(entity("U1", "i1", "t1", "d1", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId(eq("U1"))).thenReturn(list);

        var m = SendPushServiceImpl.class.getDeclaredMethod("getAllDeviceData", String.class);
        m.setAccessible(true);

        // 実行
        @SuppressWarnings("unchecked")
        var result = (List<NtfInfoEntity>) m.invoke(service, "U1");

        // 確認
        assertEquals(1, result.size());
        assertEquals("i1", result.get(0).getInstallationId());
    }

    /** クラス：SendPushServiceImpl privateメソッドvalidateRequiredの必須検知を確認するテストケース */
    @Test
    void validateRequired_01() throws Exception {
        // 準備
        SendPushRequestDto req = new SendPushRequestDto(); // 未設定
        var m = SendPushServiceImpl.class.getDeclaredMethod("validateRequired", SendPushRequestDto.class);
        m.setAccessible(true);

        // 実行
        String missing = (String) m.invoke(service, req);

        // 確認
        assertNotNull(missing);
        assertTrue(missing.contains("internalUserId"));
        assertTrue(missing.contains("body"));
    }

    // --------- handleException のテスト ---------

    /** クラス：SendPushServiceImpl privateメソッドhandleException（SQL接続エラー）を確認するテストケース */
    @Test
    void handleException_01() throws Exception {
        var e = new SQLException("conn");
        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.isSqlConnectionError(eq(e))).thenReturn(true);
            assertThrows(RuntimeException.class, () -> invokeHandleException(e));
        }
    }

    /**
     * クラス：SendPushServiceImpl
     * privateメソッドhandleException（一時SQL→CustomSqlException）を確認するテストケース
     */
    @Test
    void handleException_02_transient() throws Exception {
        var e = new SQLTransientException("transient");
        assertThrows(CustomSqlException.class, () -> invokeHandleException(e));
    }

    /**
     * クラス：SendPushServiceImpl
     * privateメソッドhandleException（非一時SQL→CustomSqlException）を確認するテストケース
     */
    @Test
    void handleException_03_nonTransient() throws Exception {
        var e = new SQLNonTransientException("non-transient");
        assertThrows(CustomSqlException.class, () -> invokeHandleException(e));
    }

    /**
     * クラス：SendPushServiceImpl
     * privateメソッドhandleException（TscApplicationException透過）を確認するテストケース
     */
    @Test
    void handleException_04_application() throws Exception {
        var e = new TscApplicationException();
        assertThrows(TscApplicationException.class, () -> invokeHandleException(e));
    }

    /**
     * クラス：SendPushServiceImpl
     * privateメソッドhandleException（TscNotificationHubsException→RuntimeException）を確認するテストケース
     */
    @Test
    void handleException_05_hubs() throws Exception {
        var e = new TscNotificationHubsException(new RuntimeException("cause"));
        assertThrows(RuntimeException.class, () -> invokeHandleException(e));
    }

    /**
     * クラス：SendPushServiceImpl
     * privateメソッドhandleException（その他例外→RuntimeException）を確認するテストケース
     */
    @Test
    void handleException_06_other() throws Exception {
        var e = new Exception("other");
        assertThrows(RuntimeException.class, () -> invokeHandleException(e));
    }
}
