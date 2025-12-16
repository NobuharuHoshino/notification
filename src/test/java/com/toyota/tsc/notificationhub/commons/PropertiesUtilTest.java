
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

    /** クラス：PropertiesUtil getJsapGetUserIdApiKey 設定値が返ることを確認するテストケース */
    @Test
    void getJsapGetUserIdApiKey_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("jsapGetUserIdApiKey");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getJsapGetUserIdApiKey();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getJsapGetUserInfoApiKey 設定値が返ることを確認するテストケース */
    @Test
    void getJsapGetUserInfoApiKey_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("jsapGetUserInfoApiKey");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getJsapGetUserInfoApiKey();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getJsapDvcLinkApiKey 設定値が返ることを確認するテストケース */
    @Test
    void getJsapDvcLinkApiKey_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("jsapDvcLinkApiKey");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getJsapDvcLinkApiKey();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getJsapNotificationApiKey 設定値が返ることを確認するテストケース */
    @Test
    void getJsapNotificationApiKey_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("jsapNotificationApiKey");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getJsapNotificationApiKey();

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

    /** クラス：PropertiesUtil getDatasourceDriverClassName 設定値が返ることを確認するテストケース */
    @Test
    void getDatasourceDriverClassName_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("datasourceDriverClassName");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getDatasourceDriverClassName();

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

    /** クラス：PropertiesUtil getInstallationPayloadTemplate 設定値が返ることを確認するテストケース */
    @Test
    void getInstallationPayloadTemplate_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("installationPayloadTemplate");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getInstallationPayloadTemplate();

        // Assert
        assertEquals("v", actual);
    }

    /** クラス：PropertiesUtil getPayloadTemplate 設定値が返ることを確認するテストケース */
    @Test
    void getPayloadTemplate_001() throws Exception {
        // Arrange
        PropertiesUtil sut = new PropertiesUtil();
        Field f = PropertiesUtil.class.getDeclaredField("payloadTemplate");
        f.setAccessible(true);
        f.set(sut, "v");

        // Act
        String actual = sut.getPayloadTemplate();

        // Assert
        assertEquals("v", actual);
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
}
