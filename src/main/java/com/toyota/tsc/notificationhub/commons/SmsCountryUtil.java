package com.toyota.tsc.notificationhub.commons;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.toyota.tsc.notificationhub.exceptions.CustomException;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * SMS送信（SmsCountry）ユーティリティクラス
 */
@Component
public class SmsCountryUtil {

    private static final String BRD_INVALID = "0";
    private static final String BRD_TOYOTA = "1";
    private static final String BRD_LEXUS = "2";

    private PropertiesUtil propertiesUtil;

    public SmsCountryUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    public ResponseEntity<String> executeSendSms(String phoneNo, String msg, String brdCd) {
        String senderId;
        try {
            if (BRD_TOYOTA.equals(brdCd) || brdCd.equals(BRD_INVALID)) {
                senderId = propertiesUtil.getSenderIdToyota();
            } else if (BRD_LEXUS.equals(brdCd)) {
                senderId = propertiesUtil.getSenderIdLexus();
            } else {
                return null;
            }

            URI uri = UriComponentsBuilder
                    .fromUriString(propertiesUtil.getSmsCountryApiUrl())
                    .queryParam("User", propertiesUtil.getSmsCountryUser())
                    .queryParam("passwd", propertiesUtil.getSmsCountryPass())
                    .queryParam("mobilenumber", phoneNo)
                    .queryParam("message", msg)
                    .queryParam("sid", senderId)
                    .queryParam("mtype", "N")
                    .queryParam("DR", "N")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUri();

            LogUtil.info(getClass(), uri.toString());

            return new RestTemplate().getForEntity(uri, String.class);

        } catch (HttpStatusCodeException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }
}