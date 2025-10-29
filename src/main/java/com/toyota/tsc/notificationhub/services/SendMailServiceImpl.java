package com.toyota.tsc.notificationhub.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.toyota.tsc.notificationhub.models.SendMailRequestDto;
import com.sendgrid.SendGrid;
import com.sendgrid.Request;
import com.sendgrid.Method;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;

import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;

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

    private final String BRD_TOYOTA = "1";
    private final String BRD_LEXUS = "2";
    private static final String PROCCESS_NAME = "メール送信";

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
            Mail mail = generateEmail(request, header);
            // メール送信実行
            executeSendEmail(request, header, mail);
            String resultCode = CommonUtil.getResultCode("SUCCESS");
            // 正常終了ログ
            LogUtil.info(SendMailServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));
            return resultCode;
        } catch (Exception e) {
            if (e instanceof TscApplicationException) {
                throw new TscApplicationException();
            } else if (e instanceof TscEMailException) {
                LogUtil.error(SendMailServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00007", "StatusCode", CommonUtil.maskText(request.getEmailAddress()),
                        request.getTitle(), header.getCorrelationId()));
                throw new TscEMailException("Failed to send email");
            } else {
                LogUtil.error(
                        RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                                "RS07E0000", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new RuntimeException();
            }
        }
    }

    private void executeSendEmail(SendMailRequestDto request, RequestHeaderDto header, Mail mail) {
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request sgRequest = new Request();
        try {
            LogUtil.info(SendMailServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00011", request.getBrdCd(), CommonUtil.maskText(request.getEmailAddress()),
                    request.getTitle(), header.getCorrelationId()));
            sgRequest.setMethod(Method.POST);
            sgRequest.setEndpoint("mail/send");
            sgRequest.setBody(mail.build());
            sg.api(sgRequest);
        } catch (IOException sgEx) {
            throw new TscEMailException("Failed to send email", sgEx);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Mail generateEmail(SendMailRequestDto request, RequestHeaderDto header) {
        String mailFrom;
        String mailFromName;
        if (request.getBrdCd().equals(BRD_TOYOTA)) {
            mailFrom = fromAddressToyota;
            mailFromName = fromNameToyota;
        } else if (request.getBrdCd().equals(BRD_LEXUS)) {
            mailFrom = fromAddressLexus;
            mailFromName = fromNameLexus;
        } else {
            return null; // バリデーションチェックしているため、対応ブランド以外は到達しない想定。
        }

        Email from = new Email(mailFrom, mailFromName);
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(request.getEmailAddress()));
        Content contentText = new Content("text/plain", request.getBody_text());
        Content contentHtml = new Content("text/html", request.getBody_html());

        // Mail Object生成
        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(request.getTitle());
        mail.addPersonalization(personalization);
        mail.addContent(contentText);
        mail.addContent(contentHtml);

        return mail;
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
            LogUtil.error(
                    SendMailServiceImpl.class,
                    CommonUtil.getMessage(
                            "RS07E00012",
                            missingField,
                            header.getCorrelationId()));
            throw new TscApplicationException("Missing required field: " + missingField);
        }
        if (!isValidBrdCd(request.getBrdCd())) {
            LogUtil.error(
                    SendMailServiceImpl.class,
                    CommonUtil.getMessage(
                            "RS07E00008",
                            request.getBrdCd(),
                            header.getCorrelationId()));
            throw new TscApplicationException("Invalid brdCd: " + request.getBrdCd());
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
