
package com.toyota.tsc.notificationhub.commons;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertiesUtil {

    // ===== Secret Configuration (Key Vault values may override) =====
    @Value("${azure.notification-hub.shared-access-key-name-t}")
    private String keyNameT;
    @Value("${azure.notification-hub.shared-access-key-t}")
    private String keyT;
    @Value("${azure.notification-hub.shared-access-key-name-l}")
    private String keyNameL;
    @Value("${azure.notification-hub.shared-access-key-l}")
    private String keyL;

    @Value("${personalinfo.api.key}")
    private String personalInfoApiKey;
    @Value("${personalinfolist.api.key}")
    private String personalInfoListApiKey;
    @Value("${registernotification.api.key}")
    private String registerNotificationApiKey;
    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Value("${ntf-silent-push-lockeys}")
    private String silentPushLockeys;

    @Value("${sms-country.user}")
    private String smsCountryUser;
    @Value("${sms-country.pass}")
    private String smsCountryPass;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.username}")
    private String datasourceUsername;
    @Value("${spring.datasource.password}")
    private String datasourcePassword;
    @Value("${thread.pool}")
    private int threadPool;
    @Value("${thread.queue}")
    private int threadQueue;

    // ===== Runtime Configuration =====
    @Value("${azure.notification-hub.retry-count}")
    private int retryCount;
    @Value("${azure.notification-hub.api-version}")
    private String apiVersion;
    @Value("${azure.notification-hub.installation-uri}")
    private String installationUri;
    @Value("${azure.notification-hub.root-uri}")
    private String rootUri;
    @Value("${azure.notification-hub.sas.ttlSeconds}")
    private int ttlSeconds;
    @Value("${azure.notification-hub.http.timeoutMillis}")
    private int timeoutMillis;
    @Value("${azure.notification-hub.namespace-t}")
    private String namespaceT;
    @Value("${azure.notification-hub.hub-name-t}")
    private String hubNameT;
    @Value("${azure.notification-hub.namespace-l}")
    private String namespaceL;
    @Value("${azure.notification-hub.hub-name-l}")
    private String hubNameL;
    @Value("${azure.notification-hub.sas.template}")
    private String sasTemplate;
    @Value("${azure.notification-hub.sdk.connection-string}")
    private String sdkConnectionStringTemplate;
    @Value("${jsap.getuserid.api.url}")
    private String jsapGetUserIdApiUrl;
    @Value("${jsap.getuserinfo.api.url}")
    private String jsapGetUserInfoApiUrl;
    @Value("${jsap.dvclink.api.url}")
    private String jsapDvcLinkApiUrl;
    @Value("${jsap.notification.api.url}")
    private String jsapNotificationApiUrl;
    @Value("${ntfinfo.upsert.retry.count}")
    private int ntfinfoUpsertRetryCount;
    @Value("${ntfinfo.upsert.retry.base-interval}")
    private int ntfinfoUpsertRetryBaseInterval;
    @Value("${ntfinfo.upsert.retry.max-interval}")
    private int ntfinfoUpsertRetryMaxInterval;
    @Value("${personalinfo.api.url}")
    private String personalInfoApiUrl;
    @Value("${personalinfolist.api.url}")
    private String personalInfoListApiUrl;
    @Value("${registernotification.url}")
    private String registerNotificationUrl;
    @Value("${sendgrid.from.address-T}")
    private String fromAddressToyota;
    @Value("${sendgrid.from.name-T}")
    private String fromNameToyota;
    @Value("${sendgrid.from.address-L}")
    private String fromAddressLexus;
    @Value("${sendgrid.from.name-L}")
    private String fromNameLexus;
    @Value("${sms-country.sender-id-t}")
    private String senderIdToyota;
    @Value("${sms-country.sender-id-l}")
    private String senderIdLexus;
    @Value("${sms-country.api-url}")
    private String smsCountryApiUrl;
    @Value("${parallel.current}")
    private int parallelCurrent;

    // Secret Configuration
    public String getKeyNameT() {
        return keyNameT;
    }

    public String getKeyT() {
        return keyT;
    }

    public String getKeyNameL() {
        return keyNameL;
    }

    public String getKeyL() {
        return keyL;
    }

    public String getPersonalInfoApiKey() {
        return personalInfoApiKey;
    }

    public String getPersonalInfoListApiKey() {
        return personalInfoListApiKey;
    }

    public String getRegisterNotificationApiKey() {
        return registerNotificationApiKey;
    }

    public String getSendGridApiKey() {
        return sendGridApiKey;
    }

    public String getSilentPushLockeys() {
        return silentPushLockeys;
    }

    public String getSmsCountryUser() {
        return smsCountryUser;
    }

    public String getSmsCountryPass() {
        return smsCountryPass;
    }

    public String getDatasourceUrl() {
        return datasourceUrl;
    }

    public String getDatasourceUsername() {
        return datasourceUsername;
    }

    public String getDatasourcePassword() {
        return datasourcePassword;
    }

    // Runtime Configuration
    public int getRetryCount() {
        return retryCount;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getInstallationUri() {
        return installationUri;
    }

    public String getRootUri() {
        return rootUri;
    }

    public int getTtlSeconds() {
        return ttlSeconds;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public String getNamespaceT() {
        return namespaceT;
    }

    public String getHubNameT() {
        return hubNameT;
    }

    public String getNamespaceL() {
        return namespaceL;
    }

    public String getHubNameL() {
        return hubNameL;
    }

    public String getSasTemplate() {
        return sasTemplate;
    }

    public String getSdkConnectionStringTemplate() {
        return sdkConnectionStringTemplate;
    }

    public String getJsapGetUserIdApiUrl() {
        return jsapGetUserIdApiUrl;
    }

    public String getJsapGetUserInfoApiUrl() {
        return jsapGetUserInfoApiUrl;
    }

    public String getJsapDvcLinkApiUrl() {
        return jsapDvcLinkApiUrl;
    }

    public String getJsapNotificationApiUrl() {
        return jsapNotificationApiUrl;
    }

    public int getNtfinfoUpsertRetryCount() {
        return ntfinfoUpsertRetryCount;
    }

    public int getNtfinfoUpsertRetryBaseInterval() {
        return ntfinfoUpsertRetryBaseInterval;
    }

    public int getNtfinfoUpsertRetryMaxInterval() {
        return ntfinfoUpsertRetryMaxInterval;
    }

    public String getPersonalInfoApiUrl() {
        return personalInfoApiUrl;
    }

    public String getPersonalInfoListApiUrl() {
        return personalInfoListApiUrl;
    }

    public String getRegisterNotificationUrl() {
        return registerNotificationUrl;
    }

    public String getFromAddressToyota() {
        return fromAddressToyota;
    }

    public String getFromNameToyota() {
        return fromNameToyota;
    }

    public String getFromAddressLexus() {
        return fromAddressLexus;
    }

    public String getFromNameLexus() {
        return fromNameLexus;
    }

    public String getSenderIdToyota() {
        return senderIdToyota;
    }

    public String getSenderIdLexus() {
        return senderIdLexus;
    }

    public String getSmsCountryApiUrl() {
        return smsCountryApiUrl;
    }

    public int getParallelCurrent() {
        return parallelCurrent;
    }

    public int getThreadPool() {
        return threadPool;
    }

    public int getThreadQueue() {
        return threadQueue;
    }
}