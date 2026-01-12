
// // ファイルパス:
// src/test/java/com/toyota/tsc/notificationhub/commons/NotificationHubUtilTest.java
// package com.toyota.tsc.notificationhub.commons;

// import com.toyota.tsc.notificationhub.exceptions.CustomException;
// import com.windowsazure.messaging.*;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.MockedConstruction;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.lang.reflect.Method;
// import java.net.URLEncoder;
// import java.nio.charset.StandardCharsets;
// import java.util.Map;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// /**
// * NotificationHubUtil のテストクラス
// */
// @SuppressWarnings("all")
// @ExtendWith(MockitoExtension.class)
// class NotificationHubUtilTest {

// @Mock
// private PropertiesUtil propertiesUtil;

// /**
// * クラス：NotificationHubUtil generateSasToken
// * TOYOTAブランドの場合にSASトークンが生成されることを確認するテストケース
// */
// @Test
// void generateSasToken_001() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getRootUri()).thenReturn("https://%s/%s");
// when(propertiesUtil.getTtlSeconds()).thenReturn(60); // ★ 60L → 60
// when(propertiesUtil.getSasTemplate()).thenReturn("SharedAccessSignature
// sr=%s&sig=%s&se=%s&skn=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// String uri = String.format("https://%s/%s", "nsT", "hubT");
// String encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.name());

// // Act
// String token = sut.generateSasToken("1");

// // Assert
// assertNotNull(token);
// assertTrue(token.startsWith("SharedAccessSignature "));
// assertTrue(token.contains("sr=" + encodedUri));
// assertTrue(token.contains("skn=keyNameT"));
// }

// /**
// * クラス：NotificationHubUtil generateSasToken
// * LEXUSブランドの場合にSASトークンが生成されることを確認するテストケース
// */
// @Test
// void generateSasToken_002() {
// // Arrange
// when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
// when(propertiesUtil.getHubNameL()).thenReturn("hubL");
// when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
// when(propertiesUtil.getKeyL()).thenReturn("secret");
// when(propertiesUtil.getRootUri()).thenReturn("https://%s/%s");
// when(propertiesUtil.getTtlSeconds()).thenReturn(60); // ★ 60L → 60
// when(propertiesUtil.getSasTemplate()).thenReturn("SharedAccessSignature
// sr=%s&sig=%s&se=%s&skn=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// // Act
// String token = sut.generateSasToken("2");

// // Assert
// assertNotNull(token);
// assertTrue(token.contains("skn=keyNameL"));
// }

// /** クラス：NotificationHubUtil generateSasToken 不正ブランドの場合にnullが返ることを確認するテストケース
// */
// @Test
// void generateSasToken_003() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// // Act
// String token = sut.generateSasToken("9");

// // Assert
// assertNull(token);
// }

// /**
// * クラス：NotificationHubUtil generateSasToken
// * 例外発生時にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void generateSasToken_004() {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn(null); // ここでNPE相当を誘発
// when(propertiesUtil.getRootUri()).thenReturn("https://%s/%s");
// when(propertiesUtil.getTtlSeconds()).thenReturn(60);
// // ★ getSasTemplate() は例外で到達しないのでスタブしない（UnnecessaryStubbing回避）

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// // Act
// CustomException ex = assertThrows(CustomException.class, () ->
// sut.generateSasToken("1"));

// // Assert
// assertNotNull(ex.getCause());
// }

// /**
// * クラス：NotificationHubUtil upsertInstallation
// * TOYOTA/ANDROIDの場合にcreateOrUpdateInstallationが呼ばれることを確認するテストケース
// */
// @Test
// void upsertInstallation_001() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {

// // Act
// sut.upsertInstallation("iid", "1", "internal", "1", "deviceToken");

// // Assert
// assertEquals(1, mocked.constructed().size());
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub,
// times(1)).createOrUpdateInstallation(any(FcmV1Installation.class));
// }
// }

// /**
// * クラス：NotificationHubUtil upsertInstallation
// * TOYOTA/IOSの場合にcreateOrUpdateInstallationが呼ばれることを確認するテストケース
// */
// @Test
// void upsertInstallation_002() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {

// // Act
// sut.upsertInstallation("iid", "1", "internal", "2", "deviceToken");

// // Assert
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub,
// times(1)).createOrUpdateInstallation(any(AppleInstallation.class));
// }
// }

// /**
// * クラス：NotificationHubUtil upsertInstallation
// 不正ブランドの場合に外部呼び出しされないことを確認するテストケース
// */
// @Test
// void upsertInstallation_003() throws Exception {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {

// // Act
// sut.upsertInstallation("iid", "9", "internal", "1", "deviceToken");

// // Assert
// assertEquals(0, mocked.constructed().size());
// }
// }

// /**
// * クラス：NotificationHubUtil upsertInstallation
// * 不正プラットフォームの場合にcreateOrUpdateInstallationが呼ばれないことを確認するテストケース
// */
// @Test
// void upsertInstallation_004() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {

// // Act
// sut.upsertInstallation("iid", "1", "internal", "9", "deviceToken");

// // Assert
// assertEquals(1, mocked.constructed().size());
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub, never()).createOrUpdateInstallation(any());
// }
// }

// /**
// * クラス：NotificationHubUtil upsertInstallation
// * NotificationHub例外が送出されることを確認するテストケース
// */
// @Test
// void upsertInstallation_005() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// NotificationHubsException nhEx = mock(NotificationHubsException.class);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class,
// (hub, ctx) ->
// doThrow(nhEx).when(hub).createOrUpdateInstallation(any(Installation.class))))
// {

// // Act
// NotificationHubsException thrown =
// assertThrows(NotificationHubsException.class,
// () -> sut.upsertInstallation("iid", "1", "internal", "1", "deviceToken"));

// // Assert
// assertSame(nhEx, thrown);
// }
// }

// /**
// * クラス：NotificationHubUtil deleteInstallation
// * TOYOTAブランドの場合にdeleteInstallationが呼ばれることを確認するテストケース
// */
// @Test
// void deleteInstallation_001() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {

// // Act
// sut.deleteInstallation("iid", "1");

// // Assert
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub, times(1)).deleteInstallation(eq("iid"));
// }
// }

// /**
// * クラス：NotificationHubUtil deleteInstallation
// 不正ブランドの場合に外部呼び出しされないことを確認するテストケース
// */
// @Test
// void deleteInstallation_002() throws Exception {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {

// // Act
// sut.deleteInstallation("iid", "9");

// // Assert
// assertEquals(0, mocked.constructed().size());
// }
// }

// /**
// * クラス：NotificationHubUtil deleteInstallation
// * NotificationHub例外が送出されることを確認するテストケース
// */
// @Test
// void deleteInstallation_003() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// NotificationHubsException nhEx = mock(NotificationHubsException.class);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class,
// (hub, ctx) -> doThrow(nhEx).when(hub).deleteInstallation(anyString()))) {

// // Act
// NotificationHubsException thrown =
// assertThrows(NotificationHubsException.class,
// () -> sut.deleteInstallation("iid", "1"));

// // Assert
// assertSame(nhEx, thrown);
// }
// }

// /**
// * クラス：NotificationHubUtil postMessage
// * TOYOTA/ANDROIDの場合にsendNotificationが呼ばれ結果が返ることを確認するテストケース
// */
// @Test
// void postMessage_001() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// NotificationOutcome outcome = mock(NotificationOutcome.class);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class,
// (hub, ctx) -> when(hub.sendNotification(any(Notification.class),
// anyString())).thenReturn(outcome))) {

// // Act
// NotificationOutcome actual = sut.postMessage("iid", "{\"x\":1}", "1", "1");

// // Assert
// assertSame(outcome, actual);
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub, times(1)).sendNotification(any(FcmV1Notification.class),
// eq("$InstallationId:{iid}"));
// }
// }

// /**
// * クラス：NotificationHubUtil postMessage
// * TOYOTA/IOSの場合にsendNotificationが呼ばれ結果が返ることを確認するテストケース
// */
// @Test
// void postMessage_002() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// NotificationOutcome outcome = mock(NotificationOutcome.class);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class,
// (hub, ctx) -> when(hub.sendNotification(any(Notification.class),
// anyString())).thenReturn(outcome))) {

// // Act
// NotificationOutcome actual = sut.postMessage("iid", "{\"x\":1}", "1", "2");

// // Assert
// assertSame(outcome, actual);
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub, times(1)).sendNotification(any(AppleNotification.class),
// eq("$InstallationId:{iid}"));
// }
// }

// /** クラス：NotificationHubUtil postMessage 不正ブランドの場合にnullが返ることを確認するテストケース */
// @Test
// void postMessage_003() throws Exception {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {

// // Act
// NotificationOutcome actual = sut.postMessage("iid", "p", "9", "1");

// // Assert
// assertNull(actual);
// assertEquals(0, mocked.constructed().size());
// }
// }

// /**
// * クラス：NotificationHubUtil postMessage
// * 不正プラットフォームの場合にnullが返り外部呼び出しされないことを確認するテストケース
// */
// @Test
// void postMessage_004() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// // ★ getSdkConnectionStringTemplate()
// // は不正platformで到達しないのでスタブしない（UnnecessaryStubbing回避）

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {

// // Act
// NotificationOutcome actual = sut.postMessage("iid", "p", "1", "9");

// // Assert
// assertNull(actual);
// assertEquals(0, mocked.constructed().size());
// }
// }

// /** クラス：NotificationHubUtil postMessage NotificationHub例外が送出されることを確認するテストケース
// */
// @Test
// void postMessage_005() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
// when(propertiesUtil.getHubNameT()).thenReturn("hubT");
// when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
// when(propertiesUtil.getKeyT()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// NotificationHubsException nhEx = mock(NotificationHubsException.class);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class,
// (hub, ctx) -> when(hub.sendNotification(any(Notification.class),
// anyString())).thenThrow(nhEx))) {

// // Act
// NotificationHubsException thrown =
// assertThrows(NotificationHubsException.class,
// () -> sut.postMessage("iid", "p", "1", "1"));

// // Assert
// assertSame(nhEx, thrown);
// }
// }

// /**
// * クラス：NotificationHubUtil buildFcmV1Payload
// * 最小項目でpayload(JSON)が生成されることを確認するテストケース
// */
// @Test
// void buildFcmV1Payload_001() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// Map<String, Object> body = Map.of(
// "LocKey", "L1",
// "notificationId", 123);

// // Act
// String json = sut.buildFcmV1Payload(body);

// // Assert
// assertNotNull(json);
// assertTrue(json.contains("\"message\""));
// assertTrue(json.contains("\"android\""));
// assertTrue(json.contains("\"data\""));
// assertTrue(json.contains("\"LocKey\":\"L1\""));
// assertTrue(json.contains("\"notificationId\":\"123\""));
// }

// /**
// * クラス：NotificationHubUtil buildFcmV1Payload
// * pushInformationListが存在する場合にJSON文字列として埋め込まれることを確認するテストケース
// */
// @Test
// void buildFcmV1Payload_002() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// Map<String, Object> body = Map.of(
// "pushInformationList", java.util.List.of(Map.of("a", 1)));

// // Act
// String json = sut.buildFcmV1Payload(body);

// // Assert
// assertTrue(json.contains("\"pushInformationList\":\"["));
// }

// /**
// * クラス：NotificationHubUtil buildFcmV1Payload
// * popupInformationListが存在する場合にJSON文字列として埋め込まれることを確認するテストケース
// */
// @Test
// void buildFcmV1Payload_003() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// Map<String, Object> body = Map.of(
// "popupInformationList", java.util.List.of(Map.of("b", 2)));

// // Act
// String json = sut.buildFcmV1Payload(body);

// // Assert
// assertTrue(json.contains("\"popupInformationList\":\"["));
// }

// static class SelfRef {
// SelfRef self = this;
// }

// /**
// * クラス：NotificationHubUtil buildFcmV1Payload
// * JSON処理例外時にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void buildFcmV1Payload_004() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// Map<String, Object> body = Map.of(
// "pushInformationList", java.util.List.of(new SelfRef()));

// // Act
// CustomException ex = assertThrows(CustomException.class, () ->
// sut.buildFcmV1Payload(body));

// // Assert
// assertNotNull(ex.getCause());
// }

// /**
// * クラス：NotificationHubUtil buildApnsPayload
// * 最小項目でpayload(JSON)が生成されることを確認するテストケース
// */
// @Test
// void buildApnsPayload_001() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// Map<String, Object> body = Map.of(
// "LocKey", "L1",
// "notificationId", 123);

// // Act
// String json = sut.buildApnsPayload(body);

// // Assert
// assertNotNull(json);
// assertTrue(json.contains("\"aps\""));
// assertTrue(json.contains("\"alert\":\" \""));
// assertTrue(json.contains("\"mutable-content\":1"));
// assertTrue(json.contains("\"LocKey\""));
// assertTrue(json.contains("\"notificationId\""));
// }

// /**
// * クラス：NotificationHubUtil buildApnsPayload
// * pushInformationListが存在する場合に配列として埋め込まれることを確認するテストケース
// */
// @Test
// void buildApnsPayload_002() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// Map<String, Object> body = Map.of(
// "pushInformationList", java.util.List.of(Map.of("a", 1)));

// // Act
// String json = sut.buildApnsPayload(body);

// // Assert
// assertTrue(json.contains("\"pushInformationList\""));
// assertTrue(json.contains("["));
// }

// /**
// * クラス：NotificationHubUtil buildApnsPayload
// * JSON変換不能な値の場合にIllegalArgumentExceptionが送出されることを確認するテストケース
// */
// @Test
// void buildApnsPayload_003() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// Map<String, Object> body = Map.of(
// "popupInformationList", java.util.List.of(new SelfRef()));

// // Act
// IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, ()
// -> sut.buildApnsPayload(body));

// // Assert
// assertNotNull(ex.getCause());
// }

// /**
// * クラス：NotificationHubUtil buildApnsPayload
// *
// writeValueAsStringでJsonProcessingException発生時にCustomExceptionが送出されることを確認するテストケース
// */
// @Test
// void buildApnsPayload_004() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<com.fasterxml.jackson.databind.ObjectMapper> mocked =
// mockConstruction(
// com.fasterxml.jackson.databind.ObjectMapper.class, (mock, ctx) -> {
// com.fasterxml.jackson.databind.ObjectMapper real = new
// com.fasterxml.jackson.databind.ObjectMapper();
// when(mock.createObjectNode()).thenAnswer(inv -> real.createObjectNode());
// when(mock.valueToTree(any())).thenAnswer(inv ->
// real.valueToTree(inv.getArgument(0)));
// when(mock.writeValueAsString(any()))
// .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {
// });
// })) {

// Map<String, Object> body = Map.of("LocKey", "L1");

// // Act
// CustomException ex = assertThrows(CustomException.class, () ->
// sut.buildApnsPayload(body));

// // Assert
// assertNotNull(ex.getCause());
// }
// }

// /**
// * クラス：NotificationHubUtil putAsStringIfPresent
// * 値が存在する場合にdataNodeへ文字列設定されることを確認するテストケース
// */
// @Test
// void putAsStringIfPresent_001() throws Exception {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// com.fasterxml.jackson.databind.ObjectMapper mapper = new
// com.fasterxml.jackson.databind.ObjectMapper();
// com.fasterxml.jackson.databind.node.ObjectNode node =
// mapper.createObjectNode();
// Map<String, Object> body = Map.of("k", 10);

// Method m =
// NotificationHubUtil.class.getDeclaredMethod("putAsStringIfPresent",
// com.fasterxml.jackson.databind.node.ObjectNode.class, Map.class,
// String.class);
// m.setAccessible(true);

// // Act
// m.invoke(sut, node, body, "k");

// // Assert
// assertEquals("10", node.get("k").asText());
// }

// /**
// * クラス：NotificationHubUtil putAsStringIfPresent
// * 値が存在しない場合にdataNodeへ設定されないことを確認するテストケース
// */
// @Test
// void putAsStringIfPresent_002() throws Exception {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// com.fasterxml.jackson.databind.ObjectMapper mapper = new
// com.fasterxml.jackson.databind.ObjectMapper();
// com.fasterxml.jackson.databind.node.ObjectNode node =
// mapper.createObjectNode();
// Map<String, Object> body = Map.of("x", 1);

// Method m =
// NotificationHubUtil.class.getDeclaredMethod("putAsStringIfPresent",
// com.fasterxml.jackson.databind.node.ObjectNode.class, Map.class,
// String.class);
// m.setAccessible(true);

// // Act
// m.invoke(sut, node, body, "k");

// // Assert
// assertNull(node.get("k"));
// }

// /** クラス：NotificationHubUtil putIfPresent 値が存在する場合にrootへ設定されることを確認するテストケース */
// @Test
// void putIfPresent_001() throws Exception {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// com.fasterxml.jackson.databind.ObjectMapper mapper = new
// com.fasterxml.jackson.databind.ObjectMapper();
// com.fasterxml.jackson.databind.node.ObjectNode node =
// mapper.createObjectNode();
// Map<String, Object> body = Map.of("k", "v");

// Method m = NotificationHubUtil.class.getDeclaredMethod("putIfPresent",
// com.fasterxml.jackson.databind.node.ObjectNode.class,
// com.fasterxml.jackson.databind.ObjectMapper.class,
// Map.class, String.class);
// m.setAccessible(true);

// // Act
// m.invoke(sut, node, mapper, body, "k");

// // Assert
// assertEquals("v", node.get("k").asText());
// }

// /** クラス：NotificationHubUtil putIfPresent 値が存在しない場合にrootへ設定されないことを確認するテストケース
// */
// @Test
// void putIfPresent_002() throws Exception {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// com.fasterxml.jackson.databind.ObjectMapper mapper = new
// com.fasterxml.jackson.databind.ObjectMapper();
// com.fasterxml.jackson.databind.node.ObjectNode node =
// mapper.createObjectNode();
// Map<String, Object> body = Map.of("x", "y");

// Method m = NotificationHubUtil.class.getDeclaredMethod("putIfPresent",
// com.fasterxml.jackson.databind.node.ObjectNode.class,
// com.fasterxml.jackson.databind.ObjectMapper.class,
// Map.class, String.class);
// m.setAccessible(true);

// // Act
// m.invoke(sut, node, mapper, body, "k");

// // Assert
// assertNull(node.get("k"));
// }

// /*
// * *
// * クラス：NotificationHubUtil upsertInstallation
// * LEXUS/ANDROIDの場合にcreateOrUpdateInstallationが呼ばれることを確認するテストケース
// */
// @Test
// void upsertInstallation_006() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
// when(propertiesUtil.getHubNameL()).thenReturn("hubL");
// when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
// when(propertiesUtil.getKeyL()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {
// // Act
// sut.upsertInstallation("iid", "2", "internal", "1", "deviceToken");

// // Assert
// assertEquals(1, mocked.constructed().size());
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub,
// times(1)).createOrUpdateInstallation(any(FcmV1Installation.class));
// }
// }

// /*
// * *
// * クラス：NotificationHubUtil upsertInstallation
// * LEXUS/IOSの場合にcreateOrUpdateInstallationが呼ばれることを確認するテストケース
// */
// @Test
// void upsertInstallation_007() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
// when(propertiesUtil.getHubNameL()).thenReturn("hubL");
// when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
// when(propertiesUtil.getKeyL()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {
// // Act
// sut.upsertInstallation("iid", "2", "internal", "2", "deviceToken");

// // Assert
// assertEquals(1, mocked.constructed().size());
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub,
// times(1)).createOrUpdateInstallation(any(AppleInstallation.class));
// }
// }

// /*
// * *
// * クラス：NotificationHubUtil deleteInstallation
// * LEXUSブランドの場合にdeleteInstallationが呼ばれることを確認するテストケース
// */
// @Test
// void deleteInstallation_004() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
// when(propertiesUtil.getHubNameL()).thenReturn("hubL");
// when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
// when(propertiesUtil.getKeyL()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

// try (MockedConstruction<NotificationHub> mocked =
// mockConstruction(NotificationHub.class)) {
// // Act
// sut.deleteInstallation("iid", "2");

// // Assert
// assertEquals(1, mocked.constructed().size());
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub, times(1)).deleteInstallation(eq("iid"));
// }
// }

// /*
// * *
// * クラス：NotificationHubUtil postMessage
// * LEXUS/ANDROIDの場合にsendNotificationが呼ばれ結果が返ることを確認するテストケース
// */
// @Test
// void postMessage_006() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
// when(propertiesUtil.getHubNameL()).thenReturn("hubL");
// when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
// when(propertiesUtil.getKeyL()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// NotificationOutcome outcome = mock(NotificationOutcome.class);

// try (MockedConstruction<NotificationHub> mocked = mockConstruction(
// NotificationHub.class,
// (hub, ctx) -> when(hub.sendNotification(any(Notification.class),
// anyString())).thenReturn(outcome))) {

// // Act
// NotificationOutcome actual = sut.postMessage("iid", "{\"x\":1}", "2", "1");

// // Assert
// assertSame(outcome, actual);
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub, times(1)).sendNotification(any(FcmV1Notification.class),
// eq("$InstallationId:{iid}"));
// }
// }

// /*
// * *
// * クラス：NotificationHubUtil postMessage
// * LEXUS/IOSの場合にsendNotificationが呼ばれ結果が返ることを確認するテストケース
// */
// @Test
// void postMessage_007() throws Exception {
// // Arrange
// when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
// when(propertiesUtil.getHubNameL()).thenReturn("hubL");
// when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
// when(propertiesUtil.getKeyL()).thenReturn("secret");
// when(propertiesUtil.getSdkConnectionStringTemplate())
// .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");

// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// NotificationOutcome outcome = mock(NotificationOutcome.class);

// try (MockedConstruction<NotificationHub> mocked = mockConstruction(
// NotificationHub.class,
// (hub, ctx) -> when(hub.sendNotification(any(Notification.class),
// anyString())).thenReturn(outcome))) {

// // Act
// NotificationOutcome actual = sut.postMessage("iid", "{\"x\":1}", "2", "2");

// // Assert
// assertSame(outcome, actual);
// NotificationHub hub = mocked.constructed().get(0);
// verify(hub, times(1)).sendNotification(any(AppleNotification.class),
// eq("$InstallationId:{iid}"));
// }
// }

// /*
// * *
// * クラス：NotificationHubUtil buildApnsPayload
// * popupInformationListが存在する場合に配列として埋め込まれることを確認するテストケース
// */
// @Test
// void buildApnsPayload_005() {
// // Arrange
// NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
// Map<String, Object> body = Map.of(
// "popupInformationList", java.util.List.of(Map.of("b", 2)));

// // Act
// String json = sut.buildApnsPayload(body);

// // Assert
// assertNotNull(json);
// assertTrue(json.contains("\"popupInformationList\""));
// assertTrue(json.contains("["));
// }
// }
