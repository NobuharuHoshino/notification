
package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.SmsCountryUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendSmsRequestDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

@Service
public class SendSmsServiceImpl implements SendSmsServiceIF {

    @Value("${sms-country.api-url}")
    private String smsCountryApiUrl;

    private static final String PROCCESS_NAME = "SMS送信要求";

    @Autowired
    SmsCountryUtil smsCountryUtil;

    @Override
    public String sendSms(SendSmsRequestDto request, RequestHeaderDto header) {

        try {

            // 開始ログ
            LogUtil.info(SendSmsServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            validate(request, header);

            // SMS送信リクエスト作成
            HttpEntity<String> entity = smsCountryUtil.createRequest(
                    CommonUtil.normalizePhoneNumber(request.getMobileNumber()), request.getBody_sms(),
                    request.getBrdCd());

            // Send
            LogUtil.info(SendSmsServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00009", request.getBrdCd(), CommonUtil.maskPhoneNumber(request.getMobileNumber()),
                    header.getCorrelationId()));
            smsCountryUtil.sendSmsCountry(entity, request.getMobileNumber());
            LogUtil.info(SendSmsServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00010", request.getBrdCd(), CommonUtil.maskPhoneNumber(request.getMobileNumber()),
                    header.getCorrelationId()));

            String resultCode = CommonUtil.getResultCode("SUCCESS");

            // 正常終了ログ
            LogUtil.info(SendSmsServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));
            return resultCode;

        } catch (TscSMSException e) {
            LogUtil.error(SendSmsServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00006", e.getStatusCode(), CommonUtil.maskPhoneNumber(request.getMobileNumber()),
                    header.getCorrelationId()));
            throw new RuntimeException();

        } catch (Exception e) {
            if (e instanceof TscApplicationException) {
                throw new TscApplicationException();

            } else {
                LogUtil.error(SendSmsServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new RuntimeException();
            }
        }
    }

    // #region Validation Methods
    /**
     * リクエスト内容の検証処理を呼び出します。
     * 
     * @param request
     * @param header
     * @return
     */
    private String validate(SendSmsRequestDto request, RequestHeaderDto header) {
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(SendSmsServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00012", missingField, header.getCorrelationId()));
            throw new TscApplicationException();
        }
        if (!isValidBrdCd(request.getBrdCd())) {
            LogUtil.error(SendSmsServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00008", request.getBrdCd(), header.getCorrelationId()));
            throw new TscApplicationException();
        }
        return null;
    }

    /**
     * リクエスト内容の必須項目検証を行います。
     * 
     * @return 必須エラーの項目名（Swagger定義の項目名）、またはnull（エラーなし）
     */
    private String validateRequired(SendSmsRequestDto request) {
        if (request == null) {
            return "requestBody";
        }
        List<String> missingFields = new ArrayList<>();
        if (request.getBrdCd() == null || request.getBrdCd().isEmpty()) {
            missingFields.add("brdCd");
        }
        if (request.getMobileNumber() == null || request.getMobileNumber().isEmpty()) {
            missingFields.add("mobileNumber");
        }
        if (request.getBody_sms() == null || request.getBody_sms().isEmpty()) {
            missingFields.add("body_sms");
        }

        if (!missingFields.isEmpty()) {
            return String.join(",", missingFields);
        }
        return null;
    }

    /**
     * brdCdの妥当性検証を行います。
     * 
     * @return true: トヨタ(1)またはレクサス(2)、false: その他の非対応ブランド
     */
    private boolean isValidBrdCd(String brdCd) {
        return brdCd.equals("1") || brdCd.equals("2");
    }
    // #endregion
}
