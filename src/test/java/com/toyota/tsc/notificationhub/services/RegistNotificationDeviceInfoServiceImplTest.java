
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/services/RegistNotificationDeviceInfoServiceImplTest.java
package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
import com.windowsazure.messaging.NotificationHubsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.SQLException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RegistNotificationDeviceInfoServiceImpl のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class RegistNotificationDeviceInfoServiceImplTest {

        @Mock
        private NtfInfoRepositoryIF ntfInfoRepository;

        @Mock
        private NotificationHubUtil notificationHubUtil;

        @Mock
        private PropertiesUtil propertiesUtil;

        private static RegistNotificationDeviceInfoRequestDto req(
                        String internalUserId, String platform, String deviceToken, String dvcId, String brdCd) {
                RegistNotificationDeviceInfoRequestDto r = mock(RegistNotificationDeviceInfoRequestDto.class);
                when(r.getInternalUserId()).thenReturn(internalUserId);
                when(r.getPlatform()).thenReturn(platform);
                when(r.getDeviceToken()).thenReturn(deviceToken);
                when(r.getDvcId()).thenReturn(dvcId);
                when(r.getBrdCd()).thenReturn(brdCd);
                return r;
        }

        private static RequestHeaderDto header(String correlationId) {
                RequestHeaderDto h = mock(RequestHeaderDto.class);
                when(h.getCorrelationId()).thenReturn(correlationId);
                return h;
        }

        private static NtfInfoEntity entity(String internalUserId, String installationId, String deviceToken,
                        LocalDateTime updatedAt) {
                NtfInfoEntity e = mock(NtfInfoEntity.class);
                when(e.getInternalUserId()).thenReturn(internalUserId);
                when(e.getInstallationId()).thenReturn(installationId);
                when(e.getDeviceToken()).thenReturn(deviceToken);
                when(e.getUpdatedAt()).thenReturn(updatedAt);
                return e;
        }

        @Test
        void registDeviceInfo_001() throws NotificationHubsException {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                // ★ここがポイント：このテストで使うのは deviceToken だけ
                NtfInfoEntity e = mock(NtfInfoEntity.class);
                when(e.getDeviceToken()).thenReturn("tok");

                when(ntfInfoRepository.selectAllByInternalUserId("u"))
                                .thenReturn(List.of(e));

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(org.mockito.Mockito.any()))
                                        .thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(org.mockito.Mockito.anyString(),
                                        org.mockito.Mockito.<Object[]>any()))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(org.mockito.Mockito.anyString()))
                                        .thenReturn("RC_OK");

                        // Act
                        ResponseDto resp = sut.registDeviceInfo(request, header);

                        // Assert
                        assertNotNull(resp);
                        verify(notificationHubUtil, never()).deleteInstallation(anyString(), anyString());
                        verify(notificationHubUtil, never()).upsertInstallation(anyString(), anyString(), anyString(),
                                        anyString(), anyString());
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * デバイストークン重複がない場合にInstallation処理が実行され成功することを確認するテストケース
         */
        @Test
        void registDeviceInfo_002() throws NotificationHubsException {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(propertiesUtil.getRetryCount()).thenReturn(1);
                when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of());
                when(ntfInfoRepository.upsert(any())).thenReturn(1);

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        ResponseDto resp = sut.registDeviceInfo(request, header);

                        // Assert
                        assertNotNull(resp);
                        verify(ntfInfoRepository, times(1)).upsert(any());
                        verify(notificationHubUtil, times(1)).upsertInstallation(anyString(), eq("1"), eq("u"), eq("1"),
                                        eq("tok"));
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 必須項目不足の場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_003() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = req(null, "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        TscApplicationException ex = assertThrows(TscApplicationException.class,
                                        () -> sut.registDeviceInfo(request, header));

                        // Assert
                        assertNotNull(ex);
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 不正ブランドの場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_004() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "9");
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        TscApplicationException ex = assertThrows(TscApplicationException.class,
                                        () -> sut.registDeviceInfo(request, header));

                        // Assert
                        assertNotNull(ex);
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 不正プラットフォームの場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_005() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "9", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        TscApplicationException ex = assertThrows(TscApplicationException.class,
                                        () -> sut.registDeviceInfo(request, header));

                        // Assert
                        assertNotNull(ex);
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
         * Transient例外がリトライ後に成功することを確認するテストケース
         */
        @Test
        void executeDeleteInstallation_001() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(2);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                // ★このメソッドで使うgetterだけstub（UnnecessaryStubbing回避）
                RegistNotificationDeviceInfoRequestDto request = mock(RegistNotificationDeviceInfoRequestDto.class);
                when(request.getBrdCd()).thenReturn("1");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("cid");

                NtfInfoEntity entity = mock(NtfInfoEntity.class);
                when(entity.getInstallationId()).thenReturn("iid1");

                NotificationHubsException transientEx = mock(NotificationHubsException.class);
                when(transientEx.isTransient()).thenReturn(true);
                when(transientEx.httpStatusCode()).thenReturn(500);

                doThrow(transientEx).doNothing()
                                .when(notificationHubUtil).deleteInstallation(eq("iid1"), eq("1"));

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeDeleteInstallation", RegistNotificationDeviceInfoRequestDto.class,
                                RequestHeaderDto.class, NtfInfoEntity.class);
                m.setAccessible(true);

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.getMessage(anyString(),
                                        org.mockito.ArgumentMatchers.<Object[]>any()))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

                        // Act
                        m.invoke(sut, request, header, entity);

                        // Assert
                        verify(notificationHubUtil, times(2)).deleteInstallation(eq("iid1"), eq("1"));
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
         * Transient例外が回数超過でTscNotificationHubsExceptionになることを確認するテストケース
         */
        @Test
        void executeDeleteInstallation_002() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(1);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                // ★このメソッドで使うgetterだけstub（UnnecessaryStubbing回避）
                RegistNotificationDeviceInfoRequestDto request = mock(RegistNotificationDeviceInfoRequestDto.class);
                when(request.getBrdCd()).thenReturn("1");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("cid");

                NtfInfoEntity entity = mock(NtfInfoEntity.class);
                when(entity.getInstallationId()).thenReturn("iid1");

                NotificationHubsException transientEx = mock(NotificationHubsException.class);
                when(transientEx.isTransient()).thenReturn(true);
                when(transientEx.httpStatusCode()).thenReturn(500);

                doThrow(transientEx).when(notificationHubUtil).deleteInstallation(eq("iid1"), eq("1"));

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeDeleteInstallation", RegistNotificationDeviceInfoRequestDto.class,
                                RequestHeaderDto.class, NtfInfoEntity.class);
                m.setAccessible(true);

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.getMessage(anyString(),
                                        org.mockito.ArgumentMatchers.<Object[]>any()))
                                        .thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act + Assert（Reflection例外のcauseを投げ直す）
                        assertThrows(TscNotificationHubsException.class, () -> {
                                try {
                                        m.invoke(sut, request, header, entity);
                                } catch (Exception ex) {
                                        throw (RuntimeException) ex.getCause();
                                }
                        });

                        // Assert
                        verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid1"), eq("1"));
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
         * NonTransient例外でTscNotificationHubsExceptionになることを確認するテストケース
         */
        @Test
        void executeUpsertInstallation_001() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(1);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                NotificationHubsException nonTransient = mock(NotificationHubsException.class);
                when(nonTransient.isTransient()).thenReturn(false);
                when(nonTransient.httpStatusCode()).thenReturn(400);

                doThrow(nonTransient).when(notificationHubUtil)
                                .upsertInstallation(anyString(), eq("1"), eq("u"), eq("1"), eq("tok"));

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeUpsertInstallation", RegistNotificationDeviceInfoRequestDto.class,
                                RequestHeaderDto.class, String.class);
                m.setAccessible(true);

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(TscNotificationHubsException.class, () -> {
                                try {
                                        m.invoke(sut, request, header, "iidNew");
                                } catch (Exception ex) {
                                        throw (RuntimeException) ex.getCause();
                                }
                        });

                        // Assert
                        verify(notificationHubUtil, times(1))
                                        .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * SQL接続系例外の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_006() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(ntfInfoRepository.selectAllByInternalUserId("u"))
                                .thenThrow(new RuntimeException(new SQLException("x", "08S01")));

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(ntfInfoRepository, times(1)).selectAllByInternalUserId("u");
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * SQL操作系例外の場合にCustomSqlExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_007() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(ntfInfoRepository.selectAllByInternalUserId("u"))
                                .thenThrow(new RuntimeException(new SQLException("x", "23505")));

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(CustomSqlException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(ntfInfoRepository, times(1)).selectAllByInternalUserId("u");
                }
        }

        // -------- private method smoke tests (Reflection) --------

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl isValidPlatform
         * 有効値の場合にtrueとなることを確認するテストケース
         */
        @Test
        void isValidPlatform_001() throws Exception {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);
                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("isValidPlatform",
                                String.class);
                m.setAccessible(true);

                // Act
                boolean actual = (boolean) m.invoke(sut, "1");

                // Assert
                assertTrue(actual);
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl isValidBrdCd
         * 不正値の場合にfalseとなることを確認するテストケース
         */
        @Test
        void isValidBrdCd_001() throws Exception {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);
                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("isValidBrdCd",
                                String.class);
                m.setAccessible(true);

                // Act
                boolean actual = (boolean) m.invoke(sut, "9");

                // Assert
                assertFalse(actual);
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * リクエストがnullの場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_008() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);
                RegistNotificationDeviceInfoRequestDto request = null;
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        TscApplicationException ex = assertThrows(TscApplicationException.class,
                                        () -> sut.registDeviceInfo(request, header));

                        // Assert
                        assertNotNull(ex);
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 必須項目が空文字の場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_009() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);
                RegistNotificationDeviceInfoRequestDto request = req("", "1", "tok", "dvc", "1"); // internalUserIdが空文字
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        TscApplicationException ex = assertThrows(TscApplicationException.class,
                                        () -> sut.registDeviceInfo(request, header));

                        // Assert
                        assertNotNull(ex);
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * SQLExceptionが存在するが接続/操作どちらでもない場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_010() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(ntfInfoRepository.selectAllByInternalUserId("u"))
                                .thenThrow(new RuntimeException(new SQLException("x", "99999"))); // connection/operation
                                                                                                  // どちらにも該当しにくい

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(ntfInfoRepository, times(1)).selectAllByInternalUserId("u");
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * SQLExceptionが存在しない場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_011() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(ntfInfoRepository.selectAllByInternalUserId("u"))
                                .thenThrow(new RuntimeException("boom"));

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(ntfInfoRepository, times(1)).selectAllByInternalUserId("u");
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * deviceListが3件以上の場合に古いデータ削除が実行され成功することを確認するテストケース
         */
        @Test
        void registDeviceInfo_012() throws NotificationHubsException {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(propertiesUtil.getRetryCount()).thenReturn(1);
                when(ntfInfoRepository.upsert(any())).thenReturn(1);

                LocalDateTime now = LocalDateTime.now();

                // ★ entity() ヘルパを使わず、必要なgetterだけstub（UnnecessaryStubbing回避）
                NtfInfoEntity e1 = mock(NtfInfoEntity.class);
                when(e1.getInstallationId()).thenReturn("iid_new");
                when(e1.getDeviceToken()).thenReturn("tokA");
                when(e1.getUpdatedAt()).thenReturn(now);

                NtfInfoEntity e2 = mock(NtfInfoEntity.class);
                when(e2.getInstallationId()).thenReturn("iid_mid");
                when(e2.getDeviceToken()).thenReturn("tokB");
                when(e2.getUpdatedAt()).thenReturn(now.minusMinutes(1));

                NtfInfoEntity e3 = mock(NtfInfoEntity.class);
                when(e3.getInstallationId()).thenReturn("iid_old");
                when(e3.getDeviceToken()).thenReturn("tokC");
                when(e3.getUpdatedAt()).thenReturn(now.minusMinutes(2));
                // ★ deleteTarget でのみ必要
                when(e3.getInternalUserId()).thenReturn("u");

                when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of(e1, e2, e3));
                when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(1);

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        ResponseDto resp = sut.registDeviceInfo(request, header);

                        // Assert
                        assertNotNull(resp);
                        verify(ntfInfoRepository, times(1)).delete("u", "iid_old");
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * upsert件数が0の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_013() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of());
                when(ntfInfoRepository.upsert(any())).thenReturn(0); // ← upsertCount==0 分岐
                // ★ propertiesUtil.getRetryCount() はこの経路で呼ばれないのでstubしない

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(ntfInfoRepository, times(1)).upsert(any());
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * deviceListが3件以上かつ削除件数が0の場合にCustomExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_014() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);
                RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc", "1");
                RequestHeaderDto header = header("cid");

                when(propertiesUtil.getRetryCount()).thenReturn(1);
                when(ntfInfoRepository.upsert(any())).thenReturn(1);

                LocalDateTime now = LocalDateTime.now();

                // ★ entity() ヘルパを使わず、必要なgetterだけstub（UnnecessaryStubbing回避）
                NtfInfoEntity e1 = mock(NtfInfoEntity.class);
                when(e1.getInstallationId()).thenReturn("iid_new");
                when(e1.getDeviceToken()).thenReturn("tokA");
                when(e1.getUpdatedAt()).thenReturn(now);

                NtfInfoEntity e2 = mock(NtfInfoEntity.class);
                when(e2.getInstallationId()).thenReturn("iid_mid");
                when(e2.getDeviceToken()).thenReturn("tokB");
                when(e2.getUpdatedAt()).thenReturn(now.minusMinutes(1));

                NtfInfoEntity e3 = mock(NtfInfoEntity.class);
                when(e3.getInstallationId()).thenReturn("iid_old");
                when(e3.getDeviceToken()).thenReturn("tokC");
                when(e3.getUpdatedAt()).thenReturn(now.minusMinutes(2));
                // ★ deleteTarget でのみ必要
                when(e3.getInternalUserId()).thenReturn("u");

                when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of(e1, e2, e3));
                when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(0); // ← deleteCount==0 分岐

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(CustomException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(ntfInfoRepository, times(1)).delete("u", "iid_old");
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
         * NonTransient例外の場合にTscNotificationHubsExceptionになることを確認するテストケース
         */
        @Test
        void executeDeleteInstallation_003() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(1);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = mock(RegistNotificationDeviceInfoRequestDto.class);
                when(request.getBrdCd()).thenReturn("1");

                RequestHeaderDto header = mock(RequestHeaderDto.class);
                when(header.getCorrelationId()).thenReturn("cid");

                NtfInfoEntity entity = mock(NtfInfoEntity.class);
                when(entity.getInstallationId()).thenReturn("iid1");

                NotificationHubsException nonTransient = mock(NotificationHubsException.class);
                when(nonTransient.isTransient()).thenReturn(false);
                when(nonTransient.httpStatusCode()).thenReturn(400);
                doThrow(nonTransient).when(notificationHubUtil).deleteInstallation(eq("iid1"), eq("1"));

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeDeleteInstallation",
                                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act + Assert（Reflection例外のcauseを投げ直す）
                        assertThrows(TscNotificationHubsException.class, () -> {
                                try {
                                        m.invoke(sut, request, header, entity);
                                } catch (Exception ex) {
                                        throw (RuntimeException) ex.getCause();
                                }
                        });

                        // Assert
                        verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid1"), eq("1"));
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
         * Transient例外がリトライ後に成功することを確認するテストケース
         */
        @Test
        void executeUpsertInstallation_002() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(2);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                // ★ req() を使わず、当該経路で呼ばれるgetterだけstub（UnnecessaryStubbing回避）
                RegistNotificationDeviceInfoRequestDto request = mock(RegistNotificationDeviceInfoRequestDto.class);
                when(request.getBrdCd()).thenReturn("1");
                when(request.getInternalUserId()).thenReturn("u");
                when(request.getPlatform()).thenReturn("1");
                when(request.getDeviceToken()).thenReturn("tok");

                RequestHeaderDto header = header("cid");

                NotificationHubsException transientEx = mock(NotificationHubsException.class);
                when(transientEx.isTransient()).thenReturn(true);
                when(transientEx.httpStatusCode()).thenReturn(500);

                doThrow(transientEx).doNothing()
                                .when(notificationHubUtil)
                                .upsertInstallation(anyString(), eq("1"), eq("u"), eq("1"), eq("tok"));

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeUpsertInstallation",
                                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, String.class);
                m.setAccessible(true);

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");

                        // Act
                        m.invoke(sut, request, header, "iidNew");

                        // Assert
                        verify(notificationHubUtil, times(2))
                                        .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * NotificationHub更新でTscNotificationHubsExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_015() throws NotificationHubsException {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                // ★ platform=2, brdCd=2 にして OR の「第2条件 true」を通す狙いも兼ねる
                RegistNotificationDeviceInfoRequestDto request = req("u", "2", "tok", "dvc", "2");
                RequestHeaderDto header = header("cid");

                when(propertiesUtil.getRetryCount()).thenReturn(1);
                when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of());
                when(ntfInfoRepository.upsert(any())).thenReturn(1);

                NotificationHubsException nonTransient = mock(NotificationHubsException.class);
                when(nonTransient.isTransient()).thenReturn(false);
                when(nonTransient.httpStatusCode()).thenReturn(400);

                doThrow(nonTransient).when(notificationHubUtil)
                                .upsertInstallation(anyString(), eq("2"), eq("u"), eq("2"), eq("tok"));

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        assertThrows(TscNotificationHubsException.class, () -> sut.registDeviceInfo(request, header));

                        // Assert
                        verify(notificationHubUtil, times(1))
                                        .upsertInstallation(anyString(), eq("2"), eq("u"), eq("2"), eq("tok"));
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 必須項目がnull(プラットフォーム/デバイストークン/dvcId/brdCd)の場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_016() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                // internalUserIdだけ正常、他をnullにして「== null」側のOR分岐を踏む
                RegistNotificationDeviceInfoRequestDto request = req("u", null, null, null, null);
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        TscApplicationException ex = assertThrows(TscApplicationException.class,
                                        () -> sut.registDeviceInfo(request, header));

                        // Assert
                        assertNotNull(ex);
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
         * 必須項目が空文字(プラットフォーム/デバイストークン/dvcId/brdCd)の場合にTscApplicationExceptionが送出されることを確認するテストケース
         */
        @Test
        void registDeviceInfo_017() {
                // Arrange
                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                // internalUserIdだけ正常、他を空文字にして「isEmpty()」側のOR分岐を踏む
                RegistNotificationDeviceInfoRequestDto request = req("u", "", "", "", "");
                RequestHeaderDto header = header("cid");

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act
                        TscApplicationException ex = assertThrows(TscApplicationException.class,
                                        () -> sut.registDeviceInfo(request, header));

                        // Assert
                        assertNotNull(ex);
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
         * NotificationHubsException以外の例外が発生した場合に例外がそのまま送出されることを確認するテストケース
         */
        @Test
        void executeDeleteInstallation_004() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(1);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = mock(RegistNotificationDeviceInfoRequestDto.class);
                when(request.getBrdCd()).thenReturn("1"); // deleteInstallationの引数一致用

                // ★ RuntimeException経路では header.getCorrelationId() は参照されないためスタブしない
                RequestHeaderDto header = mock(RequestHeaderDto.class);

                NtfInfoEntity entity = mock(NtfInfoEntity.class);
                when(entity.getInstallationId()).thenReturn("iid1");

                doThrow(new RuntimeException("boom"))
                                .when(notificationHubUtil).deleteInstallation(eq("iid1"), eq("1"));

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeDeleteInstallation",
                                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);

                // Act + Assert（Reflection例外のcauseを投げ直す）
                assertThrows(RuntimeException.class, () -> {
                        try {
                                m.invoke(sut, request, header, entity);
                        } catch (Exception ex) {
                                throw (RuntimeException) ex.getCause();
                        }
                });

                // Assert
                verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid1"), eq("1"));
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
         * NotificationHubsException以外の例外が発生した場合に例外がそのまま送出されることを確認するテストケース
         */
        @Test
        void executeUpsertInstallation_003() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(1);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                // upsertInstallationの引数に使うgetterだけstub
                RegistNotificationDeviceInfoRequestDto request = mock(RegistNotificationDeviceInfoRequestDto.class);
                when(request.getBrdCd()).thenReturn("1");
                when(request.getInternalUserId()).thenReturn("u");
                when(request.getPlatform()).thenReturn("1");
                when(request.getDeviceToken()).thenReturn("tok");

                // ★ RuntimeException経路では header.getCorrelationId() は参照されないためスタブしない
                RequestHeaderDto header = mock(RequestHeaderDto.class);

                doThrow(new RuntimeException("boom"))
                                .when(notificationHubUtil)
                                .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeUpsertInstallation",
                                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, String.class);
                m.setAccessible(true);

                // Act + Assert（Reflection例外のcauseを投げ直す）
                assertThrows(RuntimeException.class, () -> {
                        try {
                                m.invoke(sut, request, header, "iidNew");
                        } catch (Exception ex) {
                                throw (RuntimeException) ex.getCause();
                        }
                });

                // Assert
                verify(notificationHubUtil, times(1))
                                .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
         * Transient例外が回数超過でTscNotificationHubsExceptionになることを確認するテストケース
         */
        @Test
        void executeUpsertInstallation_004() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(1);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                RegistNotificationDeviceInfoRequestDto request = mock(RegistNotificationDeviceInfoRequestDto.class);
                when(request.getBrdCd()).thenReturn("1");
                when(request.getInternalUserId()).thenReturn("u");
                when(request.getPlatform()).thenReturn("1");
                when(request.getDeviceToken()).thenReturn("tok");
                when(request.getDvcId()).thenReturn("dvc"); // errorログ引数で参照される

                RequestHeaderDto header = header("cid");

                NotificationHubsException transientEx = mock(NotificationHubsException.class);
                when(transientEx.isTransient()).thenReturn(true);
                when(transientEx.httpStatusCode()).thenReturn(500);

                doThrow(transientEx).when(notificationHubUtil)
                                .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeUpsertInstallation",
                                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, String.class);
                m.setAccessible(true);

                try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
                        common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class))).thenReturn("msg");
                        common.when(() -> CommonUtil.getResultCode(anyString()))
                                        .thenAnswer(inv -> "RC_" + inv.getArgument(0));

                        // Act + Assert（Reflection例外のcauseを投げ直す）
                        assertThrows(TscNotificationHubsException.class, () -> {
                                try {
                                        m.invoke(sut, request, header, "iidNew");
                                } catch (Exception ex) {
                                        throw (RuntimeException) ex.getCause();
                                }
                        });

                        // Assert
                        verify(notificationHubUtil, times(1))
                                        .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));
                }
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
         * リトライ回数が0の場合に処理が実行されず外部呼び出しされないことを確認するテストケース
         */
        @Test
        void executeDeleteInstallation_005() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(0);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                // ★ ループに入らないため getter は呼ばれない。不要スタブ回避のため素のmockのみ
                RegistNotificationDeviceInfoRequestDto request = mock(RegistNotificationDeviceInfoRequestDto.class);
                RequestHeaderDto header = mock(RequestHeaderDto.class);
                NtfInfoEntity entity = mock(NtfInfoEntity.class);

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeDeleteInstallation",
                                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
                                NtfInfoEntity.class);
                m.setAccessible(true);

                // Act（例外なく終了するはず）
                assertDoesNotThrow(() -> m.invoke(sut, request, header, entity));

                // Assert（外部呼び出しは発生しない）
                verify(notificationHubUtil, never()).deleteInstallation(anyString(), anyString());
        }

        /**
         * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
         * リトライ回数が0の場合に処理が実行されず外部呼び出しされないことを確認するテストケース
         */
        @Test
        void executeUpsertInstallation_005() throws Exception {
                // Arrange
                when(propertiesUtil.getRetryCount()).thenReturn(0);

                RegistNotificationDeviceInfoServiceImpl sut = new RegistNotificationDeviceInfoServiceImpl(
                                ntfInfoRepository, notificationHubUtil, propertiesUtil);

                // ★ ループに入らないため getter は呼ばれない。不要スタブ回避のため素のmockのみ
                RegistNotificationDeviceInfoRequestDto request = mock(RegistNotificationDeviceInfoRequestDto.class);
                RequestHeaderDto header = mock(RequestHeaderDto.class);

                Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
                                "executeUpsertInstallation",
                                RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class, String.class);
                m.setAccessible(true);

                // Act（例外なく終了するはず）
                assertDoesNotThrow(() -> m.invoke(sut, request, header, "iidNew"));

                // Assert（外部呼び出しは発生しない）
                verify(notificationHubUtil, never()).upsertInstallation(anyString(), anyString(), anyString(),
                                anyString(), anyString());
        }

}
