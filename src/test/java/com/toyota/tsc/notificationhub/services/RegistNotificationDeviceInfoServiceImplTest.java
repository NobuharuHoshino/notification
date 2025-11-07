package com.toyota.tsc.notificationhub.services;

import java.lang.reflect.InvocationTargetException;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
import com.windowsazure.messaging.NotificationHubsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class RegistNotificationDeviceInfoServiceImplTest {

    @InjectMocks
    private RegistNotificationDeviceInfoServiceImpl service;

    @Mock
    private NtfInfoRepositoryIF ntfInfoRepository;
    @Mock
    private NotificationHubUtil notificationHubUtil;

    private RequestHeaderDto header;

    @BeforeEach
    void setUp() throws Exception {
        header = new RequestHeaderDto();
        header.setCorrelationId("corr-003");
        // retryCountを2に固定
        var f = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredField("retryCount");
        f.setAccessible(true);
        f.setInt(service, 2);
    }

    private RegistNotificationDeviceInfoRequestDto baseRequest() {
        var req = new RegistNotificationDeviceInfoRequestDto();
        req.setInternalUserId("U1");
        req.setPlatform("1");
        req.setDeviceToken("token-new");
        req.setDvcId("d1");
        req.setBrdCd("1");
        return req;
    }

    private NtfInfoEntity entity(String user, String inst, String token, String dvc, String brd, String pf,
            LocalDateTime updated) {
        var e = new NtfInfoEntity(user, inst, token, dvc, brd, pf, updated, updated);
        return e;
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * 正常系（新規登録→古い端末削除→Installation処理）を確認するテストケース
     */
    @Test
    void registDeviceInfo_01() throws Exception {
        // 準備：既存3件（古い2件を削除対象）
        var existing = new ArrayList<NtfInfoEntity>();
        existing.add(entity("U1", "old-1", "token-1", "d-old1", "1", "1", LocalDateTime.now().minusDays(3)));
        existing.add(entity("U1", "old-2", "token-2", "d-old2", "1", "1", LocalDateTime.now().minusDays(2)));
        existing.add(entity("U1", "keep-1", "token-3", "d-old3", "1", "1", LocalDateTime.now().minusDays(1)));

        when(ntfInfoRepository.selectAllByInternalUserId(eq("U1"))).thenReturn(existing);
        when(ntfInfoRepository.upsert(any())).thenReturn(1);
        when(ntfInfoRepository.delete(eq("U1"), anyString())).thenReturn(1);

        // NotificationHubUtil の void メソッドはスタブ不要（デフォルトで何もしない）

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream prev = System.out;
        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class);
                MockedStatic<LogUtil> mockedLog = mockStatic(LogUtil.class)) {
            System.setOut(new PrintStream(out));

            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.toJson(any())).thenReturn("{json}");
            mockedCommon.when(() -> CommonUtil.getResultCode(eq("SUCCESS"))).thenReturn("SUCCESS");

            mockedLog.when(() -> LogUtil.info(eq(RegistNotificationDeviceInfoServiceImpl.class), anyString()))
                    .thenAnswer(inv -> {
                        System.out.println("INFO:" + inv.getArgument(1));
                        return null;
                    });
            mockedLog.when(() -> LogUtil.warn(eq(RegistNotificationDeviceInfoServiceImpl.class), anyString()))
                    .thenAnswer(inv -> {
                        System.out.println("WARN:" + inv.getArgument(1));
                        return null;
                    });

            // 実行
            String result = service.registDeviceInfo(baseRequest(), header);

            // 確認
            assertEquals("SUCCESS", result);
            verify(ntfInfoRepository, atLeastOnce()).upsert(any());
            verify(ntfInfoRepository, atLeast(1)).delete(eq("U1"), anyString());
            verify(notificationHubUtil, atLeastOnce()).deleteInstallation(anyString(), eq("1"));
            verify(notificationHubUtil, times(1))
                    .upsertInstallation(anyString(), eq("1"), eq("U1"), eq("1"), eq("token-new"));
        } finally {
            System.setOut(prev);
        }
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * 正常系（既存に同一トークン→Installation処理をSKIP）を確認するテストケース
     */
    @Test
    void registDeviceInfo_02() throws Exception {
        // 準備：既存に同一トークンを含む
        var existing = List.of(entity("U1", "inst-1", "token-new", "d1", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId(eq("U1"))).thenReturn(existing);

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            mockedCommon.when(() -> CommonUtil.toJson(any())).thenReturn("{json}");
            mockedCommon.when(() -> CommonUtil.getResultCode(eq("SUCCESS"))).thenReturn("SUCCESS");

            // 実行
            String result = service.registDeviceInfo(baseRequest(), header);

            // 確認
            assertEquals("SUCCESS", result);
            // upsertInstallation は呼ばれない（SKIP）
            verify(notificationHubUtil, never())
                    .upsertInstallation(anyString(), anyString(), anyString(), anyString(), anyString());
        }
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * 異常系（必須不足でTscApplicationException）を確認するテストケース
     */
    @Test
    void registDeviceInfo_03() {
        // 準備
        var req = new RegistNotificationDeviceInfoRequestDto(); // 未設定

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認
            assertThrows(TscApplicationException.class, () -> service.registDeviceInfo(req, header));
        }
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl 例外系（NotificationHub
     * upsertが非一時的例外→RuntimeException）を確認するテストケース
     */
    @Test
    void registDeviceInfo_04() throws Exception {
        // 準備：既存0件でupsertに進む
        when(ntfInfoRepository.selectAllByInternalUserId(eq("U1"))).thenReturn(new ArrayList<>());
        when(ntfInfoRepository.upsert(any())).thenReturn(1);

        NotificationHubsException nhEx = mock(NotificationHubsException.class);
        when(nhEx.isTransient()).thenReturn(false);
        when(nhEx.httpStatusCode()).thenReturn(500);
        doThrow(nhEx).when(notificationHubUtil)
                .upsertInstallation(anyString(), anyString(), anyString(), anyString(), anyString());

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認（内部でTscNotificationHubsException→handleExceptionでRuntimeException）
            assertThrows(RuntimeException.class, () -> service.registDeviceInfo(baseRequest(), header));
        }
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * 例外系（DeleteInstallationが一時的例外をリトライ上限→RuntimeException）を確認するテストケース
     */
    @Test
    void registDeviceInfo_05() throws Exception {
        // 準備：既存1件（deleteが発生）
        var existing = List.of(entity("U1", "inst-del", "token-old", "d", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId(eq("U1"))).thenReturn(existing);
        when(ntfInfoRepository.upsert(any())).thenReturn(1);

        NotificationHubsException nhEx = mock(NotificationHubsException.class);
        when(nhEx.isTransient()).thenReturn(true);
        when(nhEx.httpStatusCode()).thenReturn(503);
        doThrow(nhEx).when(notificationHubUtil).deleteInstallation(eq("inst-del"), eq("1"));

        try (MockedStatic<CommonUtil> mockedCommon = mockStatic(CommonUtil.class)) {
            mockedCommon.when(() -> CommonUtil.getMessage(anyString(), any())).thenReturn("MSG");
            // 実行・確認（最終的に TscNotificationHubsException → handleException で RuntimeException）
            assertThrows(RuntimeException.class, () -> service.registDeviceInfo(baseRequest(), header));
        }
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドvalidateRequiredの必須検知を確認するテストケース
     */
    @Test
    void validateRequired_01() throws Exception {
        // 準備
        var req = new RegistNotificationDeviceInfoRequestDto(); // 未設定
        var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("validateRequired",
                RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);

        // 実行
        String missing = (String) m.invoke(service, req);

        // 確認
        assertNotNull(missing);
        assertTrue(missing.contains("internalUserId"));
        assertTrue(missing.contains("platform"));
        assertTrue(missing.contains("deviceToken"));
        assertTrue(missing.contains("dvcId"));
        assertTrue(missing.contains("brdCd"));
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドisValidPlatformの妥当値（1/2）を確認するテストケース
     */
    @Test
    void isValidPlatform_01() throws Exception {
        var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("isValidPlatform", String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(service, "1"));
        assertTrue((Boolean) m.invoke(service, "2"));
        assertFalse((Boolean) m.invoke(service, "9"));
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドisValidBrdCdの妥当値（1/2）を確認するテストケース
     */
    @Test
    void isValidBrdCd_01() throws Exception {
        var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("isValidBrdCd", String.class);
        m.setAccessible(true);
        assertTrue((Boolean) m.invoke(service, "1"));
        assertTrue((Boolean) m.invoke(service, "2"));
        assertFalse((Boolean) m.invoke(service, "9"));
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドextractByDeviceTokenの抽出を確認するテストケース
     */
    @Test
    void extractByDeviceToken_01() throws Exception {
        var list = List.of(
                entity("U1", "i1", "t1", "d1", "1", "1", LocalDateTime.now()),
                entity("U1", "i2", "token-new", "d2", "1", "1", LocalDateTime.now()));
        var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("extractByDeviceToken", List.class,
                String.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        var result = (List<NtfInfoEntity>) m.invoke(service, list, "token-new");
        assertEquals(1, result.size());
        assertEquals("i2", result.get(0).getInstallationId());
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドextractToDeviceTokenListの変換を確認するテストケース
     */
    @Test
    void extractToDeviceTokenList_01() throws Exception {
        var list = List.of(
                entity("U1", "i1", "t1", "d1", "1", "1", LocalDateTime.now()),
                entity("U1", "i2", "t2", "d2", "1", "1", LocalDateTime.now()));
        var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("extractToDeviceTokenList", List.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        var result = (List<String>) m.invoke(service, list);
        assertEquals(List.of("t1", "t2"), result);
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドhandleException（SQL接続エラー）を確認するテストケース
     */
    @Test
    void handleException_01() throws Exception {
        var e = new SQLException("conn");
        try (MockedStatic<ExtractSqlExceptionUtil> mocked = mockStatic(ExtractSqlExceptionUtil.class)) {
            mocked.when(() -> ExtractSqlExceptionUtil.isSqlConnectionError(eq(e))).thenReturn(true);
            var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("handleException", Exception.class,
                    RequestHeaderDto.class);
            m.setAccessible(true);
            assertThrows(RuntimeException.class, () -> invokeHandleException(e));
        }
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドhandleException（一時/非一時SQLでCustomSqlException）を確認するテストケース
     */
    @Test
    void handleException_02() throws Exception {
        var e = new SQLTransientException("transient");
        var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("handleException", Exception.class,
                RequestHeaderDto.class);
        m.setAccessible(true);
        assertThrows(RuntimeException.class, () -> invokeHandleException(e));
        assertThrows(RuntimeException.class, () -> invokeHandleException(e));
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドhandleException（TscApplicationException透過）を確認するテストケース
     */
    @Test
    void handleException_03() throws Exception {
        var e = new TscApplicationException();
        var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("handleException", Exception.class,
                RequestHeaderDto.class);
        m.setAccessible(true);
        assertThrows(RuntimeException.class, () -> invokeHandleException(e));
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドhandleException（TscNotificationHubsException→RuntimeException）を確認するテストケース
     */
    @Test
    void handleException_04() throws Exception {
        var e = new TscNotificationHubsException(new RuntimeException("cause"));
        var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("handleException", Exception.class,
                RequestHeaderDto.class);
        m.setAccessible(true);
        assertThrows(RuntimeException.class, () -> invokeHandleException(e));
    }

    /**
     * クラス：RegistNotificationDeviceInfoServiceImpl
     * privateメソッドhandleException（その他例外→RuntimeException）を確認するテストケース
     */
    @Test
    void handleException_05() throws Exception {
        var e = new Exception("other");
        var m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("handleException", Exception.class,
                RequestHeaderDto.class);
        m.setAccessible(true);
        assertThrows(RuntimeException.class, () -> invokeHandleException(e));
    }

    private void invokeHandleException(Exception ex) throws Throwable {
        var m = RegistNotificationDeviceInfoServiceImpl.class
                .getDeclaredMethod("handleException", Exception.class, RequestHeaderDto.class);
        m.setAccessible(true);
        try {
            m.invoke(service, ex, header);
        } catch (InvocationTargetException ite) {
            // ラップを剥がして元例外を再スロー（JUnitのExecutableはThrowableを投げられる）
            throw ite.getTargetException();
        }
    }

}
