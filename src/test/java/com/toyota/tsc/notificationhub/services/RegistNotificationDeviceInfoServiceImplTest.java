
// // ファイルパス:
// src/test/java/com/toyota/tsc/notificationhub/services/RegistNotificationDeviceInfoServiceImplTest.java
// package com.toyota.tsc.notificationhub.services;

// import com.toyota.tsc.notificationhub.commons.CommonUtil;
// import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
// import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
// import com.toyota.tsc.notificationhub.exceptions.CustomException;
// import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
// import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
// import
// com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
// import
// com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
// import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
// import com.toyota.tsc.notificationhub.models.ResponseDto;
// import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
// import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
// import com.windowsazure.messaging.NotificationHubsException;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.MockedStatic;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.test.context.ActiveProfiles;

// import java.sql.SQLException;
// import java.lang.reflect.Method;
// import java.time.LocalDateTime;
// import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// /**
// * RegistNotificationDeviceInfoServiceImpl のテストクラス
// */
// @SuppressWarnings("all")
// @ExtendWith(MockitoExtension.class)
// class RegistNotificationDeviceInfoServiceImplTest {

// @Mock
// private NtfInfoRepositoryIF ntfInfoRepository;

// @Mock
// private NotificationHubUtil notificationHubUtil;

// @Mock
// private PropertiesUtil propertiesUtil;

// private static RegistNotificationDeviceInfoRequestDto req(
// String internalUserId, String platform, String deviceToken, String dvcId,
// String brdCd) {
// RegistNotificationDeviceInfoRequestDto r =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(r.getInternalUserId()).thenReturn(internalUserId);
// when(r.getPlatform()).thenReturn(platform);
// when(r.getDeviceToken()).thenReturn(deviceToken);
// when(r.getDvcId()).thenReturn(dvcId);
// when(r.getBrdCd()).thenReturn(brdCd);
// return r;
// }

// private static RequestHeaderDto header(String correlationId) {
// RequestHeaderDto h = mock(RequestHeaderDto.class);
// when(h.getCorrelationId()).thenReturn(correlationId);
// return h;
// }

// private static NtfInfoEntity entity(String internalUserId, String
// installationId, String deviceToken,
// LocalDateTime updatedAt) {
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getInternalUserId()).thenReturn(internalUserId);
// when(e.getInstallationId()).thenReturn(installationId);
// when(e.getDeviceToken()).thenReturn(deviceToken);
// when(e.getUpdatedAt()).thenReturn(updatedAt);
// return e;
// }

// @Test
// void registDeviceInfo_001() throws NotificationHubsException {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// // ★ここがポイント：このテストで使うのは deviceToken だけ
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getDeviceToken()).thenReturn("tok");

// when(ntfInfoRepository.selectAllByInternalUserId("u"))
// .thenReturn(List.of(e));

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(org.mockito.Mockito.any()))
// .thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(org.mockito.Mockito.anyString(),
// org.mockito.Mockito.<Object[]>any()))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(org.mockito.Mockito.anyString()))
// .thenReturn("RC_OK");

// // Act
// ResponseDto resp = sut.registDeviceInfo(request, header);

// // Assert
// assertNotNull(resp);
// verify(notificationHubUtil, never()).deleteInstallation(anyString(),
// anyString());
// verify(notificationHubUtil, never()).upsertInstallation(anyString(),
// anyString(), anyString(),
// anyString(), anyString());
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * デバイストークン重複がない場合にInstallation処理が実行され成功することを確認するテストケース
// */
// @Test
// void registDeviceInfo_002() throws NotificationHubsException {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// when(propertiesUtil.getRetryCount()).thenReturn(1);
// when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of());
// when(ntfInfoRepository.upsert(any())).thenReturn(1);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// ResponseDto resp = sut.registDeviceInfo(request, header);

// // Assert
// assertNotNull(resp);
// verify(ntfInfoRepository, times(1)).upsert(any());
// verify(notificationHubUtil, times(1)).upsertInstallation(anyString(),
// eq("1"), eq("u"), eq("1"),
// eq("tok"));
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * 必須項目不足の場合にTscApplicationExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_003() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request = req(null, "1", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// TscApplicationException ex = assertThrows(TscApplicationException.class,
// () -> sut.registDeviceInfo(request, header));

// // Assert
// assertNotNull(ex);
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * 不正ブランドの場合にTscApplicationExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_004() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc",
// "9");
// RequestHeaderDto header = header("cid");

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// TscApplicationException ex = assertThrows(TscApplicationException.class,
// () -> sut.registDeviceInfo(request, header));

// // Assert
// assertNotNull(ex);
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * 不正プラットフォームの場合にTscApplicationExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_005() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request = req("u", "9", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// TscApplicationException ex = assertThrows(TscApplicationException.class,
// () -> sut.registDeviceInfo(request, header));

// // Assert
// assertNotNull(ex);
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
// * Transient例外がリトライ後に成功することを確認するテストケース
// */
// @Test
// void executeDeleteInstallation_001() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(2);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // ★このメソッドで使うgetterだけstub（UnnecessaryStubbing回避）
// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getBrdCd()).thenReturn("1");

// RequestHeaderDto header = mock(RequestHeaderDto.class);
// when(header.getCorrelationId()).thenReturn("cid");

// NtfInfoEntity entity = mock(NtfInfoEntity.class);
// when(entity.getInstallationId()).thenReturn("iid1");

// NotificationHubsException transientEx =
// mock(NotificationHubsException.class);
// when(transientEx.isTransient()).thenReturn(true);
// when(transientEx.httpStatusCode()).thenReturn(500);

// doThrow(transientEx).doNothing()
// .when(notificationHubUtil).deleteInstallation(eq("iid1"), eq("1"));

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeDeleteInstallation", RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class, NtfInfoEntity.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(),
// org.mockito.ArgumentMatchers.<Object[]>any()))
// .thenReturn("msg");
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");

// // Act
// m.invoke(sut, request, header, entity);

// // Assert
// verify(notificationHubUtil, times(2)).deleteInstallation(eq("iid1"),
// eq("1"));
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
// * Transient例外が回数超過でTscNotificationHubsExceptionになることを確認するテストケース
// */
// @Test
// void executeDeleteInstallation_002() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // ★このメソッドで使うgetterだけstub（UnnecessaryStubbing回避）
// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getBrdCd()).thenReturn("1");

// RequestHeaderDto header = mock(RequestHeaderDto.class);
// when(header.getCorrelationId()).thenReturn("cid");

// NtfInfoEntity entity = mock(NtfInfoEntity.class);
// when(entity.getInstallationId()).thenReturn("iid1");

// NotificationHubsException transientEx =
// mock(NotificationHubsException.class);
// when(transientEx.isTransient()).thenReturn(true);
// when(transientEx.httpStatusCode()).thenReturn(500);

// doThrow(transientEx).when(notificationHubUtil).deleteInstallation(eq("iid1"),
// eq("1"));

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeDeleteInstallation", RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class, NtfInfoEntity.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(),
// org.mockito.ArgumentMatchers.<Object[]>any()))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act + Assert（Reflection例外のcauseを投げ直す）
// assertThrows(TscNotificationHubsException.class, () -> {
// try {
// m.invoke(sut, request, header, entity);
// } catch (Exception ex) {
// throw (RuntimeException) ex.getCause();
// }
// });

// // Assert
// verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid1"),
// eq("1"));
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
// * NonTransient例外でTscNotificationHubsExceptionになることを確認するテストケース
// */
// @Test
// void executeUpsertInstallation_001() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// NotificationHubsException nonTransient =
// mock(NotificationHubsException.class);
// when(nonTransient.isTransient()).thenReturn(false);
// when(nonTransient.httpStatusCode()).thenReturn(400);

// doThrow(nonTransient).when(notificationHubUtil)
// .upsertInstallation(anyString(), eq("1"), eq("u"), eq("1"), eq("tok"));

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeUpsertInstallation", RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class, String.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// assertThrows(TscNotificationHubsException.class, () -> {
// try {
// m.invoke(sut, request, header, "iidNew");
// } catch (Exception ex) {
// throw (RuntimeException) ex.getCause();
// }
// });

// // Assert
// verify(notificationHubUtil, times(1))
// .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * SQL接続系例外の場合にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_006() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// when(ntfInfoRepository.selectAllByInternalUserId("u"))
// .thenThrow(new RuntimeException(new SQLException("x", "08S01")));

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// assertThrows(CustomException.class, () -> sut.registDeviceInfo(request,
// header));

// // Assert
// verify(ntfInfoRepository, times(1)).selectAllByInternalUserId("u");
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * SQL操作系例外の場合にCustomSqlExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_007() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// when(ntfInfoRepository.selectAllByInternalUserId("u"))
// .thenThrow(new RuntimeException(new SQLException("x", "23505")));

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// assertThrows(CustomSqlException.class, () -> sut.registDeviceInfo(request,
// header));

// // Assert
// verify(ntfInfoRepository, times(1)).selectAllByInternalUserId("u");
// }
// }

// // -------- private method smoke tests (Reflection) --------

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl isValidPlatform
// * 有効値の場合にtrueとなることを確認するテストケース
// */
// @Test
// void isValidPlatform_001() throws Exception {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);
// Method m =
// RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("isValidPlatform",
// String.class);
// m.setAccessible(true);

// // Act
// boolean actual = (boolean) m.invoke(sut, "1");

// // Assert
// assertTrue(actual);
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl isValidBrdCd
// * 不正値の場合にfalseとなることを確認するテストケース
// */
// @Test
// void isValidBrdCd_001() throws Exception {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);
// Method m =
// RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("isValidBrdCd",
// String.class);
// m.setAccessible(true);

// // Act
// boolean actual = (boolean) m.invoke(sut, "9");

// // Assert
// assertFalse(actual);
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * リクエストがnullの場合にTscApplicationExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_008() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);
// RegistNotificationDeviceInfoRequestDto request = null;
// RequestHeaderDto header = header("cid");

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// TscApplicationException ex = assertThrows(TscApplicationException.class,
// () -> sut.registDeviceInfo(request, header));

// // Assert
// assertNotNull(ex);
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * 必須項目が空文字の場合にTscApplicationExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_009() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);
// RegistNotificationDeviceInfoRequestDto request = req("", "1", "tok", "dvc",
// "1"); // internalUserIdが空文字
// RequestHeaderDto header = header("cid");

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// TscApplicationException ex = assertThrows(TscApplicationException.class,
// () -> sut.registDeviceInfo(request, header));

// // Assert
// assertNotNull(ex);
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * SQLExceptionが存在するが接続/操作どちらでもない場合にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_010() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);
// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// when(ntfInfoRepository.selectAllByInternalUserId("u"))
// .thenThrow(new RuntimeException(new SQLException("x", "99999"))); //
// connection/operation
// // どちらにも該当しにくい

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// assertThrows(CustomException.class, () -> sut.registDeviceInfo(request,
// header));

// // Assert
// verify(ntfInfoRepository, times(1)).selectAllByInternalUserId("u");
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * SQLExceptionが存在しない場合にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_011() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);
// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// when(ntfInfoRepository.selectAllByInternalUserId("u"))
// .thenThrow(new RuntimeException("boom"));

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// assertThrows(CustomException.class, () -> sut.registDeviceInfo(request,
// header));

// // Assert
// verify(ntfInfoRepository, times(1)).selectAllByInternalUserId("u");
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * deviceListが3件以上の場合に古いデータ削除が実行され成功することを確認するテストケース
// */
// // @Test
// // void registDeviceInfo_012() throws NotificationHubsException {
// // // Arrange
// // RegistNotificationDeviceInfoServiceImpl sut = new
// // RegistNotificationDeviceInfoServiceImpl(
// // ntfInfoRepository, notificationHubUtil, propertiesUtil);
// // RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok",
// "dvc",
// // "1");
// // RequestHeaderDto header = header("cid");

// // when(propertiesUtil.getRetryCount()).thenReturn(1);
// // when(ntfInfoRepository.upsert(any())).thenReturn(1);

// // LocalDateTime now = LocalDateTime.now();

// // // ★ entity() ヘルパを使わず、必要なgetterだけstub（UnnecessaryStubbing回避）
// // NtfInfoEntity e1 = mock(NtfInfoEntity.class);
// // when(e1.getInstallationId()).thenReturn("iid_new");
// // when(e1.getDeviceToken()).thenReturn("tokA");
// // when(e1.getUpdatedAt()).thenReturn(now);

// // NtfInfoEntity e2 = mock(NtfInfoEntity.class);
// // when(e2.getInstallationId()).thenReturn("iid_mid");
// // when(e2.getDeviceToken()).thenReturn("tokB");
// // when(e2.getUpdatedAt()).thenReturn(now.minusMinutes(1));

// // NtfInfoEntity e3 = mock(NtfInfoEntity.class);
// // when(e3.getInstallationId()).thenReturn("iid_old");
// // when(e3.getDeviceToken()).thenReturn("tokC");
// // when(e3.getUpdatedAt()).thenReturn(now.minusMinutes(2));
// // // ★ deleteTarget でのみ必要
// // when(e3.getInternalUserId()).thenReturn("u");

// //
// when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of(e1,
// // e2, e3));
// // when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(1);

// // try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// // common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// // common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// // .thenReturn("msg");
// // common.when(() -> CommonUtil.getResultCode(anyString()))
// // .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // // Act
// // ResponseDto resp = sut.registDeviceInfo(request, header);

// // // Assert
// // assertNotNull(resp);
// // verify(ntfInfoRepository, times(1)).delete("u", "iid_old");
// // }
// // }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * upsert件数が0の場合にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_013() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);
// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok", "dvc",
// "1");
// RequestHeaderDto header = header("cid");

// when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of());
// when(ntfInfoRepository.upsert(any())).thenReturn(0); // ← upsertCount==0 分岐
// // ★ propertiesUtil.getRetryCount() はこの経路で呼ばれないのでstubしない

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// assertThrows(CustomException.class, () -> sut.registDeviceInfo(request,
// header));

// // Assert
// verify(ntfInfoRepository, times(1)).upsert(any());
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * deviceListが3件以上かつ削除件数が0の場合にCustomExceptionが送出されることを確認するテストケース
// */
// // @Test
// // void registDeviceInfo_014() {
// // // Arrange
// // RegistNotificationDeviceInfoServiceImpl sut = new
// // RegistNotificationDeviceInfoServiceImpl(
// // ntfInfoRepository, notificationHubUtil, propertiesUtil);
// // RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tok",
// "dvc",
// // "1");
// // RequestHeaderDto header = header("cid");

// // when(propertiesUtil.getRetryCount()).thenReturn(1);
// // when(ntfInfoRepository.upsert(any())).thenReturn(1);

// // LocalDateTime now = LocalDateTime.now();

// // // ★ entity() ヘルパを使わず、必要なgetterだけstub（UnnecessaryStubbing回避）
// // NtfInfoEntity e1 = mock(NtfInfoEntity.class);
// // when(e1.getInstallationId()).thenReturn("iid_new");
// // when(e1.getDeviceToken()).thenReturn("tokA");
// // when(e1.getUpdatedAt()).thenReturn(now);

// // NtfInfoEntity e2 = mock(NtfInfoEntity.class);
// // when(e2.getInstallationId()).thenReturn("iid_mid");
// // when(e2.getDeviceToken()).thenReturn("tokB");
// // when(e2.getUpdatedAt()).thenReturn(now.minusMinutes(1));

// // NtfInfoEntity e3 = mock(NtfInfoEntity.class);
// // when(e3.getInstallationId()).thenReturn("iid_old");
// // when(e3.getDeviceToken()).thenReturn("tokC");
// // when(e3.getUpdatedAt()).thenReturn(now.minusMinutes(2));
// // // ★ deleteTarget でのみ必要
// // when(e3.getInternalUserId()).thenReturn("u");

// //
// when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of(e1,
// // e2, e3));
// // when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(0); // ←
// // deleteCount==0 分岐

// // try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// // common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// // common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// // .thenReturn("msg");
// // common.when(() -> CommonUtil.getResultCode(anyString()))
// // .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // // Act
// // assertThrows(CustomException.class, () -> sut.registDeviceInfo(request,
// // header));

// // // Assert
// // verify(ntfInfoRepository, times(1)).delete("u", "iid_old");
// // }
// // }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
// * NonTransient例外の場合にTscNotificationHubsExceptionになることを確認するテストケース
// */
// @Test
// void executeDeleteInstallation_003() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getBrdCd()).thenReturn("1");

// RequestHeaderDto header = mock(RequestHeaderDto.class);
// when(header.getCorrelationId()).thenReturn("cid");

// NtfInfoEntity entity = mock(NtfInfoEntity.class);
// when(entity.getInstallationId()).thenReturn("iid1");

// NotificationHubsException nonTransient =
// mock(NotificationHubsException.class);
// when(nonTransient.isTransient()).thenReturn(false);
// when(nonTransient.httpStatusCode()).thenReturn(400);
// doThrow(nonTransient).when(notificationHubUtil).deleteInstallation(eq("iid1"),
// eq("1"));

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeDeleteInstallation",
// RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
// NtfInfoEntity.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act + Assert（Reflection例外のcauseを投げ直す）
// assertThrows(TscNotificationHubsException.class, () -> {
// try {
// m.invoke(sut, request, header, entity);
// } catch (Exception ex) {
// throw (RuntimeException) ex.getCause();
// }
// });

// // Assert
// verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid1"),
// eq("1"));
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
// * Transient例外がリトライ後に成功することを確認するテストケース
// */
// @Test
// void executeUpsertInstallation_002() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(2);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // ★ req() を使わず、当該経路で呼ばれるgetterだけstub（UnnecessaryStubbing回避）
// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getBrdCd()).thenReturn("1");
// when(request.getInternalUserId()).thenReturn("u");
// when(request.getPlatform()).thenReturn("1");
// when(request.getDeviceToken()).thenReturn("tok");

// RequestHeaderDto header = header("cid");

// NotificationHubsException transientEx =
// mock(NotificationHubsException.class);
// when(transientEx.isTransient()).thenReturn(true);
// when(transientEx.httpStatusCode()).thenReturn(500);

// doThrow(transientEx).doNothing()
// .when(notificationHubUtil)
// .upsertInstallation(anyString(), eq("1"), eq("u"), eq("1"), eq("tok"));

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeUpsertInstallation",
// RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
// String.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");

// // Act
// m.invoke(sut, request, header, "iidNew");

// // Assert
// verify(notificationHubUtil, times(2))
// .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// * NotificationHub更新でTscNotificationHubsExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_015() throws NotificationHubsException {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // ★ platform=2, brdCd=2 にして OR の「第2条件 true」を通す狙いも兼ねる
// RegistNotificationDeviceInfoRequestDto request = req("u", "2", "tok", "dvc",
// "2");
// RequestHeaderDto header = header("cid");

// when(propertiesUtil.getRetryCount()).thenReturn(1);
// when(ntfInfoRepository.selectAllByInternalUserId("u")).thenReturn(List.of());
// when(ntfInfoRepository.upsert(any())).thenReturn(1);

// NotificationHubsException nonTransient =
// mock(NotificationHubsException.class);
// when(nonTransient.isTransient()).thenReturn(false);
// when(nonTransient.httpStatusCode()).thenReturn(400);

// doThrow(nonTransient).when(notificationHubUtil)
// .upsertInstallation(anyString(), eq("2"), eq("u"), eq("2"), eq("tok"));

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// assertThrows(TscNotificationHubsException.class, () ->
// sut.registDeviceInfo(request, header));

// // Assert
// verify(notificationHubUtil, times(1))
// .upsertInstallation(anyString(), eq("2"), eq("u"), eq("2"), eq("tok"));
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// *
// 必須項目がnull(プラットフォーム/デバイストークン/dvcId/brdCd)の場合にTscApplicationExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_016() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // internalUserIdだけ正常、他をnullにして「== null」側のOR分岐を踏む
// RegistNotificationDeviceInfoRequestDto request = req("u", null, null, null,
// null);
// RequestHeaderDto header = header("cid");

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// TscApplicationException ex = assertThrows(TscApplicationException.class,
// () -> sut.registDeviceInfo(request, header));

// // Assert
// assertNotNull(ex);
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl registDeviceInfo
// *
// 必須項目が空文字(プラットフォーム/デバイストークン/dvcId/brdCd)の場合にTscApplicationExceptionが送出されることを確認するテストケース
// */
// @Test
// void registDeviceInfo_017() {
// // Arrange
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // internalUserIdだけ正常、他を空文字にして「isEmpty()」側のOR分岐を踏む
// RegistNotificationDeviceInfoRequestDto request = req("u", "", "", "", "");
// RequestHeaderDto header = header("cid");

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.toJson(any())).thenReturn("{}");
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act
// TscApplicationException ex = assertThrows(TscApplicationException.class,
// () -> sut.registDeviceInfo(request, header));

// // Assert
// assertNotNull(ex);
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
// * NotificationHubsException以外の例外が発生した場合に例外がそのまま送出されることを確認するテストケース
// */
// @Test
// void executeDeleteInstallation_004() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getBrdCd()).thenReturn("1"); // deleteInstallationの引数一致用

// // ★ RuntimeException経路では header.getCorrelationId() は参照されないためスタブしない
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// NtfInfoEntity entity = mock(NtfInfoEntity.class);
// when(entity.getInstallationId()).thenReturn("iid1");

// doThrow(new RuntimeException("boom"))
// .when(notificationHubUtil).deleteInstallation(eq("iid1"), eq("1"));

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeDeleteInstallation",
// RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
// NtfInfoEntity.class);
// m.setAccessible(true);

// // Act + Assert（Reflection例外のcauseを投げ直す）
// assertThrows(RuntimeException.class, () -> {
// try {
// m.invoke(sut, request, header, entity);
// } catch (Exception ex) {
// throw (RuntimeException) ex.getCause();
// }
// });

// // Assert
// verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid1"),
// eq("1"));
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
// * NotificationHubsException以外の例外が発生した場合に例外がそのまま送出されることを確認するテストケース
// */
// @Test
// void executeUpsertInstallation_003() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // upsertInstallationの引数に使うgetterだけstub
// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getBrdCd()).thenReturn("1");
// when(request.getInternalUserId()).thenReturn("u");
// when(request.getPlatform()).thenReturn("1");
// when(request.getDeviceToken()).thenReturn("tok");

// // ★ RuntimeException経路では header.getCorrelationId() は参照されないためスタブしない
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// doThrow(new RuntimeException("boom"))
// .when(notificationHubUtil)
// .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeUpsertInstallation",
// RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
// String.class);
// m.setAccessible(true);

// // Act + Assert（Reflection例外のcauseを投げ直す）
// assertThrows(RuntimeException.class, () -> {
// try {
// m.invoke(sut, request, header, "iidNew");
// } catch (Exception ex) {
// throw (RuntimeException) ex.getCause();
// }
// });

// // Assert
// verify(notificationHubUtil, times(1))
// .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
// * Transient例外が回数超過でTscNotificationHubsExceptionになることを確認するテストケース
// */
// @Test
// void executeUpsertInstallation_004() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getBrdCd()).thenReturn("1");
// when(request.getInternalUserId()).thenReturn("u");
// when(request.getPlatform()).thenReturn("1");
// when(request.getDeviceToken()).thenReturn("tok");
// when(request.getDvcId()).thenReturn("dvc"); // errorログ引数で参照される

// RequestHeaderDto header = header("cid");

// NotificationHubsException transientEx =
// mock(NotificationHubsException.class);
// when(transientEx.isTransient()).thenReturn(true);
// when(transientEx.httpStatusCode()).thenReturn(500);

// doThrow(transientEx).when(notificationHubUtil)
// .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeUpsertInstallation",
// RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
// String.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(), any(Object[].class)))
// .thenReturn("msg");
// common.when(() -> CommonUtil.getResultCode(anyString()))
// .thenAnswer(inv -> "RC_" + inv.getArgument(0));

// // Act + Assert（Reflection例外のcauseを投げ直す）
// assertThrows(TscNotificationHubsException.class, () -> {
// try {
// m.invoke(sut, request, header, "iidNew");
// } catch (Exception ex) {
// throw (RuntimeException) ex.getCause();
// }
// });

// // Assert
// verify(notificationHubUtil, times(1))
// .upsertInstallation(eq("iidNew"), eq("1"), eq("u"), eq("1"), eq("tok"));
// }
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeDeleteInstallation
// * リトライ回数が0の場合に処理が実行されず外部呼び出しされないことを確認するテストケース
// */
// @Test
// void executeDeleteInstallation_005() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(0);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // ★ ループに入らないため getter は呼ばれない。不要スタブ回避のため素のmockのみ
// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);
// NtfInfoEntity entity = mock(NtfInfoEntity.class);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeDeleteInstallation",
// RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
// NtfInfoEntity.class);
// m.setAccessible(true);

// // Act（例外なく終了するはず）
// assertDoesNotThrow(() -> m.invoke(sut, request, header, entity));

// // Assert（外部呼び出しは発生しない）
// verify(notificationHubUtil, never()).deleteInstallation(anyString(),
// anyString());
// }

// /**
// * クラス：RegistNotificationDeviceInfoServiceImpl executeUpsertInstallation
// * リトライ回数が0の場合に処理が実行されず外部呼び出しされないことを確認するテストケース
// */
// @Test
// void executeUpsertInstallation_005() throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(0);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // ★ ループに入らないため getter は呼ばれない。不要スタブ回避のため素のmockのみ
// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "executeUpsertInstallation",
// RegistNotificationDeviceInfoRequestDto.class, RequestHeaderDto.class,
// String.class);
// m.setAccessible(true);

// // Act（例外なく終了するはず）
// assertDoesNotThrow(() -> m.invoke(sut, request, header, "iidNew"));

// // Assert（外部呼び出しは発生しない）
// verify(notificationHubUtil, never()).upsertInstallation(anyString(),
// anyString(), anyString(),
// anyString(), anyString());
// }

// //
// ============================================================================
// // FIX: UnnecessaryStubbingException を潰すための「最小 stub」ヘルパー＆修正版テスト群
// // ※ 末尾「}」の直前に貼り付け
// // ※ 既存の ntfMin を使った追加テスト（*_minStub など）はコメントアウト/削除してから実行
// //
// ============================================================================

// /** updatedAt だけ必要なケース用（並び替え比較に必要） */
// private static NtfInfoEntity ntfUpdatedAtOnly(LocalDateTime updatedAt) {
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getUpdatedAt()).thenReturn(updatedAt);
// return e;
// }

// /** getDeleteTargetList の戻り値検証等で installationId が必要なケース用 */
// private static NtfInfoEntity ntfIdAndUpdatedAt(String installationId,
// LocalDateTime updatedAt) {
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getInstallationId()).thenReturn(installationId);
// when(e.getUpdatedAt()).thenReturn(updatedAt);
// return e;
// }

// /** deleteDeviceData の delete 対象（最古1件）に必要な最小セット */
// private static NtfInfoEntity ntfDbDeleteTarget(String internalUserId, String
// installationId,
// LocalDateTime updatedAt) {
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getInternalUserId()).thenReturn(internalUserId);
// when(e.getInstallationId()).thenReturn(installationId);
// when(e.getUpdatedAt()).thenReturn(updatedAt);
// return e;
// }

// /** operationDeleteInstallation の削除対象（最新以外）に必要な最小セット */
// private static NtfInfoEntity ntfHubDeleteTarget(String installationId, String
// deviceToken,
// LocalDateTime updatedAt) {
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getInstallationId()).thenReturn(installationId);
// when(e.getDeviceToken()).thenReturn(deviceToken);
// when(e.getUpdatedAt()).thenReturn(updatedAt);
// return e;
// }

// /**
// * deleteDeviceData:
// * updatedAt 昇順 + limit(1) により「最古1件」だけ delete されることを検証
// * ※ neu（削除されない側）には updatedAt だけを stub（ここが重要）
// */
// @Test
// void deleteDeviceData_201_deletesOldestOne_noUnnecessaryStub() throws
// Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getInternalUserId()).thenReturn("u"); // ログ引数で使用
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)

// RequestHeaderDto header = mock(RequestHeaderDto.class);
// when(header.getCorrelationId()).thenReturn("cid-dbdel-201");

// LocalDateTime tOld = LocalDateTime.parse("2026-01-01T00:00:00");
// LocalDateTime tNew = LocalDateTime.parse("2026-01-02T00:00:00");

// // old(削除対象): internalUserId + installationId + updatedAt が必要
// NtfInfoEntity old = ntfDbDeleteTarget("u", "iid_old", tOld);
// // neu(削除されない): 並び替え比較のため updatedAt だけ必要（他は stub しない！）
// NtfInfoEntity neu = ntfUpdatedAtOnly(tNew);

// when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(1);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "deleteDeviceData",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(),
// any(Object[].class))).thenReturn("msg"); // ログ用
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)

// Object ret = m.invoke(sut, request, header, List.of(neu, old));

// assertEquals(1, ret);
// verify(ntfInfoRepository, times(1)).delete(eq("u"), eq("iid_old"));
// verify(ntfInfoRepository, never()).delete(eq("u"), eq("iid_new"));
// }
// }

// /**
// * getLastData:
// * sorted((a,b)-> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
// が比較され、最新が返ることを検証
// * ※ old は updatedAt だけで良い（installationId を stub すると未使用になり得る）
// */
// @Test
// void getLastData_201_sortedComparator_returnsNewest_noUnnecessaryStub()
// throws Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// LocalDateTime tOld = LocalDateTime.parse("2026-01-01T00:00:00");
// LocalDateTime tNew = LocalDateTime.parse("2026-01-02T00:00:00");

// NtfInfoEntity old = ntfUpdatedAtOnly(tOld);
// // 返ってくる方（neu）だけ installationId を検証したいので stub
// NtfInfoEntity neu = ntfIdAndUpdatedAt("iid_new", tNew);

// Method m =
// RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("getLastData",
// List.class);
// m.setAccessible(true);

// Object result = m.invoke(sut, List.of(old, neu));

// assertTrue(result instanceof NtfInfoEntity);
// assertEquals("iid_new", ((NtfInfoEntity) result).getInstallationId());
// }

// /**
// * getDeleteTargetList:
// * updatedAt 降順で並べた先頭(最新)を除いたリストが返る
// * ※ newest は updatedAt だけで良い（installationId は未使用になり得る）
// */
// @Test
// void getDeleteTargetList_201_returnsAllExceptNewest_noUnnecessaryStub()
// throws Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// LocalDateTime t1 = LocalDateTime.parse("2026-01-03T00:00:00"); // newest
// LocalDateTime t2 = LocalDateTime.parse("2026-01-02T00:00:00"); // mid
// LocalDateTime t3 = LocalDateTime.parse("2026-01-01T00:00:00"); // old

// NtfInfoEntity newest = ntfUpdatedAtOnly(t1);
// NtfInfoEntity mid = ntfIdAndUpdatedAt("iid_mid", t2);
// NtfInfoEntity old = ntfIdAndUpdatedAt("iid_old", t3);

// Method m =
// RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("getDeleteTargetList",
// List.class);
// m.setAccessible(true);

// @SuppressWarnings("unchecked")
// List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(sut, List.of(mid,
// newest, old));

// assertEquals(2, result.size());
// assertEquals("iid_mid", result.get(0).getInstallationId());
// assertEquals("iid_old", result.get(1).getInstallationId());
// }

// /**
// * getDeleteTargetList:
// * nullsLast(...).reversed() の経路を踏むため updatedAt=null を含む
// * ※ 検証はサイズだけでOK（installationId は不要stubになるので入れない）
// */
// @Test
// void getDeleteTargetList_202_includesNullUpdatedAt_noUnnecessaryStub() throws
// Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// NtfInfoEntity hasTime =
// ntfUpdatedAtOnly(LocalDateTime.parse("2026-01-01T00:00:00"));
// NtfInfoEntity nullTime = ntfUpdatedAtOnly(null);

// Method m =
// RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("getDeleteTargetList",
// List.class);
// m.setAccessible(true);

// @SuppressWarnings("unchecked")
// List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(sut,
// List.of(hasTime, nullTime));

// assertEquals(1, result.size());
// }

// /**
// * operationDeleteInstallation:
// * size < 2 の場合は if に入らない（stub 不要）
// */
// @Test
// void operationDeleteInstallation_201_size1_noDelete_noUnnecessaryStub()
// throws Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// // getter は一切呼ばれないので “素のmock” でOK
// NtfInfoEntity only = mock(NtfInfoEntity.class);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "operationDeleteInstallation",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// m.invoke(sut, request, header, List.of(only));

// verify(notificationHubUtil, never()).deleteInstallation(anyString(),
// anyString());
// }

// /**
// * operationDeleteInstallation:
// * size>=2 の場合、最新以外を deleteInstallation（3件→2件削除）
// * ※ latest は updatedAt だけでOK（installationId/deviceToken は未使用になり得る）
// */
// @Test
// void
// operationDeleteInstallation_202_size3_deleteTwoOldInstallations_noUnnecessaryStub()
// throws Exception {
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getDeviceToken()).thenReturn("tok");
// when(request.getBrdCd()).thenReturn("1");
// when(request.getInternalUserId()).thenReturn("u");

// RequestHeaderDto header = mock(RequestHeaderDto.class);
// when(header.getCorrelationId()).thenReturn("cid-del-202");

// LocalDateTime t1 = LocalDateTime.parse("2026-01-03T00:00:00"); // latest
// LocalDateTime t2 = LocalDateTime.parse("2026-01-02T00:00:00"); // mid
// LocalDateTime t3 = LocalDateTime.parse("2026-01-01T00:00:00"); // old

// NtfInfoEntity latest = ntfUpdatedAtOnly(t1);
// NtfInfoEntity mid = ntfHubDeleteTarget("iid_mid", "tok_mid", t2);
// NtfInfoEntity old = ntfHubDeleteTarget("iid_old", "tok_old", t3);

// doNothing().when(notificationHubUtil).deleteInstallation(anyString(),
// anyString());

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "operationDeleteInstallation",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(),
// any(Object[].class))).thenReturn("msg");
// common.when(() -> CommonUtil.getBrd(anyString())).thenReturn("BRD");

// m.invoke(sut, request, header, List.of(latest, mid, old));

// verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid_mid"),
// eq("1"));
// verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid_old"),
// eq("1"));
// verify(notificationHubUtil, times(2)).deleteInstallation(anyString(),
// eq("1"));
// }
// }

// /**
// * execRegistNotificationInfo:
// * size>=2 分岐まで通して deleteInstallation + deleteDeviceData を両方実行
// * ※ latest は updatedAt だけ、old は必要分だけ stub
// */
// @Test
// void
// execRegistNotificationInfo_201_deviceListSize2_deletePathExecuted_noUnnecessaryStub()
// throws Exception {
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getInternalUserId()).thenReturn("u");
// when(request.getPlatform()).thenReturn("1");
// when(request.getDeviceToken()).thenReturn("tokNew");
// when(request.getBrdCd()).thenReturn("1");
// // request.getDvcId は成功経路では不要（エラー時ログ用）
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)

// RequestHeaderDto header = mock(RequestHeaderDto.class);
// when(header.getCorrelationId()).thenReturn("cid-exec-201");

// LocalDateTime tOld = LocalDateTime.parse("2026-01-01T00:00:00");
// LocalDateTime tNew = LocalDateTime.parse("2026-01-02T00:00:00");

// // latest: updatedAt だけ
// NtfInfoEntity latest = ntfUpdatedAtOnly(tNew);

// // old: deleteInstallation の対象＆deleteDeviceData の対象になり得るので必要最小限を stub
// NtfInfoEntity old = mock(NtfInfoEntity.class);
// when(old.getUpdatedAt()).thenReturn(tOld);
// when(old.getInstallationId()).thenReturn("iid_old");
// when(old.getDeviceToken()).thenReturn("tok_old");
// when(old.getInternalUserId()).thenReturn("u");

// when(ntfInfoRepository.upsert(any())).thenReturn(1);
// doNothing().when(notificationHubUtil).deleteInstallation(anyString(),
// anyString());
// doNothing().when(notificationHubUtil)
// .upsertInstallation(anyString(), anyString(), anyString(), anyString(),
// anyString());

// // deleteDeviceData は最古(iid_old)を削除
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(1);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "execRegistNotificationInfo",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(),
// any(Object[].class))).thenReturn("msg");
// common.when(() -> CommonUtil.getBrd(anyString())).thenReturn("BRD");
// common.when(() -> CommonUtil.getPlt(anyString())).thenReturn("PLT");

// m.invoke(sut, request, header, List.of(latest, old));

// verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid_old"),
// eq("1"));
// verify(ntfInfoRepository, times(1)).delete(eq("u"), eq("iid_old"));
// }
// }

// /**
// * execRegistNotificationInfo:
// * deviceList.size() >= 2 かつ deleteDeviceData の deleteCount == 0 の場合に
// * throw new CustomException() となることを検証
// *
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// *
// * ※UnnecessaryStubbingException回避のため、NtfInfoEntityは「実際に呼ばれるgetterだけ」stubする
// */
// @Test
// void execRegistNotificationInfo_301_deleteCountZero_throwCustomException()
// throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(1); // delete/upsert
// installation の while に入るため
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // request はこの経路で
// getInternalUserId/getPlatform/getDeviceToken/getDvcId/getBrdCd
// // が使われるため req() でOK
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)[2](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tokNew",
// "dvc", "1");
// RequestHeaderDto header = header("cid-delcnt0");

// // deviceList は size>=2 を満たす
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// LocalDateTime tOld = LocalDateTime.parse("2026-01-01T00:00:00");
// LocalDateTime tNew = LocalDateTime.parse("2026-01-02T00:00:00");

// // newest: ソート比較に必要な updatedAt だけ stub（他は不要＝stubしない）
// NtfInfoEntity newest = mock(NtfInfoEntity.class);
// when(newest.getUpdatedAt()).thenReturn(tNew);

// // oldest: operationDeleteInstallation と deleteDeviceData 両方で参照される getter だけ
// // stub
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// NtfInfoEntity oldest = mock(NtfInfoEntity.class);
// when(oldest.getUpdatedAt()).thenReturn(tOld);
// when(oldest.getInstallationId()).thenReturn("iid_old"); // deleteInstallation
// / deleteDeviceData で必要
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// when(oldest.getDeviceToken()).thenReturn("tok_old"); //
// operationDeleteInstallation のログで必要
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// when(oldest.getInternalUserId()).thenReturn("u"); // deleteDeviceData の
// delete 引数で必要
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)

// // upsert
// //
// は成功させる（0だと別分岐で落ちる）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// when(ntfInfoRepository.upsert(any())).thenReturn(1);

// // Installation API
// //
// は成功させる（例外系に行かない）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// doNothing().when(notificationHubUtil).deleteInstallation(anyString(),
// anyString());
// doNothing().when(notificationHubUtil)
// .upsertInstallation(anyString(), anyString(), anyString(), anyString(),
// anyString());

// // ★deleteDeviceData の deleteCount を 0 にする（delete が 0
// //
// を返す）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(0);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "execRegistNotificationInfo",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// // この経路で呼ばれる static を最小限
// //
// stub（不要stub禁止）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// common.when(() -> CommonUtil.getMessage(anyString(),
// any(Object[].class))).thenReturn("msg");
// common.when(() -> CommonUtil.getBrd(anyString())).thenReturn("BRD");
// common.when(() -> CommonUtil.getPlt(anyString())).thenReturn("PLT");

// // Act + Assert（Reflection の cause を投げ直す）
// assertThrows(CustomException.class, () -> {
// try {
// m.invoke(sut, request, header, List.of(newest, oldest));
// } catch (Exception ex) {
// Throwable c = ex.getCause();
// if (c instanceof RuntimeException)
// throw (RuntimeException) c;
// throw new RuntimeException(c);
// }
// });

// // delete
// //
// は実行されている（その結果が0だったので例外になる）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// verify(ntfInfoRepository, times(1)).delete(eq("u"), eq("iid_old"));
// }
// }
// //
// ============================================================================
// // FIX: UnnecessaryStubbingException を潰すための「最小 stub」ヘルパー＆修正版テスト群
// // ※ 末尾「}」の直前に貼り付け
// // ※ 既存の ntfMin を使った追加テスト（*_minStub など）はコメントアウト/削除してから実行
// //
// ============================================================================

// /** updatedAt だけ必要なケース用（並び替え比較に必要） */
// private static NtfInfoEntity ntfUpdatedAtOnly(LocalDateTime updatedAt) {
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getUpdatedAt()).thenReturn(updatedAt);
// return e;
// }

// /** getDeleteTargetList の戻り値検証等で installationId が必要なケース用 */
// private static NtfInfoEntity ntfIdAndUpdatedAt(String installationId,
// LocalDateTime updatedAt) {
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getInstallationId()).thenReturn(installationId);
// when(e.getUpdatedAt()).thenReturn(updatedAt);
// return e;
// }

// /** deleteDeviceData の delete 対象（最古1件）に必要な最小セット */
// private static NtfInfoEntity ntfDbDeleteTarget(String internalUserId, String
// installationId,
// LocalDateTime updatedAt) {
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getInternalUserId()).thenReturn(internalUserId);
// when(e.getInstallationId()).thenReturn(installationId);
// when(e.getUpdatedAt()).thenReturn(updatedAt);
// return e;
// }

// /** operationDeleteInstallation の削除対象（最新以外）に必要な最小セット */
// private static NtfInfoEntity ntfHubDeleteTarget(String installationId, String
// deviceToken,
// LocalDateTime updatedAt) {
// NtfInfoEntity e = mock(NtfInfoEntity.class);
// when(e.getInstallationId()).thenReturn(installationId);
// when(e.getDeviceToken()).thenReturn(deviceToken);
// when(e.getUpdatedAt()).thenReturn(updatedAt);
// return e;
// }

// /**
// * deleteDeviceData:
// * updatedAt 昇順 + limit(1) により「最古1件」だけ delete されることを検証
// * ※ neu（削除されない側）には updatedAt だけを stub（ここが重要）
// */
// @Test
// void deleteDeviceData_201_deletesOldestOne_noUnnecessaryStub() throws
// Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getInternalUserId()).thenReturn("u"); // ログ引数で使用
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)

// RequestHeaderDto header = mock(RequestHeaderDto.class);
// when(header.getCorrelationId()).thenReturn("cid-dbdel-201");

// LocalDateTime tOld = LocalDateTime.parse("2026-01-01T00:00:00");
// LocalDateTime tNew = LocalDateTime.parse("2026-01-02T00:00:00");

// // old(削除対象): internalUserId + installationId + updatedAt が必要
// NtfInfoEntity old = ntfDbDeleteTarget("u", "iid_old", tOld);
// // neu(削除されない): 並び替え比較のため updatedAt だけ必要（他は stub しない！）
// NtfInfoEntity neu = ntfUpdatedAtOnly(tNew);

// when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(1);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "deleteDeviceData",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(),
// any(Object[].class))).thenReturn("msg"); // ログ用
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)

// Object ret = m.invoke(sut, request, header, List.of(neu, old));

// assertEquals(1, ret);
// verify(ntfInfoRepository, times(1)).delete(eq("u"), eq("iid_old"));
// verify(ntfInfoRepository, never()).delete(eq("u"), eq("iid_new"));
// }
// }

// /**
// * getLastData:
// * sorted((a,b)-> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
// が比較され、最新が返ることを検証
// * ※ old は updatedAt だけで良い（installationId を stub すると未使用になり得る）
// */
// @Test
// void getLastData_201_sortedComparator_returnsNewest_noUnnecessaryStub()
// throws Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// LocalDateTime tOld = LocalDateTime.parse("2026-01-01T00:00:00");
// LocalDateTime tNew = LocalDateTime.parse("2026-01-02T00:00:00");

// NtfInfoEntity old = ntfUpdatedAtOnly(tOld);
// // 返ってくる方（neu）だけ installationId を検証したいので stub
// NtfInfoEntity neu = ntfIdAndUpdatedAt("iid_new", tNew);

// Method m =
// RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("getLastData",
// List.class);
// m.setAccessible(true);

// Object result = m.invoke(sut, List.of(old, neu));

// assertTrue(result instanceof NtfInfoEntity);
// assertEquals("iid_new", ((NtfInfoEntity) result).getInstallationId());
// }

// /**
// * getDeleteTargetList:
// * updatedAt 降順で並べた先頭(最新)を除いたリストが返る
// * ※ newest は updatedAt だけで良い（installationId は未使用になり得る）
// */
// @Test
// void getDeleteTargetList_201_returnsAllExceptNewest_noUnnecessaryStub()
// throws Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// LocalDateTime t1 = LocalDateTime.parse("2026-01-03T00:00:00"); // newest
// LocalDateTime t2 = LocalDateTime.parse("2026-01-02T00:00:00"); // mid
// LocalDateTime t3 = LocalDateTime.parse("2026-01-01T00:00:00"); // old

// NtfInfoEntity newest = ntfUpdatedAtOnly(t1);
// NtfInfoEntity mid = ntfIdAndUpdatedAt("iid_mid", t2);
// NtfInfoEntity old = ntfIdAndUpdatedAt("iid_old", t3);

// Method m =
// RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("getDeleteTargetList",
// List.class);
// m.setAccessible(true);

// @SuppressWarnings("unchecked")
// List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(sut, List.of(mid,
// newest, old));

// assertEquals(2, result.size());
// assertEquals("iid_mid", result.get(0).getInstallationId());
// assertEquals("iid_old", result.get(1).getInstallationId());
// }

// /**
// * getDeleteTargetList:
// * nullsLast(...).reversed() の経路を踏むため updatedAt=null を含む
// * ※ 検証はサイズだけでOK（installationId は不要stubになるので入れない）
// */
// @Test
// void getDeleteTargetList_202_includesNullUpdatedAt_noUnnecessaryStub() throws
// Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// NtfInfoEntity hasTime =
// ntfUpdatedAtOnly(LocalDateTime.parse("2026-01-01T00:00:00"));
// NtfInfoEntity nullTime = ntfUpdatedAtOnly(null);

// Method m =
// RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod("getDeleteTargetList",
// List.class);
// m.setAccessible(true);

// @SuppressWarnings("unchecked")
// List<NtfInfoEntity> result = (List<NtfInfoEntity>) m.invoke(sut,
// List.of(hasTime, nullTime));

// assertEquals(1, result.size());
// }

// /**
// * operationDeleteInstallation:
// * size < 2 の場合は if に入らない（stub 不要）
// */
// @Test
// void operationDeleteInstallation_201_size1_noDelete_noUnnecessaryStub()
// throws Exception {
// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// RequestHeaderDto header = mock(RequestHeaderDto.class);

// // getter は一切呼ばれないので “素のmock” でOK
// NtfInfoEntity only = mock(NtfInfoEntity.class);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "operationDeleteInstallation",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// m.invoke(sut, request, header, List.of(only));

// verify(notificationHubUtil, never()).deleteInstallation(anyString(),
// anyString());
// }

// /**
// * operationDeleteInstallation:
// * size>=2 の場合、最新以外を deleteInstallation（3件→2件削除）
// * ※ latest は updatedAt だけでOK（installationId/deviceToken は未使用になり得る）
// */
// @Test
// void
// operationDeleteInstallation_202_size3_deleteTwoOldInstallations_noUnnecessaryStub()
// throws Exception {
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getDeviceToken()).thenReturn("tok");
// when(request.getBrdCd()).thenReturn("1");
// when(request.getInternalUserId()).thenReturn("u");

// RequestHeaderDto header = mock(RequestHeaderDto.class);
// when(header.getCorrelationId()).thenReturn("cid-del-202");

// LocalDateTime t1 = LocalDateTime.parse("2026-01-03T00:00:00"); // latest
// LocalDateTime t2 = LocalDateTime.parse("2026-01-02T00:00:00"); // mid
// LocalDateTime t3 = LocalDateTime.parse("2026-01-01T00:00:00"); // old

// NtfInfoEntity latest = ntfUpdatedAtOnly(t1);
// NtfInfoEntity mid = ntfHubDeleteTarget("iid_mid", "tok_mid", t2);
// NtfInfoEntity old = ntfHubDeleteTarget("iid_old", "tok_old", t3);

// doNothing().when(notificationHubUtil).deleteInstallation(anyString(),
// anyString());

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "operationDeleteInstallation",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(),
// any(Object[].class))).thenReturn("msg");
// common.when(() -> CommonUtil.getBrd(anyString())).thenReturn("BRD");

// m.invoke(sut, request, header, List.of(latest, mid, old));

// verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid_mid"),
// eq("1"));
// verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid_old"),
// eq("1"));
// verify(notificationHubUtil, times(2)).deleteInstallation(anyString(),
// eq("1"));
// }
// }

// /**
// * execRegistNotificationInfo:
// * size>=2 分岐まで通して deleteInstallation + deleteDeviceData を両方実行
// * ※ latest は updatedAt だけ、old は必要分だけ stub
// */
// @Test
// void
// execRegistNotificationInfo_201_deviceListSize2_deletePathExecuted_noUnnecessaryStub()
// throws Exception {
// when(propertiesUtil.getRetryCount()).thenReturn(1);

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// RegistNotificationDeviceInfoRequestDto request =
// mock(RegistNotificationDeviceInfoRequestDto.class);
// when(request.getInternalUserId()).thenReturn("u");
// when(request.getPlatform()).thenReturn("1");
// when(request.getDeviceToken()).thenReturn("tokNew");
// when(request.getBrdCd()).thenReturn("1");

// RequestHeaderDto header = mock(RequestHeaderDto.class);
// when(header.getCorrelationId()).thenReturn("cid-exec-201");

// LocalDateTime tOld = LocalDateTime.parse("2026-01-01T00:00:00");
// LocalDateTime tNew = LocalDateTime.parse("2026-01-02T00:00:00");

// // latest: updatedAt だけ
// NtfInfoEntity latest = ntfUpdatedAtOnly(tNew);

// // old: deleteInstallation の対象＆deleteDeviceData の対象になり得るので必要最小限を stub
// NtfInfoEntity old = mock(NtfInfoEntity.class);
// when(old.getUpdatedAt()).thenReturn(tOld);
// when(old.getInstallationId()).thenReturn("iid_old");
// when(old.getDeviceToken()).thenReturn("tok_old");
// when(old.getInternalUserId()).thenReturn("u");

// when(ntfInfoRepository.upsert(any())).thenReturn(1);
// doNothing().when(notificationHubUtil).deleteInstallation(anyString(),
// anyString());
// doNothing().when(notificationHubUtil)
// .upsertInstallation(anyString(), anyString(), anyString(), anyString(),
// anyString());

// // deleteDeviceData は最古(iid_old)を削除
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(1);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "execRegistNotificationInfo",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// common.when(() -> CommonUtil.getMessage(anyString(),
// any(Object[].class))).thenReturn("msg");
// common.when(() -> CommonUtil.getBrd(anyString())).thenReturn("BRD");
// common.when(() -> CommonUtil.getPlt(anyString())).thenReturn("PLT");

// m.invoke(sut, request, header, List.of(latest, old));

// verify(notificationHubUtil, times(1)).deleteInstallation(eq("iid_old"),
// eq("1"));
// verify(ntfInfoRepository, times(1)).delete(eq("u"), eq("iid_old"));
// }
// }

// /**
// * execRegistNotificationInfo:
// * deviceList.size() >= 2 かつ deleteDeviceData の deleteCount == 0 の場合に
// * throw new CustomException() となることを検証
// *
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// *
// * ※UnnecessaryStubbingException回避のため、NtfInfoEntityは「実際に呼ばれるgetterだけ」stubする
// */
// @Test
// void execRegistNotificationInfo_301_deleteCountZero_throwCustomException()
// throws Exception {
// // Arrange
// when(propertiesUtil.getRetryCount()).thenReturn(1); // delete/upsert
// installation の while に入るため
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)

// RegistNotificationDeviceInfoServiceImpl sut = new
// RegistNotificationDeviceInfoServiceImpl(
// ntfInfoRepository, notificationHubUtil, propertiesUtil);

// // request はこの経路で
// getInternalUserId/getPlatform/getDeviceToken/getDvcId/getBrdCd
// // が使われるため req() でOK
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)[2](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// RegistNotificationDeviceInfoRequestDto request = req("u", "1", "tokNew",
// "dvc", "1");
// RequestHeaderDto header = header("cid-delcnt0");

// // deviceList は size>=2 を満たす
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// LocalDateTime tOld = LocalDateTime.parse("2026-01-01T00:00:00");
// LocalDateTime tNew = LocalDateTime.parse("2026-01-02T00:00:00");

// // newest: ソート比較に必要な updatedAt だけ stub（他は不要＝stubしない）
// NtfInfoEntity newest = mock(NtfInfoEntity.class);
// when(newest.getUpdatedAt()).thenReturn(tNew);

// // oldest: operationDeleteInstallation と deleteDeviceData 両方で参照される getter だけ
// // stub
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// NtfInfoEntity oldest = mock(NtfInfoEntity.class);
// when(oldest.getUpdatedAt()).thenReturn(tOld);
// when(oldest.getInstallationId()).thenReturn("iid_old"); // deleteInstallation
// / deleteDeviceData で必要
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// when(oldest.getDeviceToken()).thenReturn("tok_old"); //
// operationDeleteInstallation のログで必要
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// when(oldest.getInternalUserId()).thenReturn("u"); // deleteDeviceData の
// delete 引数で必要
// //
// [1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)

// // upsert
// //
// は成功させる（0だと別分岐で落ちる）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// when(ntfInfoRepository.upsert(any())).thenReturn(1);

// doNothing().when(notificationHubUtil).deleteInstallation(anyString(),
// anyString());
// doNothing().when(notificationHubUtil)
// .upsertInstallation(anyString(), anyString(), anyString(), anyString(),
// anyString());

// when(ntfInfoRepository.delete(eq("u"), eq("iid_old"))).thenReturn(0);

// Method m = RegistNotificationDeviceInfoServiceImpl.class.getDeclaredMethod(
// "execRegistNotificationInfo",
// RegistNotificationDeviceInfoRequestDto.class,
// RequestHeaderDto.class,
// List.class);
// m.setAccessible(true);

// try (MockedStatic<CommonUtil> common = mockStatic(CommonUtil.class)) {
// // この経路で呼ばれる static を最小限
// //
// stub（不要stub禁止）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// common.when(() -> CommonUtil.getMessage(anyString(),
// any(Object[].class))).thenReturn("msg");
// common.when(() -> CommonUtil.getBrd(anyString())).thenReturn("BRD");
// common.when(() -> CommonUtil.getPlt(anyString())).thenReturn("PLT");

// // Act + Assert（Reflection の cause を投げ直す）
// assertThrows(CustomException.class, () -> {
// try {
// m.invoke(sut, request, header, List.of(newest, oldest));
// } catch (Exception ex) {
// Throwable c = ex.getCause();
// if (c instanceof RuntimeException)
// throw (RuntimeException) c;
// throw new RuntimeException(c);
// }
// });

// // delete
// //
// は実行されている（その結果が0だったので例外になる）[1](https://nttdatajpprod-my.sharepoint.com/personal/nobuharu_hoshino_bp_jp_nttdata_com/Documents/Microsoft%20Copilot%20Chat%20%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB/RegistNotificationDeviceInfoServiceImpl.java)
// verify(ntfInfoRepository, times(1)).delete(eq("u"), eq("iid_old"));
// }
// }
// }
