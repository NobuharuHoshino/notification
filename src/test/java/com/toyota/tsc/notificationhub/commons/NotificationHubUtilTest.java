package com.toyota.tsc.notificationhub.commons;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.windowsazure.messaging.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NotificationHubUtil のテストクラス
 */
@SuppressWarnings("all")
@ExtendWith(MockitoExtension.class)
class NotificationHubUtilTest {

    @Mock
    private PropertiesUtil propertiesUtil;

    /** クラス：NotificationHubUtil generateSasToken TOYOTAブランドの場合にSASトークンが生成されることを確認するテストケース */
    @Test
    void generateSasToken_001() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getRootUri()).thenReturn("https://%s/%s");
        when(propertiesUtil.getTtlSeconds()).thenReturn(60);
        when(propertiesUtil.getSasTemplate()).thenReturn("SharedAccessSignature sr=%s&sig=%s&se=%s&skn=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String uri = String.format("https://%s/%s", "nsT", "hubT");
        String encodedUri = URLEncoder.encode(uri, StandardCharsets.UTF_8.name());
        // Act
        String token = sut.generateSasToken("1");
        // Assert
        assertNotNull(token);
        assertTrue(token.startsWith("SharedAccessSignature "));
        assertTrue(token.contains("sr=" + encodedUri));
        assertTrue(token.contains("skn=keyNameT"));
    }

    /** クラス：NotificationHubUtil generateSasToken LEXUSブランドの場合にSASトークンが生成されることを確認するテストケース */
    @Test
    void generateSasToken_002() {
        // Arrange
        when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
        when(propertiesUtil.getHubNameL()).thenReturn("hubL");
        when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
        when(propertiesUtil.getKeyL()).thenReturn("secret");
        when(propertiesUtil.getRootUri()).thenReturn("https://%s/%s");
        when(propertiesUtil.getTtlSeconds()).thenReturn(60);
        when(propertiesUtil.getSasTemplate()).thenReturn("SharedAccessSignature sr=%s&sig=%s&se=%s&skn=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        // Act
        String token = sut.generateSasToken("2");
        // Assert
        assertNotNull(token);
        assertTrue(token.contains("skn=keyNameL"));
    }

    /** クラス：NotificationHubUtil generateSasToken 不正ブランドの場合にnullが返ることを確認するテストケース */
    @Test
    void generateSasToken_003() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        // Act
        String token = sut.generateSasToken("9");
        // Assert
        assertNull(token);
    }

    /** クラス：NotificationHubUtil generateSasToken 例外発生時にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void generateSasToken_004() {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn(null); // NPE誘発
        when(propertiesUtil.getRootUri()).thenReturn("https://%s/%s");
        when(propertiesUtil.getTtlSeconds()).thenReturn(60);
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        // Act
        CustomException ex = assertThrows(CustomException.class, () -> sut.generateSasToken("1"));
        // Assert
        assertNotNull(ex.getCause());
    }

    /** クラス：NotificationHubUtil upsertInstallation TOYOTA/ANDROIDの場合にcreateOrUpdateInstallationが呼ばれることを確認するテストケース */
    @Test
    void upsertInstallation_001() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            sut.upsertInstallation("iid", "1", "internal", "1", "deviceToken");
            // Assert
            assertEquals(1, mocked.constructed().size());
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).createOrUpdateInstallation(any(FcmV1Installation.class));
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation TOYOTA/IOSの場合にcreateOrUpdateInstallationが呼ばれることを確認するテストケース */
    @Test
    void upsertInstallation_002() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            sut.upsertInstallation("iid", "1", "internal", "2", "deviceToken");
            // Assert
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).createOrUpdateInstallation(any(AppleInstallation.class));
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation 不正ブランドの場合に外部呼び出しされないことを確認するテストケース */
    @Test
    void upsertInstallation_003() throws Exception {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            sut.upsertInstallation("iid", "9", "internal", "1", "deviceToken");
            // Assert
            assertEquals(0, mocked.constructed().size());
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation 不正プラットフォームの場合にcreateOrUpdateInstallationが呼ばれないことを確認するテストケース */
    @Test
    void upsertInstallation_004() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            sut.upsertInstallation("iid", "1", "internal", "9", "deviceToken");
            // Assert
            assertEquals(1, mocked.constructed().size());
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, never()).createOrUpdateInstallation(any());
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation NotificationHub例外が送出されることを確認するテストケース */
    @Test
    void upsertInstallation_005() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        NotificationHubsException nhEx = mock(NotificationHubsException.class);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class,
                (hub, ctx) -> doThrow(nhEx).when(hub).createOrUpdateInstallation(any(Installation.class)))) {
            // Act
            NotificationHubsException thrown = assertThrows(NotificationHubsException.class,
                    () -> sut.upsertInstallation("iid", "1", "internal", "1", "deviceToken"));
            // Assert
            assertSame(nhEx, thrown);
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation LEXUS/ANDROIDの場合にcreateOrUpdateInstallationが呼ばれることを確認するテストケース */
    @Test
    void upsertInstallation_006() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
        when(propertiesUtil.getHubNameL()).thenReturn("hubL");
        when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
        when(propertiesUtil.getKeyL()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            sut.upsertInstallation("iid", "2", "internal", "1", "deviceToken");
            // Assert
            assertEquals(1, mocked.constructed().size());
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).createOrUpdateInstallation(any(FcmV1Installation.class));
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation LEXUS/IOSの場合にcreateOrUpdateInstallationが呼ばれることを確認するテストケース */
    @Test
    void upsertInstallation_007() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
        when(propertiesUtil.getHubNameL()).thenReturn("hubL");
        when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
        when(propertiesUtil.getKeyL()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            sut.upsertInstallation("iid", "2", "internal", "2", "deviceToken");
            // Assert
            assertEquals(1, mocked.constructed().size());
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).createOrUpdateInstallation(any(AppleInstallation.class));
        }
    }

    /** クラス：NotificationHubUtil deleteInstallation TOYOTAブランドの場合にdeleteInstallationが呼ばれることを確認するテストケース */
    @Test
    void deleteInstallation_001() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            sut.deleteInstallation("iid", "1");
            // Assert
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).deleteInstallation(eq("iid"));
        }
    }

    /** クラス：NotificationHubUtil deleteInstallation 不正ブランドの場合に外部呼び出しされないことを確認するテストケース */
    @Test
    void deleteInstallation_002() throws Exception {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            sut.deleteInstallation("iid", "9");
            // Assert
            assertEquals(0, mocked.constructed().size());
        }
    }

    /** クラス：NotificationHubUtil deleteInstallation NotificationHub例外が送出されることを確認するテストケース */
    @Test
    void deleteInstallation_003() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        NotificationHubsException nhEx = mock(NotificationHubsException.class);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class,
                (hub, ctx) -> doThrow(nhEx).when(hub).deleteInstallation(anyString()))) {
            // Act
            NotificationHubsException thrown = assertThrows(NotificationHubsException.class,
                    () -> sut.deleteInstallation("iid", "1"));
            // Assert
            assertSame(nhEx, thrown);
        }
    }

    /** クラス：NotificationHubUtil deleteInstallation LEXUSブランドの場合にdeleteInstallationが呼ばれることを確認するテストケース */
    @Test
    void deleteInstallation_004() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
        when(propertiesUtil.getHubNameL()).thenReturn("hubL");
        when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
        when(propertiesUtil.getKeyL()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            sut.deleteInstallation("iid", "2");
            // Assert
            assertEquals(1, mocked.constructed().size());
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).deleteInstallation(eq("iid"));
        }
    }

    /** クラス：NotificationHubUtil postMessage TOYOTA/ANDROIDの場合にsendNotificationが呼ばれ結果が返ることを確認するテストケース */
    @Test
    void postMessage_001() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        NotificationOutcome outcome = mock(NotificationOutcome.class);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class,
                (hub, ctx) -> when(hub.sendNotification(any(Notification.class), anyString())).thenReturn(outcome))) {
            // Act
            NotificationOutcome actual = sut.postMessage("iid", "{\"x\":1}", "1", "1");
            // Assert
            assertSame(outcome, actual);
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).sendNotification(any(FcmV1Notification.class), eq("$InstallationId:{iid}"));
        }
    }

    /** クラス：NotificationHubUtil postMessage TOYOTA/IOSの場合にsendNotificationが呼ばれ結果が返ることを確認するテストケース */
    @Test
    void postMessage_002() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        NotificationOutcome outcome = mock(NotificationOutcome.class);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class,
                (hub, ctx) -> when(hub.sendNotification(any(Notification.class), anyString())).thenReturn(outcome))) {
            // Act
            NotificationOutcome actual = sut.postMessage("iid", "{\"x\":1}", "1", "2");
            // Assert
            assertSame(outcome, actual);
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).sendNotification(any(AppleNotification.class), eq("$InstallationId:{iid}"));
        }
    }

    /** クラス：NotificationHubUtil postMessage 不正ブランドの場合にnullが返ることを確認するテストケース */
    @Test
    void postMessage_003() throws Exception {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            NotificationOutcome actual = sut.postMessage("iid", "p", "9", "1");
            // Assert
            assertNull(actual);
            assertEquals(0, mocked.constructed().size());
        }
    }

    /** クラス：NotificationHubUtil postMessage 不正プラットフォームの場合にnullが返り外部呼び出しされないことを確認するテストケース */
    @Test
    void postMessage_004() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // Act
            NotificationOutcome actual = sut.postMessage("iid", "p", "1", "9");
            // Assert
            assertNull(actual);
            assertEquals(0, mocked.constructed().size());
        }
    }

    /** クラス：NotificationHubUtil postMessage NotificationHub例外が送出されることを確認するテストケース */
    @Test
    void postMessage_005() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceT()).thenReturn("nsT");
        when(propertiesUtil.getHubNameT()).thenReturn("hubT");
        when(propertiesUtil.getKeyNameT()).thenReturn("keyNameT");
        when(propertiesUtil.getKeyT()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        NotificationHubsException nhEx = mock(NotificationHubsException.class);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class,
                (hub, ctx) -> when(hub.sendNotification(any(Notification.class), anyString())).thenThrow(nhEx))) {
            // Act
            NotificationHubsException thrown = assertThrows(NotificationHubsException.class,
                    () -> sut.postMessage("iid", "p", "1", "1"));
            // Assert
            assertSame(nhEx, thrown);
        }
    }

    /** クラス：NotificationHubUtil postMessage LEXUS/ANDROIDの場合にsendNotificationが呼ばれ結果が返ることを確認するテストケース */
    @Test
    void postMessage_006() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
        when(propertiesUtil.getHubNameL()).thenReturn("hubL");
        when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
        when(propertiesUtil.getKeyL()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        NotificationOutcome outcome = mock(NotificationOutcome.class);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class,
                (hub, ctx) -> when(hub.sendNotification(any(Notification.class), anyString())).thenReturn(outcome))) {
            // Act
            NotificationOutcome actual = sut.postMessage("iid", "{\"x\":1}", "2", "1");
            // Assert
            assertSame(outcome, actual);
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).sendNotification(any(FcmV1Notification.class), eq("$InstallationId:{iid}"));
        }
    }

    /** クラス：NotificationHubUtil postMessage LEXUS/IOSの場合にsendNotificationが呼ばれ結果が返ることを確認するテストケース */
    @Test
    void postMessage_007() throws Exception {
        // Arrange
        when(propertiesUtil.getNamespaceL()).thenReturn("nsL");
        when(propertiesUtil.getHubNameL()).thenReturn("hubL");
        when(propertiesUtil.getKeyNameL()).thenReturn("keyNameL");
        when(propertiesUtil.getKeyL()).thenReturn("secret");
        when(propertiesUtil.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        NotificationOutcome outcome = mock(NotificationOutcome.class);

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class,
                (hub, ctx) -> when(hub.sendNotification(any(Notification.class), anyString())).thenReturn(outcome))) {
            // Act
            NotificationOutcome actual = sut.postMessage("iid", "{\"x\":1}", "2", "2");
            // Assert
            assertSame(outcome, actual);
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).sendNotification(any(AppleNotification.class), eq("$InstallationId:{iid}"));
        }
    }

    /** クラス：NotificationHubUtil buildFcmV1Payload 最小項目でpayload(JSON)が生成されることを確認するテストケース */
    @Test
    void buildFcmV1Payload_001() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String bodyData = "{\"LocKey\":\"L1\",\"notificationId\":123}";
        // Act
        String json = sut.buildFcmV1Payload(bodyData);
        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"message\""));
        assertTrue(json.contains("\"android\""));
        assertTrue(json.contains("\"priority\":\"high\""));
        assertTrue(json.contains("\"data\""));
        assertTrue(json.contains("\"LocKey\":\"L1\""));
        assertTrue(json.contains("\"notificationId\":\"123\""));
    }

    /** クラス：NotificationHubUtil buildFcmV1Payload pushInformationListが存在する場合にJSON文字列として埋め込まれることを確認するテストケース */
    @Test
    void buildFcmV1Payload_002() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String bodyData = "{\"pushInformationList\":[{\"a\":1}]}";
        // Act
        String json = sut.buildFcmV1Payload(bodyData);
        // Assert
        assertTrue(json.contains("\"pushInformationList\":\"["));
        assertTrue(json.contains("\"priority\":\"high\""));
    }

    /** クラス：NotificationHubUtil buildFcmV1Payload popupInformationListが存在する場合にJSON文字列として埋め込まれることを確認するテストケース */
    @Test
    void buildFcmV1Payload_003() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String bodyData = "{\"popupInformationList\":[{\"b\":2}]}";
        // Act
        String json = sut.buildFcmV1Payload(bodyData);
        // Assert
        assertTrue(json.contains("\"popupInformationList\":\"["));
        assertTrue(json.contains("\"priority\":\"high\""));
    }

    /** クラス：NotificationHubUtil buildFcmV1Payload android.priority が "high" 固定で設定されることを確認するテストケース */
    @Test
    void buildFcmV1Payload_005() throws Exception {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String bodyData = "{\"LocKey\":\"L1\"}";
        // Act
        String json = sut.buildFcmV1Payload(bodyData);
        // Assert
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
        com.fasterxml.jackson.databind.JsonNode androidNode = root.path("message").path("android");
        assertEquals("high", androidNode.path("priority").asText());
        assertTrue(androidNode.has("data"));
    }

    static class SelfRef {
        SelfRef self = this;
    }

    /** クラス：NotificationHubUtil buildFcmV1Payload JSON処理例外時にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void buildFcmV1Payload_004() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        try (MockedStatic<NotificationHubUtil> staticMock = mockStatic(NotificationHubUtil.class);
             MockedConstruction<ObjectMapper> mockedOM = mockConstruction(ObjectMapper.class, (mock, ctx) -> {
                 when(mock.createObjectNode()).thenAnswer(inv -> JsonNodeFactory.instance.objectNode());
                 when(mock.writeValueAsString(any()))
                         .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
             })) {
            staticMock.when(() -> NotificationHubUtil.toMap(anyString()))
                    .thenReturn(Map.of("pushInformationList", List.of("item")));
            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.buildFcmV1Payload("{}"));
            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /** クラス：NotificationHubUtil buildApnsPayload 最小項目でpayload(JSON)が生成されることを確認するテストケース */
    @Test
    void buildApnsPayload_001() {
        // Arrange
        when(propertiesUtil.getSilentPushLockeys()).thenReturn("");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String bodyData = "{\"LocKey\":\"L1\",\"notificationId\":123}";
        // Act
        String json = sut.buildApnsPayload(bodyData);
        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"aps\""));
        assertTrue(json.contains("\"alert\":\" \""));
        assertTrue(json.contains("\"mutable-content\":1"));
        assertTrue(json.contains("\"LocKey\""));
        assertTrue(json.contains("\"notificationId\""));
    }

    /** クラス：NotificationHubUtil buildApnsPayload pushInformationListが存在する場合に配列として埋め込まれることを確認するテストケース */
    @Test
    void buildApnsPayload_002() {
        // Arrange
        when(propertiesUtil.getSilentPushLockeys()).thenReturn("");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String bodyData = "{\"pushInformationList\":[{\"a\":1}]}";
        // Act
        String json = sut.buildApnsPayload(bodyData);
        // Assert
        assertTrue(json.contains("\"pushInformationList\""));
        assertTrue(json.contains("["));
    }

    /** クラス：NotificationHubUtil buildApnsPayload popupInformationListのvalueToTreeで例外発生時にIllegalArgumentExceptionが送出されることを確認するテストケース */
    @Test
    void buildApnsPayload_003() {
        // Arrange
        when(propertiesUtil.getSilentPushLockeys()).thenReturn("");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        try (MockedStatic<NotificationHubUtil> staticMock = mockStatic(NotificationHubUtil.class);
             MockedConstruction<ObjectMapper> mockedOM = mockConstruction(ObjectMapper.class, (mock, ctx) -> {
                 when(mock.createObjectNode()).thenAnswer(inv -> JsonNodeFactory.instance.objectNode());
                 when(mock.valueToTree(any()))
                         .thenThrow(new IllegalArgumentException("boom", new RuntimeException("cause")));
             })) {
            staticMock.when(() -> NotificationHubUtil.toMap(anyString()))
                    .thenReturn(Map.of("popupInformationList", List.of("item")));
            // Act
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> sut.buildApnsPayload("{}"));
            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /** クラス：NotificationHubUtil buildApnsPayload writeValueAsStringで例外発生時にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void buildApnsPayload_004() {
        // Arrange
        when(propertiesUtil.getSilentPushLockeys()).thenReturn("");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        try (MockedStatic<NotificationHubUtil> staticMock = mockStatic(NotificationHubUtil.class);
             MockedConstruction<ObjectMapper> mockedOM = mockConstruction(ObjectMapper.class, (mock, ctx) -> {
                 when(mock.createObjectNode()).thenAnswer(inv -> JsonNodeFactory.instance.objectNode());
                 when(mock.writeValueAsString(any()))
                         .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("boom") {});
             })) {
            staticMock.when(() -> NotificationHubUtil.toMap(anyString()))
                    .thenReturn(Map.of("LocKey", "L1"));
            // Act
            CustomException ex = assertThrows(CustomException.class,
                    () -> sut.buildApnsPayload("{}"));
            // Assert
            assertNotNull(ex.getCause());
        }
    }

    /** クラス：NotificationHubUtil buildApnsPayload popupInformationListが存在する場合に配列として埋め込まれることを確認するテストケース */
    @Test
    void buildApnsPayload_005() {
        // Arrange
        when(propertiesUtil.getSilentPushLockeys()).thenReturn("");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String bodyData = "{\"popupInformationList\":[{\"b\":2}]}";
        // Act
        String json = sut.buildApnsPayload(bodyData);
        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"popupInformationList\""));
        assertTrue(json.contains("["));
    }

    /** クラス：NotificationHubUtil buildApnsPayload サイレントプッシュのLocKeyに一致する場合content-availableが設定されることを確認するテストケース */
    @Test
    void buildApnsPayload_006() {
        // Arrange
        when(propertiesUtil.getSilentPushLockeys()).thenReturn("SILENT1,SILENT2");
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String bodyData = "{\"LocKey\":\"SILENT1\",\"notificationId\":456}";
        // Act
        String json = sut.buildApnsPayload(bodyData);
        // Assert
        assertNotNull(json);
        assertTrue(json.contains("\"content-available\":1"));
        assertFalse(json.contains("\"mutable-content\""));
    }

    /** クラス：NotificationHubUtil putAsStringIfPresent 値が存在する場合にdataNodeへ文字列設定されることを確認するテストケース */
    @Test
    void putAsStringIfPresent_001() throws Exception {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode node = mapper.createObjectNode();
        Map<String, Object> body = Map.of("k", 10);
        Method m = NotificationHubUtil.class.getDeclaredMethod("putAsStringIfPresent",
                com.fasterxml.jackson.databind.node.ObjectNode.class, Map.class, String.class);
        m.setAccessible(true);
        // Act
        m.invoke(sut, node, body, "k");
        // Assert
        assertEquals("10", node.get("k").asText());
    }

    /** クラス：NotificationHubUtil putAsStringIfPresent 値が存在しない場合にdataNodeへ設定されないことを確認するテストケース */
    @Test
    void putAsStringIfPresent_002() throws Exception {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode node = mapper.createObjectNode();
        Map<String, Object> body = Map.of("x", 1);
        Method m = NotificationHubUtil.class.getDeclaredMethod("putAsStringIfPresent",
                com.fasterxml.jackson.databind.node.ObjectNode.class, Map.class, String.class);
        m.setAccessible(true);
        // Act
        m.invoke(sut, node, body, "k");
        // Assert
        assertNull(node.get("k"));
    }

    /** クラス：NotificationHubUtil putIfPresent 値が存在する場合にrootへ設定されることを確認するテストケース */
    @Test
    void putIfPresent_001() throws Exception {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode node = mapper.createObjectNode();
        Map<String, Object> body = Map.of("k", "v");
        Method m = NotificationHubUtil.class.getDeclaredMethod("putIfPresent",
                com.fasterxml.jackson.databind.node.ObjectNode.class,
                ObjectMapper.class,
                Map.class, String.class);
        m.setAccessible(true);
        // Act
        m.invoke(sut, node, mapper, body, "k");
        // Assert
        assertEquals("v", node.get("k").asText());
    }

    /** クラス：NotificationHubUtil putIfPresent 値が存在しない場合にrootへ設定されないことを確認するテストケース */
    @Test
    void putIfPresent_002() throws Exception {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        ObjectMapper mapper = new ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode node = mapper.createObjectNode();
        Map<String, Object> body = Map.of("x", "y");
        Method m = NotificationHubUtil.class.getDeclaredMethod("putIfPresent",
                com.fasterxml.jackson.databind.node.ObjectNode.class,
                ObjectMapper.class,
                Map.class, String.class);
        m.setAccessible(true);
        // Act
        m.invoke(sut, node, mapper, body, "k");
        // Assert
        assertNull(node.get("k"));
    }

    /** クラス：NotificationHubUtil toMap 正常なJSONの場合にMapが返ることを確認するテストケース */
    @Test
    void toMap_001() {
        // Arrange
        String json = "{\"key\":\"value\",\"num\":42}";
        // Act
        Map<String, Object> result = NotificationHubUtil.toMap(json);
        // Assert
        assertNotNull(result);
        assertEquals("value", result.get("key"));
        assertEquals(42, result.get("num"));
    }

    /** クラス：NotificationHubUtil toMap 不正JSONの場合にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void toMap_002() {
        // Arrange
        String json = "invalid-json";
        // Act
        CustomException ex = assertThrows(CustomException.class, () -> NotificationHubUtil.toMap(json));
        // Assert
        assertNotNull(ex.getCause());
    }

    /** クラス：NotificationHubUtil replaceLcsSelected ANDROIDプラットフォームの場合にlcsSelectedが更新されることを確認するテストケース */
    @Test
    void replaceLcsSelected_001() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String payload = "{\"message\":{\"android\":{\"data\":{\"key\":\"val\"}}}}";
        // Act
        String result = sut.replaceLcsSelected(payload, "1", "LCS_CODE");
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("\"lcsSelected\":\"LCS_CODE\""));
    }

    /** クラス：NotificationHubUtil replaceLcsSelected IOSプラットフォームの場合にlcsSelectedが更新されることを確認するテストケース */
    @Test
    void replaceLcsSelected_002() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String payload = "{\"aps\":{\"alert\":\" \"}}";
        // Act
        String result = sut.replaceLcsSelected(payload, "2", "LCS_CODE");
        // Assert
        assertNotNull(result);
        assertTrue(result.contains("\"lcsSelected\":\"LCS_CODE\""));
    }

    /** クラス：NotificationHubUtil replaceLcsSelected 不正プラットフォームの場合にnullが返ることを確認するテストケース */
    @Test
    void replaceLcsSelected_003() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String payload = "{\"key\":\"val\"}";
        // Act
        String result = sut.replaceLcsSelected(payload, "9", "LCS_CODE");
        // Assert
        assertNull(result);
    }

    /** クラス：NotificationHubUtil replaceLcsSelected 不正JSONの場合にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void replaceLcsSelected_004() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        // Act
        CustomException ex = assertThrows(CustomException.class,
                () -> sut.replaceLcsSelected("invalid-json", "1", "LCS_CODE"));
        // Assert
        assertNotNull(ex.getCause());
    }

    /** クラス：NotificationHubUtil replaceLcsSelected ANDROIDでノードが存在しない場合にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void replaceLcsSelected_005() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String payload = "{\"message\":{}}";
        // Act
        CustomException ex = assertThrows(CustomException.class,
                () -> sut.replaceLcsSelected(payload, "1", "LCS_CODE"));
        // Assert
        assertNotNull(ex);
    }

    /** クラス：NotificationHubUtil replaceLcsSelected ANDROIDでノードがオブジェクトでない場合にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void replaceLcsSelected_006() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String payload = "{\"message\":{\"android\":\"notAnObject\"}}";
        // Act
        CustomException ex = assertThrows(CustomException.class,
                () -> sut.replaceLcsSelected(payload, "1", "LCS_CODE"));
        // Assert
        assertNotNull(ex);
    }

    /** クラス：NotificationHubUtil replaceLcsSelected ANDROIDでノードがJSONのnullの場合にCustomExceptionが送出されることを確認するテストケース */
    @Test
    void replaceLcsSelected_007() {
        // Arrange
        NotificationHubUtil sut = new NotificationHubUtil(propertiesUtil);
        String payload = "{\"message\":{\"android\":null}}";
        // Act
        CustomException ex = assertThrows(CustomException.class,
                () -> sut.replaceLcsSelected(payload, "1", "LCS_CODE"));
        // Assert
        assertNotNull(ex);
    }
}
