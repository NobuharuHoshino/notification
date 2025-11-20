package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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
 * クラス：SendPushServiceImpl 分岐網羅100%を目指すテストクラス
 * 対象の主な分岐は以下（実装に基づく）：
 * - sendPush の
 * catch：SQLException系（接続/Transient/NonTransient/その他）、TscApplication、TscNotificationHubs、その他
 * - executePostMessage：whileリトライ（成功、リトライ成功、上限到達、非Transient、一般例外、0回転）
 * - バリデーション：validate / validateRequired（null/空文字）
 * - 補助：extractTrackingId（一致/不一致）、getAllDeviceData、getLastData
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
                header.setCorrelationId("corr-sp-001");

                // デフォルトのリトライ回数（必要なテストで上書き）
                Field f = SendPushServiceImpl.class.getDeclaredField("retryCount");
                f.setAccessible(true);
                f.setInt(service, 2);
        }

        private SendPushRequestDto baseRequest() {
                SendPushRequestDto req = new SendPushRequestDto();
                req.setInternalUserId("U1");
                req.setBody("Hello");
                return req;
        }

        private NtfInfoEntity entity(String user, String inst, String token, String dvc, String brd, String pf,
                        LocalDateTime ts) {
                return new NtfInfoEntity(user, inst, token, dvc, brd, pf, ts, ts);
        }

        // ========== sendPush ==========

        /** クラス：SendPushServiceImpl sendPush 正常完了を確認するテストケース */
        @Test
        void sendPush_01() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                List<NtfInfoEntity> list = Arrays.asList(
                                entity("U1", "i-1", "tok-old", "d-old", "1", "1", LocalDateTime.now().minusDays(1)),
                                entity("U1", "i-2", "tok-new", "d-new", "1", "1", LocalDateTime.now()));
                when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(mock(NotificationOutcome.class));
                // ログ検証用
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream orig = System.out;
                System.setOut(new PrintStream(out));
                try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                                        .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                                        .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
                        cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

                        // 実行
                        String code = service.sendPush(req, header).getResultCode();

                        // 確認
                        assertEquals("SUCCESS_CODE", code);
                        assertTrue(out.toString().contains("RS07I00002")); // 正常終了ログ
                } finally {
                        System.setOut(orig);
                }
        }

        /**
         * クラス：SendPushServiceImpl sendPush SQL接続エラーでRuntimeExceptionとなることを確認するテストケース
         */
        @Test
        void sendPush_02() {
                // 準備
                SendPushRequestDto req = baseRequest();
                when(ntfInfoRepository.selectAllByInternalUserId("U1"))
                                .thenThrow(new RuntimeException(new SQLException("conn")));
                try (MockedStatic<ExtractSqlExceptionUtil> st = Mockito.mockStatic(ExtractSqlExceptionUtil.class);
                                MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {

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
                                        .thenReturn(true);
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                                        .thenReturn("MSG");

                        // 実行・確認
                        assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
                }
        }

        /**
         * クラス：SendPushServiceImpl sendPush
         * SQLTransientExceptionでCustomSqlExceptionとなることを確認するテストケース
         */
        @Test
        void sendPush_03() {
                // 準備
                SendPushRequestDto req = baseRequest();
                when(ntfInfoRepository.selectAllByInternalUserId("U1"))
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

                        // 実行・確認
                        assertThrows(CustomSqlException.class, () -> service.sendPush(req, header));
                }
        }

        /**
         * クラス：SendPushServiceImpl sendPush
         * SQLNonTransientExceptionでCustomSqlExceptionとなることを確認するテストケース
         */
        @Test
        void sendPush_04() {
                // 準備
                SendPushRequestDto req = baseRequest();
                when(ntfInfoRepository.selectAllByInternalUserId("U1"))
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

                        // 実行・確認
                        assertThrows(CustomSqlException.class, () -> service.sendPush(req, header));
                }
        }

        /**
         * クラス：SendPushServiceImpl sendPush
         * その他SQLExceptionでRuntimeExceptionとなることを確認するテストケース
         */
        @Test
        void sendPush_05() {
                // 準備
                SendPushRequestDto req = baseRequest();
                when(ntfInfoRepository.selectAllByInternalUserId("U1"))
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

                        // 実行・確認
                        assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
                }
        }

        /**
         * クラス：SendPushServiceImpl sendPush
         * バリデーションエラー(TscApplicationException)を確認するテストケース
         */
        @Test
        void sendPush_06() {
                // 準備（internalUserId 欠落）
                SendPushRequestDto req = new SendPushRequestDto();
                req.setInternalUserId("");
                req.setBody("Hello");

                // 実行・確認
                assertThrows(TscApplicationException.class, () -> service.sendPush(req, header));
        }

        /**
         * クラス：SendPushServiceImpl sendPush
         * TscNotificationHubsExceptionがRuntimeExceptionに変換されることを確認するテストケース
         */
        @Test
        void sendPush_07() throws NotificationHubsException {
                // 準備
                SendPushRequestDto req = baseRequest();
                List<NtfInfoEntity> list = Collections.singletonList(
                                entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now()));
                when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);

                NotificationHubsException ex = mock(NotificationHubsException.class);
                when(ex.isTransient()).thenReturn(false);
                when(ex.httpStatusCode()).thenReturn(500);
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenThrow(ex);

                // 実行・確認（sendPush 側 catch で RuntimeException に変換）
                assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
        }

        /**
         * クラス：SendPushServiceImpl sendPush 予期せぬ例外がRuntimeExceptionに変換されることを確認するテストケース
         */
        @Test
        void sendPush_08() throws NotificationHubsException {
                // 準備
                SendPushRequestDto req = baseRequest();
                List<NtfInfoEntity> list = Collections.singletonList(
                                entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now()));
                when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenAnswer(inv -> {
                                        throw new RuntimeException("unexpected");
                                });

                // 実行・確認（sendPush 側 catch の最終枝）
                assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
        }

        // ========== operationPostMessage（private） ==========

        /**
         * クラス：SendPushServiceImpl operationPostMessage 正常にpostMessageされることを確認するテストケース
         */
        @Test
        void operationPostMessage_01() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "operationPostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(mock(NotificationOutcome.class));

                try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                                        .thenAnswer(inv -> "MSG:" + inv.getArgument(0));

                        // 実行
                        m.invoke(service, req, header, d);

                        // 確認（postMessage が 1 回呼ばれる）
                        verify(notificationHubUtil, times(1))
                                        .postMessage(anyString(), anyString(), anyString(), anyString());
                }
        }

        // ========== executePostMessage（private） ==========

        /** クラス：SendPushServiceImpl executePostMessage 1回で成功することを確認するテストケース */
        @Test
        void executePostMessage_01() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(mock(NotificationOutcome.class));

                // 実行
                Object outcome = m.invoke(service, req, header, d);

                // 確認
                assertNotNull(outcome);
                verify(notificationHubUtil, times(1))
                                .postMessage(anyString(), anyString(), anyString(), anyString());
        }

        /** クラス：SendPushServiceImpl executePostMessage 一時エラー後に成功することを確認するテストケース */
        @Test
        void executePostMessage_02() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);

                NotificationHubsException ex = mock(NotificationHubsException.class);
                when(ex.isTransient()).thenReturn(true);
                when(ex.httpStatusCode()).thenReturn(500);

                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenThrow(ex).thenReturn(mock(NotificationOutcome.class));

                // 実行
                Object outcome = m.invoke(service, req, header, d);

                // 確認
                assertNotNull(outcome);
                verify(notificationHubUtil, times(2))
                                .postMessage(anyString(), anyString(), anyString(), anyString());
        }

        /**
         * クラス：SendPushServiceImpl executePostMessage
         * 一時エラーが上限到達しTscNotificationHubsExceptionとなることを確認するテストケース
         */
        /**
         * クラス：SendPushServiceImpl executePostMessage
         * 一時エラーが上限到達しTscNotificationHubsExceptionとなることを確認するテストケース（堅牢版）
         */
        @Test
        void executePostMessage_03() throws Exception {
                // 準備：retryCount を 2（または実装既定値）に設定
                Field f = SendPushServiceImpl.class.getDeclaredField("retryCount");
                f.setAccessible(true);
                int raw = f.getInt(service);
                if (raw <= 0) {
                        f.setInt(service, 2);
                        raw = 2;
                }
                final int attempts = raw;

                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());

                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);

                // Transient 例外のモック
                final NotificationHubsException ex = mock(NotificationHubsException.class);
                when(ex.isTransient()).thenReturn(true);
                when(ex.httpStatusCode()).thenReturn(503);

                // attempts 回だけ例外を投げる（その後は成功にしない：上限到達で必ず throw）
                final int[] counter = { 0 };
                doAnswer(inv -> {
                        if (counter[0]++ < attempts)
                                throw ex;
                        // 実装は上限に達した時点で throw 済みなのでここに来ない想定だが、安全のため：
                        return mock(NotificationOutcome.class);
                }).when(notificationHubUtil).postMessage(anyString(), anyString(), anyString(), anyString());

                // CommonUtil の複数オーバーロードをスタブ（4/6/7/9引数）
                try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                                        .thenReturn("MSG");
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any()))
                                        .thenReturn("MSG");
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any(), any()))
                                        .thenReturn("MSG");
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any(), any(),
                                        any(), any()))
                                        .thenReturn("MSG");

                        // 実行＆確認：InvocationTargetException 経由で TscNotificationHubsException が原因
                        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                                        () -> m.invoke(service, req, header, d));
                        assertInstanceOf(TscNotificationHubsException.class, ite.getCause());

                        // 呼び出し回数確認：上限 attempts 回
                        verify(notificationHubUtil, times(attempts)).postMessage(anyString(), anyString(), anyString(),
                                        anyString());
                }
        }

        /**
         * クラス：SendPushServiceImpl executePostMessage
         * 非一時エラーで即TscNotificationHubsExceptionとなることを確認するテストケース
         */
        @Test
        void executePostMessage_04() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);

                NotificationHubsException ex = mock(NotificationHubsException.class);
                when(ex.isTransient()).thenReturn(false);
                when(ex.httpStatusCode()).thenReturn(500);

                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenThrow(ex);

                // 実行・確認
                InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                                () -> m.invoke(service, req, header, d));
                assertTrue(ite.getCause() instanceof TscNotificationHubsException);
        }

        /** クラス：SendPushServiceImpl executePostMessage 一般例外が再throwされることを確認するテストケース */
        @Test
        void executePostMessage_05() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);

                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenAnswer(inv -> {
                                        throw new RuntimeException("unexpected");
                                });

                // 実行・確認
                InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                                () -> m.invoke(service, req, header, d));
                assertTrue(ite.getCause() instanceof RuntimeException);
        }

        /**
         * クラス：SendPushServiceImpl executePostMessage
         * retryCount=0でwhile不成立（0回転）となることを確認するテストケース
         */
        @Test
        void executePostMessage_06() throws Exception {
                // 準備（retryCount=0）
                Field f = SendPushServiceImpl.class.getDeclaredField("retryCount");
                f.setAccessible(true);
                f.setInt(service, 0);

                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);

                // 実行
                Object outcome = m.invoke(service, req, header, d);

                // 確認（ループに入らず null で終了、APIは呼ばれない）
                assertNull(outcome);
                verify(notificationHubUtil, times(0))
                                .postMessage(anyString(), anyString(), anyString(), anyString());
        }

        // ========== getAllDeviceData / getLastData（private） ==========

        /** クラス：SendPushServiceImpl getAllDeviceData Repository呼出結果が返ることを確認するテストケース */
        @Test
        void getAllDeviceData_01() throws Exception {
                // 準備
                List<NtfInfoEntity> list = Collections.singletonList(
                                entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now()));
                when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "getAllDeviceData", String.class);
                m.setAccessible(true);

                // 実行
                List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(service, "U1");

                // 確認
                assertEquals(1, result.size());
        }

        /** クラス：SendPushServiceImpl getLastData 最新データが取得されることを確認するテストケース */
        @Test
        void getLastData_01() throws Exception {
                // 準備
                List<NtfInfoEntity> list = Arrays.asList(
                                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now().minusDays(2)),
                                entity("U1", "i-2", "tok2", "d2", "1", "1", LocalDateTime.now().minusDays(1)),
                                entity("U1", "i-3", "tok3", "d3", "1", "1", LocalDateTime.now()));
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "getLastData", List.class);
                m.setAccessible(true);

                // 実行
                NtfInfoEntity last = (NtfInfoEntity) m.invoke(service, list);

                // 確認
                assertNotNull(last);
                assertEquals("i-3", last.getInstallationId());
        }

        /** クラス：SendPushServiceImpl getLastData 空リストでnullが返ることを確認するテストケース */
        @Test
        void getLastData_02() throws Exception {
                // 準備
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "getLastData", List.class);
                m.setAccessible(true);

                // 実行
                NtfInfoEntity last = (NtfInfoEntity) m.invoke(service, Collections.emptyList());

                // 確認
                assertNull(last);
        }

        // ========== validate / validateRequired（private） ==========

        /**
         * クラス：SendPushServiceImpl validate
         * 必須未入力でTscApplicationExceptionとなることを確認するテストケース
         */
        @Test
        void validate_01() throws Exception {
                // 準備
                SendPushRequestDto req = new SendPushRequestDto(); // 全部null
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "validate", SendPushRequestDto.class, RequestHeaderDto.class);
                m.setAccessible(true);

                // 実行・確認
                InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                                () -> m.invoke(service, req, header));
                assertTrue(ite.getCause() instanceof TscApplicationException);
        }

        /** クラス：SendPushServiceImpl validate 正常時にnullが返ることを確認するテストケース */
        @Test
        void validate_02() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "validate", SendPushRequestDto.class, RequestHeaderDto.class);
                m.setAccessible(true);

                // 実行
                Object result = m.invoke(service, req, header);

                // 確認
                assertNull(result);
        }

        /**
         * クラス：SendPushServiceImpl validateRequired
         * request==nullで"requestBody"が返ることを確認するテストケース
         */
        @Test
        void validateRequired_01() throws Exception {
                // 準備
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);

                // 実行
                String result = (String) m.invoke(service, new Object[] { null });

                // 確認
                assertEquals("requestBody", result);
        }

        /** クラス：SendPushServiceImpl validateRequired 複数項目欠落時に両方検出されることを確認するテストケース */
        @Test
        void validateRequired_02() throws Exception {
                // 準備
                SendPushRequestDto req = new SendPushRequestDto();
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);

                // 実行
                String result = (String) m.invoke(service, req);

                // 確認
                assertTrue(result.contains("internalUserId"));
                assertTrue(result.contains("body"));
        }

        /**
         * クラス：SendPushServiceImpl validateRequired internalUserId==null の検出を確認するテストケース
         */
        @Test
        void validateRequired_03() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                req.setInternalUserId(null);
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);

                // 実行
                String result = (String) m.invoke(service, req);

                // 確認
                assertNotNull(result);
                assertTrue(result.contains("internalUserId"));
        }

        /** クラス：SendPushServiceImpl validateRequired body==null の検出を確認するテストケース */
        @Test
        void validateRequired_04() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                req.setBody(null);
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);

                // 実行
                String result = (String) m.invoke(service, req);

                // 確認
                assertNotNull(result);
                assertTrue(result.contains("body"));
        }

        /** クラス：SendPushServiceImpl validateRequired 正常（欠落なし）でnullが返ることを確認するテストケース */
        @Test
        void validateRequired_05() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);

                // 実行
                String result = (String) m.invoke(service, req);

                // 確認
                assertNull(result);
        }

        /**
         * クラス：SendPushServiceImpl validateRequired
         * bodyが空文字の場合に欠落項目として検知されることを確認するテストケース
         */
        @Test
        void validateRequired_06() throws Exception {
                // 準備
                SendPushRequestDto req = baseRequest();
                req.setBody(""); // ★ isEmpty() 側の分岐到達

                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);

                // 実行
                String result = (String) m.invoke(service, req);

                // 確認
                assertNotNull(result);
                assertTrue(result.contains("body"));
        }

}
