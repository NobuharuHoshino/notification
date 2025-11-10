package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
import com.windowsazure.messaging.NotificationHubsException;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 実装：RegistNotificationDeviceInfoServiceImpl に対する包括テスト
 * - 例外ハンドリングは ExtractSqlExceptionUtil.findSqlException に基づくチェーン探索に一本化済み
 * - while (cnt < retryCount) の true/false（0回転）両分岐を網羅
 * - validate は異常時に例外、正常時は null を返す仕様に準拠
 */
@ExtendWith(MockitoExtension.class)
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
        header.setCorrelationId("corr-xxx");

        // デフォルトのリトライ回数（必要に応じてテスト内で上書き）
        Field f = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredField("retryCount");
        f.setAccessible(true);
        f.setInt(service, 2);
    }

    private RegistNotificationDeviceInfoRequestDto baseRequest() {
        RegistNotificationDeviceInfoRequestDto req = new RegistNotificationDeviceInfoRequestDto();
        req.setInternalUserId("U1");
        req.setPlatform("1");
        req.setDeviceToken("token-new");
        req.setDvcId("d1");
        req.setBrdCd("1");
        return req;
    }

    private NtfInfoEntity entity(String user, String inst, String token, String dvc, String brd, String pf,
            LocalDateTime ts) {
        return new NtfInfoEntity(user, inst, token, dvc, brd, pf, ts, ts);
    }

    // ---------- 正常系 ----------

    /** 新規トークン・端末数<=2・Azure成功 → 正常終了 */
    @Test
    void registDeviceInfo_success_basic() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        List<NtfInfoEntity> deviceList = Arrays.asList(
                entity("U1", "i-1", "tok-old-1", "d-old1", "1", "1", LocalDateTime.now()),
                entity("U1", "i-2", "tok-old-2", "d-old2", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(deviceList);
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class))).thenReturn(1);

        doNothing().when(notificationHubUtil).deleteInstallation(anyString(), anyString());
        doNothing().when(notificationHubUtil).upsertInstallation(anyString(), anyString(), anyString(), anyString(),
                anyString());

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                    .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            String code = service.registDeviceInfo(req, header);
            assertEquals("SUCCESS_CODE", code);
        }
    }

    /** 既存トークン一致 → SKIPログ出力・正常終了 */
    @Test
    void registDeviceInfo_skip_existingToken() {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        List<NtfInfoEntity> deviceList = Collections.singletonList(
                entity("U1", "i-1", "token-new", "d1", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(deviceList);

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any()))
                    .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            String code = service.registDeviceInfo(req, header);
            assertEquals("SUCCESS_CODE", code);
        }
    }

    /** 端末数>2 → 古いデータ削除まで実行（delete>0）で正常終了 */
    @Test
    void registDeviceInfo_success_deleteOld() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        List<NtfInfoEntity> deviceList = Arrays.asList(
                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now().minusDays(3)),
                entity("U1", "i-2", "tok2", "d2", "1", "1", LocalDateTime.now().minusDays(2)),
                entity("U1", "i-3", "tok3", "d3", "1", "1", LocalDateTime.now().minusDays(1)));
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(deviceList);
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class))).thenReturn(1);
        when(ntfInfoRepository.delete(anyString(), anyString())).thenReturn(1);

        doNothing().when(notificationHubUtil).deleteInstallation(anyString(), anyString());
        doNothing().when(notificationHubUtil).upsertInstallation(anyString(), anyString(), anyString(), anyString(),
                anyString());

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
            cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

            String code = service.registDeviceInfo(req, header);
            assertEquals("SUCCESS_CODE", code);
        }
    }

    // ---------- 異常系（DB） ----------

    /** upsert=0 → RuntimeException */
    @Test
    void registDeviceInfo_fail_upsertCountZero() {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(Collections.emptyList());
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class))).thenReturn(0);

        assertThrows(RuntimeException.class, () -> service.registDeviceInfo(req, header));
    }

    /** delete=0 → RuntimeException */
    @Test
    void registDeviceInfo_fail_deleteZero() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        List<NtfInfoEntity> deviceList = Arrays.asList(
                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now().minusDays(3)),
                entity("U1", "i-2", "tok2", "d2", "1", "1", LocalDateTime.now().minusDays(2)),
                entity("U1", "i-3", "tok3", "d3", "1", "1", LocalDateTime.now().minusDays(1)));
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(deviceList);
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class))).thenReturn(1);
        when(ntfInfoRepository.delete(anyString(), anyString())).thenReturn(0);

        doNothing().when(notificationHubUtil).deleteInstallation(anyString(), anyString());
        doNothing().when(notificationHubUtil).upsertInstallation(anyString(), anyString(), anyString(), anyString(),
                anyString());

        assertThrows(RuntimeException.class, () -> service.registDeviceInfo(req, header));
    }

    /** cause: SQLException（接続系）→ RuntimeException */
    @Test
    void registDeviceInfo_fail_sqlConnection_byCause() {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(Collections.emptyList());

        // upsert が RuntimeException(cause=SQLException) を投げる
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class)))
                .thenThrow(new RuntimeException(new SQLException("conn")));

        try (MockedStatic<ExtractSqlExceptionUtil> st = Mockito.mockStatic(ExtractSqlExceptionUtil.class);
                MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            st.when(() -> ExtractSqlExceptionUtil.findSqlException(any(Throwable.class)))
                    .thenAnswer(inv -> {
                        Throwable t = inv.getArgument(0);
                        // チェーンから SQLException を返す動きを模倣
                        while (t != null) {
                            if (t instanceof SQLException)
                                return (SQLException) t;
                            t = t.getCause();
                        }
                        return null;
                    });
            st.when(() -> ExtractSqlExceptionUtil.isSqlConnectionError(any(SQLException.class)))
                    .thenReturn(true);
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                    .thenReturn("MSG");

            assertThrows(RuntimeException.class, () -> service.registDeviceInfo(req, header));
        }
    }

    /** cause: SQLTransientException → CustomSqlException */
    @Test
    void registDeviceInfo_fail_sqlTransient_byCause_toCustom() {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(Collections.emptyList());
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class)))
                .thenThrow(new RuntimeException(new SQLTransientException("trans")));

        try (MockedStatic<ExtractSqlExceptionUtil> st = Mockito.mockStatic(ExtractSqlExceptionUtil.class)) {
            st.when(() -> ExtractSqlExceptionUtil.findSqlException(any(Throwable.class)))
                    .thenAnswer(inv -> {
                        Throwable t = inv.getArgument(0);
                        while (t != null) {
                            if (t instanceof SQLException)
                                return (SQLException) t;
                            t = t.getCause();
                        }
                        return null;
                    });
            st.when(() -> ExtractSqlExceptionUtil.isSqlConnectionError(any(SQLException.class)))
                    .thenReturn(false);

            assertThrows(CustomSqlException.class, () -> service.registDeviceInfo(req, header));
        }
    }

    /** cause: SQLNonTransientException → CustomSqlException */
    @Test
    void registDeviceInfo_fail_sqlNonTransient_byCause_toCustom() {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(Collections.emptyList());
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class)))
                .thenThrow(new RuntimeException(new SQLNonTransientException("nontrans")));

        try (MockedStatic<ExtractSqlExceptionUtil> st = Mockito.mockStatic(ExtractSqlExceptionUtil.class)) {
            st.when(() -> ExtractSqlExceptionUtil.findSqlException(any(Throwable.class)))
                    .thenAnswer(inv -> {
                        Throwable t = inv.getArgument(0);
                        while (t != null) {
                            if (t instanceof SQLException)
                                return (SQLException) t;
                            t = t.getCause();
                        }
                        return null;
                    });
            st.when(() -> ExtractSqlExceptionUtil.isSqlConnectionError(any(SQLException.class)))
                    .thenReturn(false);

            assertThrows(CustomSqlException.class, () -> service.registDeviceInfo(req, header));
        }
    }

    /** cause: その他の SQLException → RuntimeException */
    @Test
    void registDeviceInfo_fail_otherSQLException_byCause_toRuntime() {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(Collections.emptyList());
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class)))
                .thenThrow(new RuntimeException(new SQLException("other")));

        try (MockedStatic<ExtractSqlExceptionUtil> st = Mockito.mockStatic(ExtractSqlExceptionUtil.class)) {
            st.when(() -> ExtractSqlExceptionUtil.findSqlException(any(Throwable.class)))
                    .thenAnswer(inv -> {
                        Throwable t = inv.getArgument(0);
                        while (t != null) {
                            if (t instanceof SQLException)
                                return (SQLException) t;
                            t = t.getCause();
                        }
                        return null;
                    });
            st.when(() -> ExtractSqlExceptionUtil.isSqlConnectionError(any(SQLException.class)))
                    .thenReturn(false);

            assertThrows(RuntimeException.class, () -> service.registDeviceInfo(req, header));
        }
    }

    // ---------- 異常系（業務／通知） ----------

    /** validate：不正ブランドコード → TscApplicationException */
    @Test
    void registDeviceInfo_fail_invalidBrand() {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        req.setBrdCd("9");
        assertThrows(TscApplicationException.class, () -> service.registDeviceInfo(req, header));
    }

    /** validate：不正プラットフォーム → TscApplicationException */
    @Test
    void registDeviceInfo_fail_invalidPlatform() {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        req.setPlatform("9");
        assertThrows(TscApplicationException.class, () -> service.registDeviceInfo(req, header));
    }

    /**
     * NotificationHubs：非Transient例外 →
     * TscNotificationHubsExceptionが内部で発生→RuntimeExceptionに変換
     */
    @Test
    void registDeviceInfo_fail_notificationHub_nonTransient_toRuntime() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(Collections.emptyList());
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class))).thenReturn(1);

        NotificationHubsException ex = mock(NotificationHubsException.class);
        when(ex.isTransient()).thenReturn(false);
        when(ex.httpStatusCode()).thenReturn(500);

        doThrow(ex).when(notificationHubUtil)
                .upsertInstallation(anyString(), anyString(), anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class, () -> service.registDeviceInfo(req, header));
    }

    /** NotificationHubs：delete中の一般例外 → RuntimeExceptionへ */
    @Test
    void registDeviceInfo_fail_delete_unexpectedRuntime() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        List<NtfInfoEntity> list = Collections.singletonList(
                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class))).thenReturn(1);

        doAnswer(inv -> {
            throw new RuntimeException("unexpected");
        })
                .when(notificationHubUtil).deleteInstallation(anyString(), anyString());

        assertThrows(RuntimeException.class, () -> service.registDeviceInfo(req, header));
    }

    // ---------- whileループの分岐網羅（true/false） ----------

    /** executeDeleteInstallation：Transient→リトライ成功（true側の典型） */
    @Test
    void executeDeleteInstallation_retry_thenSuccess() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        NtfInfoEntity e = entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now());

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "executeDeleteInstallation",
                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, NtfInfoEntity.class);
        m.setAccessible(true);

        NotificationHubsException ex = mock(NotificationHubsException.class);
        when(ex.isTransient()).thenReturn(true);
        when(ex.httpStatusCode()).thenReturn(500);

        // 1回目は例外、2回目成功
        doThrow(ex).doNothing().when(notificationHubUtil).deleteInstallation(anyString(), anyString());

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn("MSG");
            m.invoke(service, req, header, e);
            verify(notificationHubUtil, times(2)).deleteInstallation(anyString(), anyString());
        }
    }

    /**
     * executeDeleteInstallation：Transient上限到達→TscNotificationHubsException（true側の失敗）
     */
    @Test
    void executeDeleteInstallation_retryExceeded_toTscException() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        NtfInfoEntity e = entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now());

        // retryCount を取得し、少なくとも 2 に
        Field f = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredField("retryCount");
        f.setAccessible(true);
        int raw = f.getInt(service);
        if (raw <= 0) {
            f.setInt(service, 2);
            raw = 2;
        }
        final int attempts = raw;

        NotificationHubsException ex = mock(NotificationHubsException.class);
        when(ex.isTransient()).thenReturn(true);
        when(ex.httpStatusCode()).thenReturn(503);

        final int[] counter = { 0 };
        doAnswer(inv -> {
            if (counter[0]++ < attempts)
                throw ex;
            return null;
        })
                .when(notificationHubUtil).deleteInstallation(anyString(), anyString());

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "executeDeleteInstallation",
                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, NtfInfoEntity.class);
        m.setAccessible(true);

        try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
            cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn("MSG");
            InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                    () -> m.invoke(service, req, header, e));
            assertInstanceOf(TscNotificationHubsException.class, ite.getCause());
            verify(notificationHubUtil, times(attempts)).deleteInstallation(anyString(), anyString());
        }
    }

    /** executeDeleteInstallation：非Transient→即TscNotificationHubsException */
    @Test
    void executeDeleteInstallation_nonTransient_toTscException() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        NtfInfoEntity e = entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now());

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "executeDeleteInstallation",
                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, NtfInfoEntity.class);
        m.setAccessible(true);

        NotificationHubsException ex = mock(NotificationHubsException.class);
        when(ex.isTransient()).thenReturn(false);
        when(ex.httpStatusCode()).thenReturn(500);

        doThrow(ex).when(notificationHubUtil).deleteInstallation(anyString(), anyString());

        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header, e));
        assertTrue(ite.getCause() instanceof TscNotificationHubsException);
    }

    /** executeDeleteInstallation：retryCount=0 → while不成立（false側） */
    @Test
    void executeDeleteInstallation_retryCountZero_skipsLoop() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        NtfInfoEntity e = entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now());

        Field f = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredField("retryCount");
        f.setAccessible(true);
        f.setInt(service, 0);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "executeDeleteInstallation",
                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, NtfInfoEntity.class);
        m.setAccessible(true);

        m.invoke(service, req, header, e);
        verify(notificationHubUtil, times(0)).deleteInstallation(anyString(), anyString());
    }

    /** executeUpsertInstallation：Transient→リトライ成功（true側） */
    @Test
    void executeUpsertInstallation_retry_thenSuccess() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "executeUpsertInstallation",
                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, String.class);
        m.setAccessible(true);

        NotificationHubsException ex = mock(NotificationHubsException.class);
        when(ex.isTransient()).thenReturn(true);
        when(ex.httpStatusCode()).thenReturn(500);

        doThrow(ex).doNothing().when(notificationHubUtil)
                .upsertInstallation(anyString(), anyString(), anyString(), anyString(), anyString());

        m.invoke(service, req, header, "inst-1");
        verify(notificationHubUtil, times(2))
                .upsertInstallation(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    /**
     * executeUpsertInstallation：Transient上限到達→TscNotificationHubsException（true側の失敗）
     */
    @Test
    void executeUpsertInstallation_retryExceeded_toTscException() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "executeUpsertInstallation",
                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, String.class);
        m.setAccessible(true);

        NotificationHubsException ex = mock(NotificationHubsException.class);
        when(ex.isTransient()).thenReturn(true);
        when(ex.httpStatusCode()).thenReturn(503);

        doThrow(ex).doThrow(ex).when(notificationHubUtil)
                .upsertInstallation(anyString(), anyString(), anyString(), anyString(), anyString());

        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header, "inst-1"));
        assertTrue(ite.getCause() instanceof TscNotificationHubsException);
    }

    /** executeUpsertInstallation：非Transient→即TscNotificationHubsException */
    @Test
    void executeUpsertInstallation_nonTransient_toTscException() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "executeUpsertInstallation",
                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, String.class);
        m.setAccessible(true);

        NotificationHubsException ex = mock(NotificationHubsException.class);
        when(ex.isTransient()).thenReturn(false);
        when(ex.httpStatusCode()).thenReturn(500);

        doThrow(ex).when(notificationHubUtil)
                .upsertInstallation(anyString(), anyString(), anyString(), anyString(), anyString());

        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header, "inst-1"));
        assertTrue(ite.getCause() instanceof TscNotificationHubsException);
    }

    /** executeUpsertInstallation：retryCount=0 → while不成立（false側） */
    @Test
    void executeUpsertInstallation_retryCountZero_skipsLoop() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();

        Field f = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredField("retryCount");
        f.setAccessible(true);
        f.setInt(service, 0);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "executeUpsertInstallation",
                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, String.class);
        m.setAccessible(true);

        m.invoke(service, req, header, "inst-1");
        verify(notificationHubUtil, times(0))
                .upsertInstallation(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ---------- Private補助メソッドの動作検証（必要分） ----------

    @Test
    void extractByDeviceToken_notFound() throws Exception {
        List<NtfInfoEntity> list = Arrays.asList(
                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now()),
                entity("U1", "i-2", "tok2", "d2", "1", "1", LocalDateTime.now()));
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "extractByDeviceToken", List.class, String.class);
        m.setAccessible(true);
        List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(service, list, "tokX");
        assertEquals(0, result.size());
    }

    @Test
    void extractByDeviceToken_foundOne() throws Exception {
        List<NtfInfoEntity> list = Arrays.asList(
                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now()),
                entity("U1", "i-2", "tok2", "d2", "1", "1", LocalDateTime.now()));
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "extractByDeviceToken", List.class, String.class);
        m.setAccessible(true);
        List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(service, list, "tok1");
        assertEquals(1, result.size());
        assertEquals("tok1", result.get(0).getDeviceToken());
    }

    @Test
    void extractToDeviceTokenList_basic() throws Exception {
        List<NtfInfoEntity> list = Arrays.asList(
                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now()),
                entity("U1", "i-2", "tok2", "d2", "1", "1", LocalDateTime.now()));
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "extractToDeviceTokenList", List.class);
        m.setAccessible(true);
        List<String> tokens = (List<String>) m.invoke(service, list);
        assertEquals(Arrays.asList("tok1", "tok2"), tokens);
    }

    @Test
    void getAllDeviceData_basic() throws Exception {
        List<NtfInfoEntity> list = Collections.singletonList(
                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now()));
        when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "getAllDeviceData", String.class);
        m.setAccessible(true);
        List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(service, "U1");
        assertEquals(1, result.size());
    }

    @Test
    void upsertDeviceInfo_returnsCount() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        when(ntfInfoRepository.upsert(any(NtfInfoEntity.class))).thenReturn(1);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "upsertDeviceInfo", RegistNotificationDeviceInfoRequestDto.class,
                RequestHeaderDto.class, String.class);
        m.setAccessible(true);

        int count = (int) m.invoke(service, req, header, "inst-1");
        assertEquals(1, count);
    }

    @Test
    void deleteDeviceData_sumIsPositive() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        List<NtfInfoEntity> deviceList = Arrays.asList(
                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now().minusDays(3)),
                entity("U1", "i-2", "tok2", "d2", "1", "1", LocalDateTime.now().minusDays(2)),
                entity("U1", "i-3", "tok3", "d3", "1", "1", LocalDateTime.now().minusDays(1)));
        when(ntfInfoRepository.delete(anyString(), anyString())).thenReturn(1);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "deleteDeviceData", RegistNotificationDeviceInfoRequestDto.class,
                RequestHeaderDto.class, List.class);
        m.setAccessible(true);

        int sum = (int) m.invoke(service, req, header, deviceList);
        assertTrue(sum >= 1);
    }

    // ---------- validateの挙動（異常系：必須／正常系：null） ----------

    @Test
    void validate_requiredMissing_throwsAppException() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = new RegistNotificationDeviceInfoRequestDto();
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validate", RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class);
        m.setAccessible(true);

        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header));
        assertTrue(ite.getCause() instanceof TscApplicationException);
    }

    @Test
    void validate_invalidBrand_throwsAppException() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        req.setBrdCd("9");
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validate", RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class);
        m.setAccessible(true);

        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header));
        assertTrue(ite.getCause() instanceof TscApplicationException);
    }

    @Test
    void validate_invalidPlatform_throwsAppException() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        req.setPlatform("9");
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validate", RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class);
        m.setAccessible(true);

        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header));
        assertTrue(ite.getCause() instanceof TscApplicationException);
    }

    @Test
    void validate_normal_returnsNull() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validate", RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class);
        m.setAccessible(true);

        Object result = m.invoke(service, req, header);
        assertNull(result); // 正常時は null を返す仕様
    }

    // ---------- validateRequired の詳細網羅 ----------

    @Test
    void validateRequired_requestNull_returnsRequestBody() throws Exception {
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validateRequired", RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, new Object[] { null });
        assertEquals("requestBody", result);
    }

    @Test
    void validateRequired_multipleMissing_containsAll() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = new RegistNotificationDeviceInfoRequestDto();
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validateRequired", RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, req);
        assertTrue(result.contains("internalUserId"));
        assertTrue(result.contains("platform"));
        assertTrue(result.contains("deviceToken"));
        assertTrue(result.contains("dvcId"));
        assertTrue(result.contains("brdCd"));
    }

    @Test
    void validateRequired_normal_returnsNull() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validateRequired", RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, req);
        assertNull(result);
    }

    // ---------- 判定メソッド ----------

    @Test
    void isValidPlatform_trueFor1() throws Exception {
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "isValidPlatform", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "1");
        assertTrue(v);
    }

    @Test
    void isValidPlatform_trueFor2() throws Exception {
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "isValidPlatform", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "2");
        assertTrue(v);
    }

    @Test
    void isValidPlatform_falseForOthers() throws Exception {
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "isValidPlatform", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "9");
        assertFalse(v);
    }

    @Test
    void isValidBrdCd_trueFor1() throws Exception {
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "1");
        assertTrue(v);
    }

    @Test
    void isValidBrdCd_trueFor2() throws Exception {
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "2");
        assertTrue(v);
    }

    @Test
    void isValidBrdCd_falseForOthers() throws Exception {
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "isValidBrdCd", String.class);
        m.setAccessible(true);
        boolean v = (boolean) m.invoke(service, "9");
        assertFalse(v);
    }

    /** executeUpsertInstallation：一般例外（非 NotificationHubsException）を再throwすることを確認 */
    @Test
    void executeUpsertInstallation_06_generalException_propagates() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();

        // private メソッドの準備
        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "executeUpsertInstallation",
                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, String.class);
        m.setAccessible(true);

        // NotificationHubsException ではない一般例外をスローさせる
        doAnswer(inv -> {
            throw new RuntimeException("unexpected");
        })
                .when(notificationHubUtil)
                .upsertInstallation(anyString(), anyString(), anyString(), anyString(), anyString());

        // 反射呼び出し → InvocationTargetException で包まれる
        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                () -> m.invoke(service, req, header, "inst-1"));

        // 原因が RuntimeException であること（catch(Exception e){ throw e; } が働いた証拠）
        assertTrue(ite.getCause() instanceof RuntimeException);
    }

    /** validateRequired：internalUserId が null → "internalUserId" を検出 */
    @Test
    void validateRequired_09_null_internalUserId_detected() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        req.setInternalUserId(null);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validateRequired", RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);

        String result = (String) m.invoke(service, req);
        assertNotNull(result);
        assertTrue(result.contains("internalUserId"));
    }

    /** validateRequired：platform が null → "platform" を検出 */
    @Test
    void validateRequired_10_null_platform_detected() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        req.setPlatform(null);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validateRequired", RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);

        String result = (String) m.invoke(service, req);
        assertNotNull(result);
        assertTrue(result.contains("platform"));
    }

    /** validateRequired：deviceToken が null → "deviceToken" を検出 */
    @Test
    void validateRequired_11_null_deviceToken_detected() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        req.setDeviceToken(null);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validateRequired", RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);

        String result = (String) m.invoke(service, req);
        assertNotNull(result);
        assertTrue(result.contains("deviceToken"));
    }

    /** validateRequired：dvcId が null → "dvcId" を検出 */
    @Test
    void validateRequired_12_null_dvcId_detected() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        req.setDvcId(null);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validateRequired", RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);

        String result = (String) m.invoke(service, req);
        assertNotNull(result);
        assertTrue(result.contains("dvcId"));
    }

    /** validateRequired：brdCd が null → "brdCd" を検出 */
    @Test
    void validateRequired_13_null_brdCd_detected() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = baseRequest();
        req.setBrdCd(null);

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validateRequired", RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);

        String result = (String) m.invoke(service, req);
        assertNotNull(result);
        assertTrue(result.contains("brdCd"));
    }

    /** validateRequired：全項目が空文字 → 全て検出される（isEmpty() 側の再確認） */
    @Test
    void validateRequired_14_allEmpty_detected() throws Exception {
        RegistNotificationDeviceInfoRequestDto req = new RegistNotificationDeviceInfoRequestDto();
        req.setInternalUserId("");
        req.setPlatform("");
        req.setDeviceToken("");
        req.setDvcId("");
        req.setBrdCd("");

        Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                "validateRequired", RegistNotificationDeviceInfoRequestDto.class);
        m.setAccessible(true);

        String result = (String) m.invoke(service, req);
        assertNotNull(result);
        assertTrue(result.contains("internalUserId"));
        assertTrue(result.contains("platform"));
        assertTrue(result.contains("deviceToken"));
        assertTrue(result.contains("dvcId"));
        assertTrue(result.contains("brdCd"));
    }

}