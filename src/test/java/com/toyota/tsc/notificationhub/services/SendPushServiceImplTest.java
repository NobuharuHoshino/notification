
package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
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
import java.sql.SQLTransientException;
import java.sql.SQLNonTransientException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * クラス：SendPushServiceImpl 分岐網羅100%を目指すテストクラス
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
                Field f = SendPushServiceImpl.class.getDeclaredField("retryCount");
                f.setAccessible(true);
                f.setInt(service, 2);
        }

        private SendPushRequestDto baseRequest() {
                SendPushRequestDto req = new SendPushRequestDto();
                req.setInternalUserId("U1");
                req.setBody(Map.of("msg", "hello")); // bodyはMap
                return req;
        }

        private NtfInfoEntity entity(String user, String inst, String token, String dvc, String brd, String pf,
                        LocalDateTime ts) {
                return new NtfInfoEntity(user, inst, token, dvc, brd, pf, ts, ts);
        }

        // ========= sendPush =========

        @Test
        void sendPush_01() throws Exception {
                SendPushRequestDto req = baseRequest();
                List<NtfInfoEntity> list = Arrays.asList(
                                entity("U1", "i-1", "tok-old", "d-old", "1", "1", LocalDateTime.now().minusDays(1)),
                                entity("U1", "i-2", "tok-new", "d-new", "1", "1", LocalDateTime.now()));
                when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);
                when(notificationHubUtil.buildFcmV1Payload(anyMap())).thenReturn("PAYLOAD");
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(mock(NotificationOutcome.class));

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                PrintStream orig = System.out;
                System.setOut(new PrintStream(out));
                try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any()))
                                        .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                                        .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
                        cm.when(() -> CommonUtil.getResultCode("SUCCESS")).thenReturn("SUCCESS_CODE");

                        String code = service.sendPush(req, header).getResultCode();
                        assertEquals("SUCCESS_CODE", code);
                        assertTrue(out.toString().contains("RS07I00002"));
                } finally {
                        System.setOut(orig);
                }
        }

        @Test
        void sendPush_02() {
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

                        assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
                }
        }

        /** SQLTransientException→CustomSqlException（操作系として扱う） */
        @Test
        void sendPush_03() {
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
                        // ★ 操作系判定をtrueにする（これがないとCustomExceptionに落ちる）
                        st.when(() -> ExtractSqlExceptionUtil.isSqlOperationError(any(SQLException.class)))
                                        .thenReturn(true);

                        assertThrows(CustomSqlException.class, () -> service.sendPush(req, header));
                }
        }

        /** SQLNonTransientException→CustomSqlException（操作系として扱う） */
        @Test
        void sendPush_04() {
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
                        // ★ 操作系判定をtrueにする
                        st.when(() -> ExtractSqlExceptionUtil.isSqlOperationError(any(SQLException.class)))
                                        .thenReturn(true);

                        assertThrows(CustomSqlException.class, () -> service.sendPush(req, header));
                }
        }

        @Test
        void sendPush_05() {
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

                        assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
                }
        }

        @Test
        void sendPush_06() {
                SendPushRequestDto req = new SendPushRequestDto();
                req.setInternalUserId("");
                req.setBody(Map.of("msg", "any"));
                assertThrows(TscApplicationException.class, () -> service.sendPush(req, header));
        }

        @Test
        void sendPush_07() throws NotificationHubsException {
                SendPushRequestDto req = baseRequest();
                List<NtfInfoEntity> list = Collections.singletonList(
                                entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now()));
                when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);
                when(notificationHubUtil.buildFcmV1Payload(anyMap())).thenReturn("PAYLOAD");

                NotificationHubsException ex = mock(NotificationHubsException.class);
                when(ex.isTransient()).thenReturn(false);
                when(ex.httpStatusCode()).thenReturn(500);
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenThrow(ex);

                assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
        }

        @Test
        void sendPush_08() throws NotificationHubsException {
                SendPushRequestDto req = baseRequest();
                List<NtfInfoEntity> list = Collections.singletonList(
                                entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now()));
                when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);
                when(notificationHubUtil.buildFcmV1Payload(anyMap())).thenReturn("PAYLOAD");
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenAnswer(inv -> {
                                        throw new RuntimeException("unexpected");
                                });

                assertThrows(RuntimeException.class, () -> service.sendPush(req, header));
        }

        // ========= operationPostMessage（private） =========

        @Test
        void operationPostMessage_01() throws Exception {
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "operationPostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);
                when(notificationHubUtil.buildFcmV1Payload(anyMap())).thenReturn("PAYLOAD");
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(mock(NotificationOutcome.class));

                try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any()))
                                        .thenAnswer(inv -> "MSG:" + inv.getArgument(0));
                        m.invoke(service, req, header, d);
                        verify(notificationHubUtil, times(1))
                                        .postMessage(anyString(), anyString(), anyString(), anyString());
                }
        }

        // ========= executePostMessage（private） =========

        @Test
        void executePostMessage_01() throws Exception {
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);
                when(notificationHubUtil.buildFcmV1Payload(anyMap())).thenReturn("PAYLOAD");
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenReturn(mock(NotificationOutcome.class));

                Object outcome = m.invoke(service, req, header, d);
                assertNotNull(outcome);
                verify(notificationHubUtil, times(1))
                                .postMessage(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        void executePostMessage_02() throws Exception {
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);
                NotificationHubsException ex = mock(NotificationHubsException.class);
                when(ex.isTransient()).thenReturn(true);
                when(ex.httpStatusCode()).thenReturn(500);

                when(notificationHubUtil.buildFcmV1Payload(anyMap())).thenReturn("PAYLOAD");
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenThrow(ex).thenReturn(mock(NotificationOutcome.class));

                Object outcome = m.invoke(service, req, header, d);
                assertNotNull(outcome);
                verify(notificationHubUtil, times(2))
                                .postMessage(anyString(), anyString(), anyString(), anyString());
        }

        @Test
        void executePostMessage_03() throws Exception {
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

                final NotificationHubsException ex = mock(NotificationHubsException.class);
                when(ex.isTransient()).thenReturn(true);
                when(ex.httpStatusCode()).thenReturn(503);

                when(notificationHubUtil.buildFcmV1Payload(anyMap())).thenReturn("PAYLOAD");
                final int[] counter = { 0 };
                doAnswer(inv -> {
                        if (counter[0]++ < attempts)
                                throw ex;
                        return mock(NotificationOutcome.class);
                }).when(notificationHubUtil).postMessage(anyString(), anyString(), anyString(), anyString());

                try (MockedStatic<CommonUtil> cm = Mockito.mockStatic(CommonUtil.class)) {
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any())).thenReturn("MSG");
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any()))
                                        .thenReturn("MSG");
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any(), any()))
                                        .thenReturn("MSG");
                        cm.when(() -> CommonUtil.getMessage(anyString(), any(), any(), any(), any(), any(), any(),
                                        any(), any())).thenReturn("MSG");

                        InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                                        () -> m.invoke(service, req, header, d));
                        assertInstanceOf(TscNotificationHubsException.class, ite.getCause());
                        verify(notificationHubUtil, times(attempts))
                                        .postMessage(anyString(), anyString(), anyString(), anyString());
                }
        }

        @Test
        void executePostMessage_04() throws Exception {
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);
                NotificationHubsException ex = mock(NotificationHubsException.class);
                when(ex.isTransient()).thenReturn(false);
                when(ex.httpStatusCode()).thenReturn(500);
                when(notificationHubUtil.buildFcmV1Payload(anyMap())).thenReturn("PAYLOAD");
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenThrow(ex);

                InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                                () -> m.invoke(service, req, header, d));
                assertTrue(ite.getCause() instanceof TscNotificationHubsException);
        }

        @Test
        void executePostMessage_05() throws Exception {
                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);
                when(notificationHubUtil.buildFcmV1Payload(anyMap())).thenReturn("PAYLOAD");
                when(notificationHubUtil.postMessage(anyString(), anyString(), anyString(), anyString()))
                                .thenAnswer(inv -> {
                                        throw new RuntimeException("unexpected");
                                });

                InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                                () -> m.invoke(service, req, header, d));
                assertTrue(ite.getCause() instanceof RuntimeException);
        }

        @Test
        void executePostMessage_06() throws Exception {
                Field f = SendPushServiceImpl.class.getDeclaredField("retryCount");
                f.setAccessible(true);
                f.setInt(service, 0);

                SendPushRequestDto req = baseRequest();
                NtfInfoEntity d = entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now());
                Method m = SendPushServiceImpl.class.getDeclaredMethod(
                                "executePostMessage", SendPushRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);
                Object outcome = m.invoke(service, req, header, d);
                assertNull(outcome);
                verify(notificationHubUtil, times(0))
                                .postMessage(anyString(), anyString(), anyString(), anyString());
        }

        // ========= getAllDeviceData / getLastData（private） =========

        @Test
        void getAllDeviceData_01() throws Exception {
                List<NtfInfoEntity> list = Collections.singletonList(
                                entity("U1", "i-1", "tok", "d", "1", "1", LocalDateTime.now()));
                when(ntfInfoRepository.selectAllByInternalUserId("U1")).thenReturn(list);
                Method m = SendPushServiceImpl.class.getDeclaredMethod("getAllDeviceData", String.class);
                m.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(service, "U1");
                assertEquals(1, result.size());
        }

        @Test
        void getLastData_01() throws Exception {
                List<NtfInfoEntity> list = Arrays.asList(
                                entity("U1", "i-1", "tok1", "d1", "1", "1", LocalDateTime.now().minusDays(2)),
                                entity("U1", "i-2", "tok2", "d2", "1", "1", LocalDateTime.now().minusDays(1)),
                                entity("U1", "i-3", "tok3", "d3", "1", "1", LocalDateTime.now()));
                Method m = SendPushServiceImpl.class.getDeclaredMethod("getLastData", List.class);
                m.setAccessible(true);
                NtfInfoEntity last = (NtfInfoEntity) m.invoke(service, list);
                assertNotNull(last);
                assertEquals("i-3", last.getInstallationId());
        }

        @Test
        void getLastData_02() throws Exception {
                Method m = SendPushServiceImpl.class.getDeclaredMethod("getLastData", List.class);
                m.setAccessible(true);
                NtfInfoEntity last = (NtfInfoEntity) m.invoke(service, Collections.emptyList());
                assertNull(last);
        }

        // ========= validate / validateRequired（private） =========

        @Test
        void validate_01() throws Exception {
                SendPushRequestDto req = new SendPushRequestDto();
                Method m = SendPushServiceImpl.class.getDeclaredMethod("validate", SendPushRequestDto.class,
                                RequestHeaderDto.class);
                m.setAccessible(true);
                InvocationTargetException ite = assertThrows(InvocationTargetException.class,
                                () -> m.invoke(service, req, header));
                assertTrue(ite.getCause() instanceof TscApplicationException);
        }

        @Test
        void validate_02() throws Exception {
                SendPushRequestDto req = baseRequest();
                Method m = SendPushServiceImpl.class.getDeclaredMethod("validate", SendPushRequestDto.class,
                                RequestHeaderDto.class);
                m.setAccessible(true);
                Object result = m.invoke(service, req, header);
                assertNull(result);
        }

        @Test
        void validateRequired_01() throws Exception {
                Method m = SendPushServiceImpl.class.getDeclaredMethod("validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);
                String result = (String) m.invoke(service, new Object[] { null });
                assertEquals("requestBody", result);
        }

        @Test
        void validateRequired_02() throws Exception {
                SendPushRequestDto req = new SendPushRequestDto();
                Method m = SendPushServiceImpl.class.getDeclaredMethod("validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);
                String result = (String) m.invoke(service, req);
                assertTrue(result.contains("internalUserId"));
                assertTrue(result.contains("body"));
        }

        @Test
        void validateRequired_03() throws Exception {
                SendPushRequestDto req = baseRequest();
                req.setInternalUserId(null);
                Method m = SendPushServiceImpl.class.getDeclaredMethod("validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);
                String result = (String) m.invoke(service, req);
                assertNotNull(result);
                assertTrue(result.contains("internalUserId"));
        }

        @Test
        void validateRequired_04() throws Exception {
                SendPushRequestDto req = baseRequest();
                req.setBody(null);
                Method m = SendPushServiceImpl.class.getDeclaredMethod("validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);
                String result = (String) m.invoke(service, req);
                assertNotNull(result);
                assertTrue(result.contains("body"));
        }

        @Test
        void validateRequired_05() throws Exception {
                SendPushRequestDto req = baseRequest();
                Method m = SendPushServiceImpl.class.getDeclaredMethod("validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);
                String result = (String) m.invoke(service, req);
                assertNull(result);
        }

        @Test
        void validateRequired_06() throws Exception {
                SendPushRequestDto req = baseRequest();
                req.setBody(Collections.emptyMap());
                Method m = SendPushServiceImpl.class.getDeclaredMethod("validateRequired", SendPushRequestDto.class);
                m.setAccessible(true);
                String result = (String) m.invoke(service, req);
                assertNotNull(result);
                assertTrue(result.contains("body"));
        }
}
