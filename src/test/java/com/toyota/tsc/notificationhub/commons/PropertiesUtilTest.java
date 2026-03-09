
// ファイルパス: src/test/java/com/toyota/tsc/notificationhub/commons/PropertiesUtilTest.java
package com.toyota.tsc.notificationhub.commons;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PropertiesUtil のテストクラス
 */
@SuppressWarnings("all")
class PropertiesUtilTest {

    /** クラス：PropertiesUtil getKeyNameT 設定値が返ることを確認するテストケース */
    @Test
    void getKeyNameT_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("keyNameT");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getKeyNameT();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getKeyT 設定値が返ることを確認するテストケース */
    @Test
    void getKeyT_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("keyT");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getKeyT();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getKeyNameL 設定値が返ることを確認するテストケース */
    @Test
    void getKeyNameL_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("keyNameL");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getKeyNameL();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getKeyL 設定値が返ることを確認するテストケース */
    @Test
    void getKeyL_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("keyL");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getKeyL();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getPersonalInfoApiKey 設定値が返ることを確認するテストケース */
    @Test
    void getPersonalInfoApiKey_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("personalInfoApiKey");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getPersonalInfoApiKey();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getPersonalInfoListApiKey 設定値が返ることを確認するテストケース */
    @Test
    void getPersonalInfoListApiKey_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("personalInfoListApiKey");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getPersonalInfoListApiKey();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getRegisterNotificationApiKey 設定値が返ることを確認するテストケース */
    @Test
    void getRegisterNotificationApiKey_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("registerNotificationApiKey");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getRegisterNotificationApiKey();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getSilentPushLockeys 設定値が返ることを確認するテストケース */
    @Test
    void getSilentPushLockeys_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("silentPushLockeys");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getSilentPushLockeys();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getRegisterNotificationUrl 設定値が返ることを確認するテストケース */
    @Test
    void getRegisterNotificationUrl_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("registerNotificationUrl");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getRegisterNotificationUrl();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getSendGridApiKey 設定値が返ることを確認するテストケース */
    @Test
    void getSendGridApiKey_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("sendGridApiKey");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getSendGridApiKey();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getSmsCountryUser 設定値が返ることを確認するテストケース */
    @Test
    void getSmsCountryUser_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("smsCountryUser");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getSmsCountryUser();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getSmsCountryPass 設定値が返ることを確認するテストケース */
    @Test
    void getSmsCountryPass_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("smsCountryPass");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getSmsCountryPass();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getDatasourceUrl 設定値が返ることを確認するテストケース */
    @Test
    void getDatasourceUrl_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("datasourceUrl");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getDatasourceUrl();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getDatasourceUsername 設定値が返ることを確認するテストケース */
    @Test
    void getDatasourceUsername_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("datasourceUsername");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getDatasourceUsername();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getDatasourcePassword 設定値が返ることを確認するテストケース */
    @Test
    void getDatasourcePassword_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("datasourcePassword");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getDatasourcePassword();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getRetryCount 設定値が返ることを確認するテストケース */
    @Test
    void getRetryCount_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("retryCount");
        f.setAccessible(true);
        f.setInt(sut, 7);

        // Act
        int actual = sut.getRetryCount();

        // Assert
        assertEquals(7, actual);
    }

    /** クラス：PropertiesUtil getApiVersion 設定値が返ることを確認するテストケース */
    @Test
    void getApiVersion_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("apiVersion");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getApiVersion();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getInstallationUri 設定値が返ることを確認するテストケース */
    @Test
    void getInstallationUri_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("installationUri");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getInstallationUri();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getRootUri 設定値が返ることを確認するテストケース */
    @Test
    void getRootUri_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("rootUri");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getRootUri();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getTtlSeconds 設定値が返ることを確認するテストケース */
    @Test
    void getTtlSeconds_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("ttlSeconds");
        f.setAccessible(true);
        f.setInt(sut, 60);

        // Act
        int actual = sut.getTtlSeconds();

        // Assert
        assertEquals(60, actual);
    }

    /** クラス：PropertiesUtil getTimeoutMillis 設定値が返ることを確認するテストケース */
    @Test
    void getTimeoutMillis_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("timeoutMillis");
        f.setAccessible(true);
        f.setInt(sut, 1234);

        // Act
        int actual = sut.getTimeoutMillis();

        // Assert
        assertEquals(1234, actual);
    }

    /** クラス：PropertiesUtil getNamespaceT 設定値が返ることを確認するテストケース */
    @Test
    void getNamespaceT_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("namespaceT");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getNamespaceT();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getHubNameT 設定値が返ることを確認するテストケース */
    @Test
    void getHubNameT_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("hubNameT");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getHubNameT();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getNamespaceL 設定値が返ることを確認するテストケース */
    @Test
    void getNamespaceL_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("namespaceL");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getNamespaceL();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getHubNameL 設定値が返ることを確認するテストケース */
    @Test
    void getHubNameL_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("hubNameL");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getHubNameL();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getSasTemplate 設定値が返ることを確認するテストケース */
    @Test
    void getSasTemplate_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("sasTemplate");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getSasTemplate();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getSdkConnectionStringTemplate 設定値が返ることを確認するテストケース */
    @Test
    void getSdkConnectionStringTemplate_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("sdkConnectionStringTemplate");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getSdkConnectionStringTemplate();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getPersonalInfoListApiUrl 設定値が返ることを確認するテストケース */
    @Test
    void getPersonalInfoListApiUrl_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("personalInfoListApiUrl");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getPersonalInfoListApiUrl();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getParallelCurrent 設定値が返ることを確認するテストケース */
    @Test
    void getParallelCurrent_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("parallelCurrent");
        f.setAccessible(true);
        f.setInt(sut, 5);

        // Act
        int actual = sut.getParallelCurrent();

        // Assert
        assertEquals(5, actual);
    }

    /** クラス：PropertiesUtil getJsapGetUserIdApiUrl 設定値が返ることを確認するテストケース */
    @Test
    void getJsapGetUserIdApiUrl_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("jsapGetUserIdApiUrl");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getJsapGetUserIdApiUrl();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getJsapGetUserInfoApiUrl 設定値が返ることを確認するテストケース */
    @Test
    void getJsapGetUserInfoApiUrl_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("jsapGetUserInfoApiUrl");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getJsapGetUserInfoApiUrl();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getJsapDvcLinkApiUrl 設定値が返ることを確認するテストケース */
    @Test
    void getJsapDvcLinkApiUrl_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("jsapDvcLinkApiUrl");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getJsapDvcLinkApiUrl();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getJsapNotificationApiUrl 設定値が返ることを確認するテストケース */
    @Test
    void getJsapNotificationApiUrl_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("jsapNotificationApiUrl");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getJsapNotificationApiUrl();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getNtfinfoUpsertRetryCount 設定値が返ることを確認するテストケース */
    @Test
    void getNtfinfoUpsertRetryCount_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("ntfinfoUpsertRetryCount");
        f.setAccessible(true);
        f.setInt(sut, 3);

        // Act
        int actual = sut.getNtfinfoUpsertRetryCount();

        // Assert
        assertEquals(3, actual);
    }

    /** クラス：PropertiesUtil getNtfinfoUpsertRetryBaseInterval 設定値が返ることを確認するテストケース */
    @Test
    void getNtfinfoUpsertRetryBaseInterval_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("ntfinfoUpsertRetryBaseInterval");
        f.setAccessible(true);
        f.setInt(sut, 10);

        // Act
        int actual = sut.getNtfinfoUpsertRetryBaseInterval();

        // Assert
        assertEquals(10, actual);
    }

    /** クラス：PropertiesUtil getNtfinfoUpsertRetryMaxInterval 設定値が返ることを確認するテストケース */
    @Test
    void getNtfinfoUpsertRetryMaxInterval_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("ntfinfoUpsertRetryMaxInterval");
        f.setAccessible(true);
        f.setInt(sut, 99);

        // Act
        int actual = sut.getNtfinfoUpsertRetryMaxInterval();

        // Assert
        assertEquals(99, actual);
    }

    /** クラス：PropertiesUtil getPersonalInfoApiUrl 設定値が返ることを確認するテストケース */
    @Test
    void getPersonalInfoApiUrl_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("personalInfoApiUrl");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getPersonalInfoApiUrl();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getFromAddressToyota 設定値が返ることを確認するテストケース */
    @Test
    void getFromAddressToyota_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("fromAddressToyota");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getFromAddressToyota();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getFromNameToyota 設定値が返ることを確認するテストケース */
    @Test
    void getFromNameToyota_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("fromNameToyota");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getFromNameToyota();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getFromAddressLexus 設定値が返ることを確認するテストケース */
    @Test
    void getFromAddressLexus_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("fromAddressLexus");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getFromAddressLexus();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getFromNameLexus 設定値が返ることを確認するテストケース */
    @Test
    void getFromNameLexus_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("fromNameLexus");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getFromNameLexus();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getSenderIdToyota 設定値が返ることを確認するテストケース */
    @Test
    void getSenderIdToyota_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("senderIdToyota");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getSenderIdToyota();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getSenderIdLexus 設定値が返ることを確認するテストケース */
    @Test
    void getSenderIdLexus_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("senderIdLexus");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getSenderIdLexus();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getSmsCountryApiUrl 設定値が返ることを確認するテストケース */
    @Test
    void getSmsCountryApiUrl_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("smsCountryApiUrl");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getSmsCountryApiUrl();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getThreadPool 設定値が返ることを確認するテストケース */
    @Test
    void getThreadPool_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("threadPool");
        f.setAccessible(true);
        f.setInt(sut, 10);

        // Act
        int actual = sut.getThreadPool();

        // Assert
        assertEquals(10, actual);
    }

    /** クラス：PropertiesUtil getThreadQueue 設定値が返ることを確認するテストケース */
    @Test
    void getThreadQueue_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("threadQueue");
        f.setAccessible(true);
        f.setInt(sut, 20);

        // Act
        int actual = sut.getThreadQueue();

        // Assert
        assertEquals(20, actual);
    }
}
