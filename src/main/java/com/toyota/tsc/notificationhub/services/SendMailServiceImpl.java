
package com.toyota.tsc.notificationhub.services;

import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.SendGridUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendMailRequestDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SendMailServiceImpl implements SendMailServiceIF {

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

    private static final String PROCCESS_NAME = "メール送信";

    @Autowired
    private SendGridUtil sendGridUtil;

    @Override
    public String sendMail(SendMailRequestDto request, RequestHeaderDto header) {

        try {
            // 開始ログ
            LogUtil.info(SendMailServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, CommonUtil.toJson(request), header.getCorrelationId()));

            // リクエスト検証
            String validateResult = validate(request, header);
            if (validateResult != null) {
                return validateResult;
            }

            // メール生成
            Mail mail = sendGridUtil.generateEmail(
                    request.getEmailAddress(), request.getTitle(), request.getBody_text(),
                    request.getBody_html(), request.getBrdCd());

            // メール送信実行
            LogUtil.info(SendMailServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00011", request.getBrdCd(), CommonUtil.maskText(request.getEmailAddress()),
                    request.getTitle(), header.getCorrelationId()));
            sendGridUtil.executeSendEmail(mail);
            LogUtil.info(SendMailServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00012", request.getBrdCd(), CommonUtil.maskText(request.getEmailAddress()),
                    request.getTitle(), header.getCorrelationId()));

            String resultCode = CommonUtil.getResultCode("SUCCESS");

            // 正常終了ログ
            LogUtil.info(SendMailServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));
            return resultCode;

        } catch (TscEMailException e) {
            LogUtil.error(SendMailServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00007", e.getStatusCode(), CommonUtil.maskText(request.getEmailAddress()),
                    request.getTitle(), header.getCorrelationId()));
            throw new RuntimeException();

        } catch (Exception e) {

            if (e instanceof TscApplicationException) {
                throw new TscApplicationException();

            } else {
                LogUtil.error(SendMailServiceImpl.class, CommonUtil.getMessage(
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
    private String validate(SendMailRequestDto request, RequestHeaderDto header) {
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(SendMailServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00012", missingField, header.getCorrelationId()));
            throw new TscApplicationException();
        }
        if (!isValidBrdCd(request.getBrdCd())) {
            LogUtil.error(SendMailServiceImpl.class, CommonUtil.getMessage(
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
    private String validateRequired(SendMailRequestDto request) {
        if (request == null) {
            return "requestBody";
        }
        List<String> missingFields = new ArrayList<>();
        if (request.getBrdCd() == null || request.getBrdCd().isEmpty()) {
            missingFields.add("brdCd");
        }
        if (request.getEmailAddress() == null || request.getEmailAddress().isEmpty()) {
            missingFields.add("to");
        }
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            missingFields.add("title");
        }
        if (request.getBody_text() == null || request.getBody_text().isEmpty()) {
            missingFields.add("body_text");
        }
        if (request.getBody_html() == null || request.getBody_html().isEmpty()) {
            missingFields.add("body_html");
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
