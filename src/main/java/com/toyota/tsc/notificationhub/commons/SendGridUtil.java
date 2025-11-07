package com.toyota.tsc.notificationhub.commons;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;

@Component
public class SendGridUtil {

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

    private static final String BRD_TOYOTA = "1";
    private static final String BRD_LEXUS = "2";

    public Mail generateEmail(
            String toAddress, String title, String body_text, String body_html, String brdCd) {
        String mailFrom;
        String mailFromName;
        if (brdCd.equals(BRD_TOYOTA)) {
            mailFrom = fromAddressToyota;
            mailFromName = fromNameToyota;
        } else if (brdCd.equals(BRD_LEXUS)) {
            mailFrom = fromAddressLexus;
            mailFromName = fromNameLexus;
        } else {
            return null; // バリデーションチェックしているため、対応ブランド以外は到達しない想定。
        }

        Email from = new Email(mailFrom, mailFromName);
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(toAddress));
        Content contentText = new Content("text/plain", body_text);
        Content contentHtml = new Content("text/html", body_html);

        // Mail Object生成
        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(title);
        mail.addPersonalization(personalization);
        mail.addContent(contentText);
        mail.addContent(contentHtml);

        return mail;
    }

    public void executeSendEmail(Mail mail) {
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request sgRequest = new Request();
        try {
            sgRequest.setMethod(Method.POST);
            sgRequest.setEndpoint("mail/send");
            sgRequest.setBody(mail.build());
            Response response = sg.api(sgRequest);
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                throw new TscEMailException(response.getStatusCode(), response.getBody(),
                        mail.getPersonalization().get(0).getTos().get(0).getEmail());
            }
        } catch (TscEMailException sgEx) {
            throw new TscEMailException(sgEx.getStatusCode(), sgEx.getResponseBody(), sgEx.getAddress());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
