
package com.toyota.tsc.notificationhub.commons;

import com.windowsazure.messaging.NotificationHubsException;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.windowsazure.messaging.NotificationHub;
import com.windowsazure.messaging.NotificationOutcome;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * クラス：NotificationHubUtil すべての分岐を確認するテストケース
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationHubUtilTest {

    @InjectMocks
    private NotificationHubUtil util;

    @Mock
    private PropertiesUtil propsMock;

    @BeforeEach
    void setup() throws Exception {
        // --- PropertiesUtil をモック ---
        propsMock = mock(PropertiesUtil.class);

        // 共通テンプレート（null だと NPE になるため必須）
        when(propsMock.getRootUri()).thenReturn("https://%s.servicebus.windows.net/%s");
        when(propsMock.getSasTemplate()).thenReturn("SharedAccessSignature sr=%s&sig=%s&se=%d&skn=%s");
        when(propsMock.getSdkConnectionStringTemplate())
                .thenReturn("Endpoint=sb://%s.servicebus.windows.net/;SharedAccessKeyName=%s;SharedAccessKey=%s");
        when(propsMock.getTtlSeconds()).thenReturn(3600);

        // TOYOTA（BRD_TOYOTA = "1"）
        when(propsMock.getNamespaceT()).thenReturn("ns-toyota");
        when(propsMock.getHubNameT()).thenReturn("hub-toyota");
        when(propsMock.getKeyNameT()).thenReturn("DefaultFullSharedAccessSignature");
        when(propsMock.getKeyT()).thenReturn("dummy-key-toyota");

        // LEXUS（BRD_LEXUS = "2"）
        when(propsMock.getNamespaceL()).thenReturn("ns-lexus");
        when(propsMock.getHubNameL()).thenReturn("hub-lexus");
        when(propsMock.getKeyNameL()).thenReturn("DefaultFullSharedAccessSignature");
        when(propsMock.getKeyL()).thenReturn("dummy-key-lexus");

        // NotificationHubUtil の private フィールドへモックを挿入
        setField(util, NotificationHubUtil.class, "propertiesUtil", propsMock);
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
        assertTrue(token.contains("skn=DefaultFullSharedAccessSignature"));
        assertTrue(token.contains("sr=")); // 対象URI
        assertTrue(token.contains("sig=")); // 署名
        assertTrue(token.contains("se=")); // 期限
    }

    /** クラス：NotificationHubUtil generateSasToken LEXUSブランドの生成を確認するテストケース */
    @Test
    void generateSasToken_02() {
        String token = util.generateSasToken("2");
        assertNotNull(token);
        assertTrue(token.startsWith("SharedAccessSignature "));
        assertTrue(token.contains("skn=DefaultFullSharedAccessSignature"));
    }

    /** クラス：NotificationHubUtil generateSasToken 不正ブランドでnullとなることを確認するテストケース */
    @Test
    void generateSasToken_03() {
        assertNull(util.generateSasToken("9"));
    }

    /**
     * クラス：NotificationHubUtil generateSasToken
     * キー未設定で CustomException が投げられることを確認するテストケース
     */
    @Test
    void generateSasToken_04() throws Exception {
        // TOYOTAキーを null に差し替え
        when(propsMock.getKeyT()).thenReturn(null);

        // 例外型を CustomException に変更（cause は NullPointerException が望ましい）
        CustomException ex = assertThrows(CustomException.class, () -> util.generateSasToken("1"));
        assertTrue(ex.getCause() instanceof NullPointerException);
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
     * 不正プラットフォームで hub 生成はされるが createOrUpdateInstallation が呼ばれないことを確認
     */
    @Test
    void upsertInstallation_04() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            // 呼ばれない想定のためスタブ不要
        })) {
            assertDoesNotThrow(() -> util.upsertInstallation("inst-3", "1", "U3", "X", "token"));
            assertEquals(1, nhc.constructed().size());
            NotificationHub constructed = nhc.constructed().get(0);
            verify(constructed, times(0)).createOrUpdateInstallation(any());
        }
    }

    /** クラス：NotificationHubUtil upsertInstallation Hub例外が伝播することを確認するテストケース */
    @Test
    void upsertInstallation_05() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
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

    /**
     * クラス：NotificationHubUtil deleteInstallation
     * LEXUS分岐でHubが生成され削除APIが1回呼ばれることを確認するテストケース
     */
    @Test
    void deleteInstallation_04() throws Exception {
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            doNothing().when(hub).deleteInstallation(anyString());
        })) {
            assertDoesNotThrow(() -> util.deleteInstallation("inst-lex-001", "2")); // BRD_LEXUS
            assertEquals(1, nhc.constructed().size());
            NotificationHub constructed = nhc.constructed().get(0);
            verify(constructed, times(1)).deleteInstallation("inst-lex-001");
        }
    }

    // --- postMessage ---

    /** クラス：NotificationHubUtil postMessage Androidへ通知送信されることを確認するテストケース */
    @Test
    void postMessage_01() throws Exception {
        NotificationOutcome outcome = mock(NotificationOutcome.class);
        try (MockedConstruction<NotificationHub> nhc = Mockito.mockConstruction(NotificationHub.class, (hub, ctx) -> {
            when(hub.sendNotification(any(), anyString())).thenReturn(outcome);
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
            when(hub.sendNotification(any(), anyString())).thenReturn(outcome);
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
            when(hub.sendNotification(any(), anyString()))
                    .thenThrow(mock(NotificationHubsException.class));
        })) {
            assertThrows(NotificationHubsException.class,
                    () -> util.postMessage("inst-e", "{\"k\":\"v\"}", "1", "1"));
        }
    }
}
