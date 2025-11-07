package com.toyota.tsc.notificationhub.commons;

import com.windowsazure.messaging.NotificationHub;
import com.windowsazure.messaging.NotificationOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * テストクラス：NotificationHubUtilTest
 */
class NotificationHubUtilTest {

    private NotificationHubUtil util;

    @BeforeEach
    void setUp() throws Exception {
        util = new NotificationHubUtil();
        // ブランドT
        set(util, "namespaceT", "ns-t");
        set(util, "hubNameT", "hub-t");
        set(util, "keyNameT", "keyname-t");
        set(util, "keyT", "key-t");
        // ブランドL
        set(util, "namespaceL", "ns-l");
        set(util, "hubNameL", "hub-l");
        set(util, "keyNameL", "keyname-l");
        set(util, "keyL", "key-l");
        // 共通
        set(util, "apiVersion", "2020-01");
        set(util, "rootUri", "https://%s.servicebus.windows.net/%s");
        set(util, "installationUri", "/installations/%s?api-version=%s");
        set(util, "ttlSeconds", 60);
        set(util, "timeoutMillis", 1000);
        set(util, "sdkConnectionStringTemplate",
                "Endpoint=sb://%s.servicebus.windows.net/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        set(util, "sasTemplate", "sr=%1$s&sig=%2$s&se=%3$d&skn=%4$s");
        set(util, "installationPayloadTemplate", "{\"id\":\"%s\"}");
    }

    private static void set(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }

    /** クラス：NotificationHubUtil generateSasToken（TOYOTA向けSAS生成）を確認するテストケース */
    @Test
    void generateSasToken_01() {
        // 実行
        String sas = util.generateSasToken("1");
        // 確認（キー名がテンプレートに埋め込まれていること）
        assertNotNull(sas);
        assertTrue(sas.contains("skn=keyname-t"));
        assertTrue(sas.contains("sr=https%3A%2F%2Fns-t.servicebus.windows.net%2Fhub-t"));
    }

    /** クラス：NotificationHubUtil generateSasToken（LEXUS向けSAS生成）を確認するテストケース */
    @Test
    void generateSasToken_02() {
        String sas = util.generateSasToken("2");
        assertNotNull(sas);
        assertTrue(sas.contains("skn=keyname-l"));
        assertTrue(sas.contains("sr=https%3A%2F%2Fns-l.servicebus.windows.net%2Fhub-l"));
    }

    /** クラス：NotificationHubUtil generateSasToken（未知ブランド→null）を確認するテストケース */
    @Test
    void generateSasToken_03() {
        assertNull(util.generateSasToken("9"));
    }

    /**
     * クラス：NotificationHubUtil upsertInstallation（Android:
     * createOrUpdateが呼ばれる）を確認するテストケース
     */
    @Test
    void upsertInstallation_01() throws Exception {
        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            // 実行
            util.upsertInstallation("inst-1", "1", "U1", "1", "token-A");
            // 確認：Hub生成1回、createOrUpdateInstallationが呼ばれる
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).createOrUpdateInstallation(any());
        }
    }

    /**
     * クラス：NotificationHubUtil upsertInstallation（iOS:
     * createOrUpdateが呼ばれる）を確認するテストケース
     */
    @Test
    void upsertInstallation_02() throws Exception {
        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            util.upsertInstallation("inst-2", "2", "U2", "2", "token-I");
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).createOrUpdateInstallation(any());
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation（未知ブランド→Hub未生成）を確認するテストケース */
    @Test
    void upsertInstallation_03() throws Exception {
        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            util.upsertInstallation("inst-9", "9", "U9", "1", "token-X");
            assertEquals(0, mocked.constructed().size());
        }
    }

    /**
     * クラス：NotificationHubUtil
     * upsertInstallation（未知プラットフォーム→createOrUpdate未呼出）を確認するテストケース
     */
    @Test
    void upsertInstallation_04() throws Exception {
        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            util.upsertInstallation("inst-3", "1", "U3", "9", "token-X");
            NotificationHub hub = mocked.constructed().get(0);
            // createOrUpdateInstallation が呼ばれていない
            verify(hub, times(0)).createOrUpdateInstallation(any());
        }
    }

    /** クラス：NotificationHubUtil deleteInstallation（呼び出し成功）を確認するテストケース */
    @Test
    void deleteInstallation_01() throws Exception {
        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            util.deleteInstallation("inst-1", "1");
            NotificationHub hub = mocked.constructed().get(0);
            verify(hub, times(1)).deleteInstallation(eq("inst-1"));
        }
    }

    /** クラス：NotificationHubUtil deleteInstallation（未知ブランド→Hub未生成）を確認するテストケース */
    @Test
    void deleteInstallation_02() throws Exception {
        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            util.deleteInstallation("inst-9", "9");
            assertEquals(0, mocked.constructed().size());
        }
    }

    /** クラス：NotificationHubUtil postMessage（Android: Outcome返却）を確認するテストケース */
    @Test
    void postMessage_01() throws Exception {
        NotificationOutcome outcome = mock(NotificationOutcome.class);
        when(outcome.getNotificationId()).thenReturn("n-android");

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class,
                (hub, ctx) -> when(hub.sendDirectNotification(any(), eq("inst-A"))).thenReturn(outcome))) {

            // 実行
            NotificationOutcome result = util.postMessage("inst-A", "{\"data\":\"x\"}", "1", "1");

            // 確認
            assertNotNull(result);
            assertEquals("n-android", result.getNotificationId());
            NotificationHub nh = mocked.constructed().get(0);
            verify(nh, times(1)).sendDirectNotification(any(), eq("inst-A"));
        }
    }

    /** クラス：NotificationHubUtil postMessage（iOS: Outcome返却）を確認するテストケース */
    @Test
    void postMessage_02() throws Exception {
        NotificationOutcome outcome = mock(NotificationOutcome.class);
        when(outcome.getNotificationId()).thenReturn("n-ios");

        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class,
                (hub, ctx) -> when(hub.sendDirectNotification(any(), eq("inst-I"))).thenReturn(outcome))) {

            NotificationOutcome result = util.postMessage("inst-I", "{\"aps\":{\"alert\":\"x\"}}", "2", "2");
            assertNotNull(result);
            assertEquals("n-ios", result.getNotificationId());
        }
    }

    /** クラス：NotificationHubUtil postMessage（未知ブランド→null）を確認するテストケース */
    @Test
    void postMessage_03() throws Exception {
        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            NotificationOutcome result = util.postMessage("inst-X", "{}", "9", "1");
            assertNull(result);
            assertEquals(0, mocked.constructed().size());
        }
    }

    /** クラス：NotificationHubUtil postMessage（未知プラットフォーム→null）を確認するテストケース */
    @Test
    void postMessage_04() throws Exception {
        try (MockedConstruction<NotificationHub> mocked = mockConstruction(NotificationHub.class)) {
            NotificationOutcome result = util.postMessage("inst-Y", "{}", "1", "9");
            assertNull(result);
            assertEquals(0, mocked.constructed().size());
        }
    }
}
