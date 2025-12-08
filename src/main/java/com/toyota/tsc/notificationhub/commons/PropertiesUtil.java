package com.toyota.tsc.notificationhub.commons;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PropertiesUtil {
    @Value("${personalinfo.api.url}")
    private String personalInfoApiUrl;
    @Value("${azure.notification-hub.api-version}")
    private String apiVersion;
    @Value("${azure.notification-hub.root-uri}")
    private String rootUri;
    @Value("${azure.notification-hub.installation-uri}")
    private String installationUri;
    @Value("${azure.notification-hub.sas.ttlSeconds}")
    private int ttlSeconds;
    @Value("${azure.notification-hub.http.timeoutMillis}")
    private int timeoutMillis;
    @Value("${azure.notification-hub.namespace-t}")
    private String namespaceT;
    @Value("${azure.notification-hub.hub-name-t}")
    private String hubNameT;
    @Value("${azure.notification-hub.shared-access-key-name-t}")
    private String keyNameT;
    @Value("${azure.notification-hub.shared-access-key-t}")
    private String keyT;
    @Value("${azure.notification-hub.namespace-l}")
    private String namespaceL;
    @Value("${azure.notification-hub.hub-name-l}")
    private String hubNameL;
    @Value("${azure.notification-hub.shared-access-key-name-l}")
    private String keyNameL;
    @Value("${azure.notification-hub.shared-access-key-l}")
    private String keyL;
    @Value("${azure.notification-hub.sas.template}")
    private String sasTemplate;
    @Value("${azure.notification-hub.installation.payload.template}")
    private String installationPayloadTemplate;
    @Value("${azure.notification-hub.sdk.connection-string}")
    private String sdkConnectionStringTemplate;
    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;
    @Value("${sendgrid.from.address-T}")
    private String fromAddressToyota;
    @Value("${sendgrid.from.name-T}")
    private String fromNameToyota;
    @Value("${sendgrid.from.address-L}")
    private String fromAddressLexus;
    @Value("${sendgrid.from.name-L}")
    private String fromNameLexus;
    @Value("${sms-country.user}")
    private String smsCountryUser;
    @Value("${sms-country.pass}")
    private String smsCountryPass;
    @Value("${sms-country.sender-id-t}")
    private String senderIdToyota;
    @Value("${sms-country.sender-id-l}")
    private String senderIdLexus;
    @Value("${sms-country.api-url}")
    private String smsCountryApiUrl;
    @Value("${azure.notification-hub.retry-count}")
    private int retryCount;

    public String getPersonalInfoApiUrl() {
        return personalInfoApiUrl;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getRootUri() {
        return rootUri;
    }

    public String getInstallationUri() {
        return installationUri;
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

    public String getKeyNameT() {
        return keyNameT;
    }

    public String getKeyT() {
        return keyT;
    }

    public String getNamespaceL() {
        return namespaceL;
    }

    public String getHubNameL() {
        return hubNameL;
    }

    public String getKeyNameL() {
        return keyNameL;
    }

    public String getKeyL() {
        return keyL;
    }

    public String getSasTemplate() {
        return sasTemplate;
    }

    public String getInstallationPayloadTemplate() {
        return installationPayloadTemplate;
    }

    public String getSdkConnectionStringTemplate() {
        return sdkConnectionStringTemplate;
    }

    public String getSendGridApiKey() {
        return sendGridApiKey;
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

    public String getSmsCountryUser() {
        return smsCountryUser;
    }

    public String getSmsCountryPass() {
        return smsCountryPass;
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

    public int getRetryCount() {
        return retryCount;
    }

}