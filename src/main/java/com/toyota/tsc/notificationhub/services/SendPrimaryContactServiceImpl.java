
package com.toyota.tsc.notificationhub.services;

import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.SendGridUtil;
import com.toyota.tsc.notificationhub.commons.SmsCountryUtil;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;

/**
 * プライマリ連絡先送信サービス実装クラス
 */
@Service
public class SendPrimaryContactServiceImpl implements SendPrimaryContactServiceIF {

    private static final String PROCCESS_NAME = "PrimaryContactメッセージ送信要求";
    private static final String CONTACT_PHONE = "1";
    private static final String CONTACT_EMAIL = "2";

    @Autowired
    SmsCountryUtil smsCountryUtil;
    @Autowired
    private SendGridUtil sendGridUtil;

    @Override
    /**
     * プライマリ連絡先へメッセージ送信を実行します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @return 結果コード
     */
    public String sendPrimaryContact(SendPrimaryContactRequestDto request, RequestHeaderDto header) {
        try {

            // 開始ログ
            LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            validate(request, header);

            // 個人情報取得
            PersonalInfoResponseDto response = CommonUtil.getPersonalInfoApiResponse(request.getInternalUserId());
            if (response == null) {
                throw new RuntimeException();
            }
            List<PersonalInfoResponseDto.ContactDto> contactList = response.getContactList();

            // 送信要求
            sendRequest(contactList, request, header);

            String resultCode = CommonUtil.getResultCode("SUCCESS");

            // 正常終了ログ
            LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));
            return resultCode;

        } catch (TscEMailException e) {
            LogUtil.error(SendMailServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00007", e.getStatusCode(), CommonUtil.maskText(e.getAddress()),
                    request.getTitle(), header.getCorrelationId()));
            throw new RuntimeException();

        } catch (TscSMSException e) {
            LogUtil.error(SendSmsServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00006", e.getStatusCode(), CommonUtil.maskPhoneNumber(e.getPhoneNo()),
                    header.getCorrelationId()));
            throw new RuntimeException();

        } catch (Exception e) {

            if (e instanceof TscApplicationException) {
                throw new TscApplicationException();

            } else {
                LogUtil.error(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new RuntimeException();
            }
        }
    }

    /**
     * 連絡先リストへ送信要求を実行します。
     * 
     * @param contactList 連絡先リスト
     * @param request     リクエストDTO
     * @param header      ヘッダーDTO
     * @return なし
     */
    private void sendRequest(List<PersonalInfoResponseDto.ContactDto> contactList,
            SendPrimaryContactRequestDto request, RequestHeaderDto header) {
        contactList.stream().forEach(contact -> {
            if (!contact.isPrimaryContactFlag()) {
                return; // primaryContactFlagがfalseの場合はスキップ
            }
            if (contact.getContactType().equals(CONTACT_PHONE) && contact.isPrimaryContactFlag()) {
                // SMS送信要求
                LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                        "RS07I00013", request.getInternalUserId(), contact.getContact(), request.getTitle(),
                        header.getCorrelationId()));
                executeSendSms(request, header, contact.getContact());
            } else if (contact.getContactType().equals(CONTACT_EMAIL) && contact.isPrimaryContactFlag()) {
                // メール送信要求
                LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                        "RS07I00014", request.getInternalUserId(), request.getBrdCd(), contact.getContact(),
                        request.getTitle(), header.getCorrelationId()));
                executeSendEmail(request, header, contact.getContact());
            } else {
                return;
            }
        });
    }

    /**
     * SMS送信処理を実行します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param phoneNo 送信先電話番号
     * @return なし
     */
    private void executeSendSms(SendPrimaryContactRequestDto request, RequestHeaderDto header, String phoneNo) {
        HttpEntity<String> entity = smsCountryUtil.createRequest(
                CommonUtil.normalizePhoneNumber(phoneNo), request.getBody_sms(),
                request.getBrdCd());
        smsCountryUtil.sendSmsCountry(entity, phoneNo);
    }

    /**
     * メール送信処理を実行します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param email   送信先メールアドレス
     * @return なし
     */
    private void executeSendEmail(SendPrimaryContactRequestDto request, RequestHeaderDto header, String email) {
        Mail mail = sendGridUtil.generateEmail(
                email, request.getTitle(), request.getBody_text(),
                request.getBody_html(), request.getBrdCd());
        sendGridUtil.executeSendEmail(mail);
    }

    // #region Validation Methods
    /**
     * リクエストの必須項目・値を検証します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @return 不足項目名（問題なければnull）
     */
    private String validate(SendPrimaryContactRequestDto request, RequestHeaderDto header) {
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00012", missingField, header.getCorrelationId()));
            throw new TscApplicationException();
        }
        if (!isValidBrdCd(request.getBrdCd())) {
            LogUtil.error(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00008", request.getBrdCd(), header.getCorrelationId()));
            throw new TscApplicationException();
        }
        return null;
    }

    /**
     * リクエストDTOの必須項目を検証します。
     * 
     * @param request リクエストDTO
     * @return 不足項目名（問題なければnull）
     */
    private String validateRequired(SendPrimaryContactRequestDto request) {
        if (request == null) {
            return "requestBody";
        }
        List<String> missingFields = new ArrayList<>();
        if (request.getProcessId() == null || request.getProcessId().isEmpty()) {
            missingFields.add("processId");
        }
        if (request.getInternalUserId() == null || request.getInternalUserId().isEmpty()) {
            missingFields.add("internalUserId");
        }
        if (request.getBrdCd() == null || request.getBrdCd().isEmpty()) {
            missingFields.add("brdCd");
        }

        if (!missingFields.isEmpty()) {
            return String.join(",", missingFields);
        }
        return null;
    }

    /**
     * ブランドコード値が有効か判定します。
     * 
     * @param brdCd ブランドコード
     * @return 有効ならtrue
     */
    private boolean isValidBrdCd(String brdCd) {
        return brdCd.equals("0") || brdCd.equals("1") || brdCd.equals("2");
    }
    // #endregion
}
