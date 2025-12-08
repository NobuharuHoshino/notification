package com.toyota.tsc.notificationhub.commons;

import org.springframework.stereotype.Component;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;

/**
 * SendGridメール送信ユーティリティクラス
 */
@Component
public class SendGridUtil {

    private static final String BRD_INVALID = "0";
    private static final String BRD_TOYOTA = "1";
    private static final String BRD_LEXUS = "2";

    private PropertiesUtil propertiesUtil;

    public SendGridUtil(PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    /**
     * メール送信用Mailオブジェクトを生成します。
     * 
     * @param toAddress 送信先メールアドレス
     * @param title     メールタイトル
     * @param body_text テキスト本文
     * @param body_html HTML本文
     * @param brdCd     ブランドコード
     * @return Mailオブジェクト（失敗時はnull）
     */
    public Mail generateEmail(
            String toAddress, String title, String bodyText, String bodyHtml, String brdCd) {
        String mailFrom;
        String mailFromName;
        if (brdCd.equals(BRD_TOYOTA) || brdCd.equals(BRD_INVALID)) {
            mailFrom = propertiesUtil.getFromAddressToyota();
            mailFromName = propertiesUtil.getFromNameToyota();
        } else if (brdCd.equals(BRD_LEXUS)) {
            mailFrom = propertiesUtil.getFromAddressLexus();
            mailFromName = propertiesUtil.getFromNameLexus();
        } else {
            return null; // バリデーションチェックしているため、対応ブランド以外は到達しない想定。
        }

        Email from = new Email(mailFrom, mailFromName);
        Personalization personalization = new Personalization();
        personalization.addTo(new Email(toAddress));

        bodyText = bodyText == null || bodyText.isEmpty() ? " " : bodyText;
        Content contentText = new Content("text/plain", bodyText);

        Content contentHtml = null;
        if (bodyHtml != null && !bodyHtml.isEmpty()) {
            contentHtml = new Content("text/html", bodyHtml);
        }

        // Mail Object生成
        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(title);
        mail.addPersonalization(personalization);
        mail.addContent(contentText);
        if (contentHtml != null) {
            mail.addContent(contentHtml);
        }

        return mail;
    }

    /**
     * メール送信処理を実行します。
     * 
     * @param mail 送信対象Mailオブジェクト
     * @return なし
     */
    public void executeSendEmail(Mail mail) {
        SendGrid sg = new SendGrid(propertiesUtil.getSendGridApiKey());
        Request sgRequest = new Request();
        try {
            sgRequest.setMethod(Method.POST);
            sgRequest.setEndpoint("mail/send");
            sgRequest.setBody(mail.build());
            Response response = sg.api(sgRequest);
            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                throw new TscEMailException(response.getStatusCode(),
                        mail.getPersonalization().get(0).getTos().get(0).getEmail(),
                        mail.getSubject());
            }
        } catch (TscEMailException sgEx) {
            throw new TscEMailException(sgEx.getStatusCode(), sgEx.getAddress(), sgEx.getTitle());
        } catch (Exception ex) {
            throw new CustomException(ex);
        }
    }
}
