package com.toyota.tsc.notificationhub.commons;

import com.windowsazure.messaging.NotificationHubsException;
import com.windowsazure.messaging.NotificationHub;
import com.windowsazure.messaging.NotificationOutcome;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * クラス：NotificationHubUtil すべての分岐を確認するテストケース
 */
class NotificationHubUtilTest {

    private NotificationHubUtil util;

    @BeforeEach
    void setup() throws Exception {
        util = new NotificationHubUtil();
        // @Value を直接セット
        setField(util, NotificationHubUtil.class, "apiVersion", "2020-01");
        setField(util, NotificationHubUtil.class, "rootUri", "https://%s.servicebus.windows.net/%s");
        setField(util, NotificationHubUtil.class, "installationUri", "/installations");
        setField(util, NotificationHubUtil.class, "ttlSeconds", 3600);
        setField(util, NotificationHubUtil.class, "timeoutMillis", 10000);

        // TOYOTA
        setField(util, NotificationHubUtil.class, "namespaceT", "ns-t");
        setField(util, NotificationHubUtil.class, "hubNameT", "hub-t");
        setField(util, NotificationHubUtil.class, "keyNameT", "DefaultFullSharedAccessSignature");
        setField(util, NotificationHubUtil.class, "keyT", "KEY_T_123456");

        // LEXUS
        setField(util, NotificationHubUtil.class, "namespaceL", "ns-l");
        setField(util, NotificationHubUtil.class, "hubNameL", "hub-l");
        setField(util, NotificationHubUtil.class, "keyNameL", "DefaultFullSharedAccessSignature");
        setField(util, NotificationHubUtil.class, "keyL", "KEY_L_654321");

        setField(util, NotificationHubUtil.class, "sasTemplate",
                "SharedAccessSignature sr=%s&sig=%s&se=%d&skn=%s");
        setField(util, NotificationHubUtil.class, "installationPayloadTemplate", "{\"id\":\"%s\"}");
        setField(util, NotificationHubUtil.class, "sdkConnectionStringTemplate",
                "Endpoint=sb://%s.servicebus.windows.net/;SharedAccessKeyName=%s;SharedAccessKey=%s;");
    }

    private static void setField(Object target, Class<?> declaring, String name, Object value) throws Exception {
        var f = declaring.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // --- generateSasToken ---

    /** クラス：NotificationHubUtil generateSasToken TOYOTAブランドの生成を確認するテストケース */
    @Test
    void generateSasToken_01() {
        String token = util.generateSasToken("1");
        assertNotNull(token);
        assertTrue(token.startsWith("SharedAccessSignature "));
    }

    /** クラス：NotificationHubUtil generateSasToken LEXUSブランドの生成を確認するテストケース */
    @Test
    void generateSasToken_02() {
        String token = util.generateSasToken("2");
        assertNotNull(token);
        assertTrue(token.contains("skn=DefaultFullSharedAccessSignature"));
    }

    /** クラス：NotificationHubUtil generateSasToken 不正ブランドでnullとなることを確認するテストケース */
    @Test
    void generateSasToken_03() {
        assertNull(util.generateSasToken("9"));
    }

    /**
     * クラス：NotificationHubUtil generateSasToken
     * キー未設定でRuntimeExceptionとなることを確認するテストケース
     */
    @Test
    void generateSasToken_04() throws Exception {
        setField(util, NotificationHubUtil.class, "keyT", null);
        assertThrows(RuntimeException.class, () -> util.generateSasToken("1"));
    }

    // --- upsertInstallation ---

    /** クラス：NotificationHubUtil upsertInstallation Androidで登録が呼ばれることを確認するテストケース */
    @Test
    void upsertInstallation_01() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            doNothing().when(hub).createOrUpdateInstallation(any());
        })) {
            assertDoesNotThrow(() -> util.upsertInstallation("inst-1", "1", "U1", "1", "token"));
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation iOSで登録が呼ばれることを確認するテストケース */
    @Test
    void upsertInstallation_02() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            doNothing().when(hub).createOrUpdateInstallation(any());
        })) {
            assertDoesNotThrow(() -> util.upsertInstallation("inst-2", "2", "U2", "2", "token"));
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation 不正ブランドで何も行われないことを確認するテストケース */
    @Test
    void upsertInstallation_03() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            fail("NotificationHub should not be constructed for invalid brand");
        })) {
            assertDoesNotThrow(() -> util.upsertInstallation("inst-x", "9", "U9", "1", "token"));
        }
    }

    /**
     * クラス：NotificationHubUtil upsertInstallation
     * 不正プラットフォームでhub生成はされるがcreateOrUpdateInstallationが呼ばれないことを確認するテストケース
     */
    @Test
    void upsertInstallation_04() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            // もし呼ばれたら分かるようにスタブ（今回は呼ばれない想定）
            // doNothing().when(hub).createOrUpdateInstallation(any());
        })) {

            // 実行（platformCode="X"：不正）
            assertDoesNotThrow(() -> util.upsertInstallation("inst-3", "1", "U3", "X", "token"));

            // 確認：hub は生成されている（実装上、switch前に生成）
            assertEquals(1, nhc.constructed().size());

            // 確認：createOrUpdateInstallation は呼ばれていない
            NotificationHub constructed = nhc.constructed().get(0);
            verify(constructed, times(0)).createOrUpdateInstallation(any());
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation Hub例外が伝播することを確認するテストケース */
    @Test
    void upsertInstallation_05() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            // ★ コンストラクタに依存せず、モック例外を投げる
            doThrow(mock(NotificationHubsException.class))
                    .when(hub).createOrUpdateInstallation(any());
        })) {
            assertThrows(NotificationHubsException.class,
                    () -> util.upsertInstallation("inst-4", "1", "U4", "1", "token"));
        }
    }

    // --- deleteInstallation ---

    /** クラス：NotificationHubUtil deleteInstallation 正常に削除が呼ばれることを確認するテストケース */
    @Test
    void deleteInstallation_01() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            doNothing().when(hub).deleteInstallation(anyString());
        })) {
            assertDoesNotThrow(() -> util.deleteInstallation("inst-1", "1"));
        }
    }

    /** クラス：NotificationHubUtil deleteInstallation 不正ブランドで何も行われないことを確認するテストケース */
    @Test
    void deleteInstallation_02() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            fail("NotificationHub should not be constructed for invalid brand");
        })) {
            assertDoesNotThrow(() -> util.deleteInstallation("inst-x", "9"));
        }
    }

    /** クラス：NotificationHubUtil deleteInstallation Hub例外が伝播することを確認するテストケース */
    @Test
    void deleteInstallation_03() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            doThrow(mock(NotificationHubsException.class))
                    .when(hub).deleteInstallation(anyString());
        })) {
            assertThrows(NotificationHubsException.class,
                    () -> util.deleteInstallation("inst-err", "1"));
        }
    }

    // --- postMessage ---

    /** クラス：NotificationHubUtil postMessage Androidへ通知送信されることを確認するテストケース */
    @Test
    void postMessage_01() throws Exception {
        NotificationOutcome outcome = mock(NotificationOutcome.class);
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            when(hub.sendDirectNotification(any(), anyString())).thenReturn(outcome);
        })) {
            NotificationOutcome r = util.postMessage("inst-1", "{\"k\":\"v\"}", "1", "1");
            assertSame(outcome, r);
        }
    }

    /** クラス：NotificationHubUtil postMessage iOSへ通知送信されることを確認するテストケース */
    @Test
    void postMessage_02() throws Exception {
        NotificationOutcome outcome = mock(NotificationOutcome.class);
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            when(hub.sendDirectNotification(any(), anyString())).thenReturn(outcome);
        })) {
            NotificationOutcome r = util.postMessage("inst-2", "{\"k\":\"v\"}", "2", "2");
            assertSame(outcome, r);
        }
    }

    /** クラス：NotificationHubUtil postMessage 不正ブランドでnullとなることを確認するテストケース */
    @Test
    void postMessage_03() throws NotificationHubsException {
        assertNull(util.postMessage("inst-x", "{\"k\":\"v\"}", "9", "1"));
    }

    /** クラス：NotificationHubUtil postMessage 不正プラットフォームでnullとなることを確認するテストケース */
    @Test
    void postMessage_04() throws NotificationHubsException {
        assertNull(util.postMessage("inst-x", "{\"k\":\"v\"}", "1", "X"));
    }

    /** クラス：NotificationHubUtil postMessage Hub例外が伝播することを確認するテストケース */
    @Test
    void postMessage_05() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            when(hub.sendDirectNotification(any(), anyString()))
                    .thenThrow(mock(NotificationHubsException.class));
        })) {
            assertThrows(NotificationHubsException.class,
                    () -> util.postMessage("inst-e", "{\"k\":\"v\"}", "1", "1"));
        }
    }

    /**
     * クラス：NotificationHubUtil deleteInstallation
     * LEXUS分岐でHubが生成され削除APIが1回呼ばれることを確認するテストケース
     */
    @Test
    void deleteInstallation_04() throws Exception {
        // 準備：NotificationHub の生成と delete 呼び出しを監視
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            // 呼ばれるが、ここでは特に副作用無し
            doNothing().when(hub).deleteInstallation(anyString());
        })) {

            // 実行
            assertDoesNotThrow(() -> util.deleteInstallation("inst-lex-001", "2")); // BRD_LEXUS

            // 確認：Hub が1回生成され、deleteInstallation が1回呼ばれている
            assertEquals(1, nhc.constructed().size());
            NotificationHub constructed = nhc.constructed().get(0);
            verify(constructed, times(1)).deleteInstallation("inst-lex-001");
        }
    }

}