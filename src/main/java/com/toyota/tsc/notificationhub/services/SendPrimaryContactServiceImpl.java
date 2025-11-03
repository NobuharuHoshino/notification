
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
    public String sendPrimaryContact(SendPrimaryContactRequestDto request, RequestHeaderDto header) {
        try {

            // 開始ログ
            LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, CommonUtil.toJson(request), header.getCorrelationId()));

            // リクエスト検証
            String validateResult = validate(request, header);
            if (validateResult != null) {
                return validateResult;
            }

            // 個人情報取得
            PersonalInfoResponseDto response = CommonUtil.getPersonalInfoApiResponse(request.getInternalUserId());
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
                        "RS07E0000", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new RuntimeException();
            }
        }
    }

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

    private void executeSendSms(SendPrimaryContactRequestDto request, RequestHeaderDto header, String phoneNo) {
        HttpEntity<String> entity = smsCountryUtil.createRequest(
                CommonUtil.normalizePhoneNumber(phoneNo), request.getBody_sms(),
                request.getBrdCd());
        smsCountryUtil.sendSmsCountry(entity, phoneNo);
    }

    private void executeSendEmail(SendPrimaryContactRequestDto request, RequestHeaderDto header, String email) {
        Mail mail = sendGridUtil.generateEmail(
                email, request.getTitle(), request.getBody_text(),
                request.getBody_html(), request.getBrdCd());
        sendGridUtil.executeSendEmail(mail);
    }

    // #region Validation Methods
    private String validate(SendPrimaryContactRequestDto request, RequestHeaderDto header) {
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(
                    SendPrimaryContactServiceImpl.class,
                    CommonUtil.getMessage(
                            "RS07E00012",
                            missingField,
                            header.getCorrelationId()));
            throw new TscApplicationException("Missing required field: " + missingField);
        }
        if (!isValidBrdCd(request.getBrdCd())) {
            LogUtil.error(
                    SendPrimaryContactServiceImpl.class,
                    CommonUtil.getMessage(
                            "RS07E00008",
                            request.getBrdCd(),
                            header.getCorrelationId()));
            throw new TscApplicationException("Invalid brdCd: " + request.getBrdCd());
        }
        return null;
    }

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

    private boolean isValidBrdCd(String brdCd) {
        return brdCd.equals("0") || brdCd.equals("1") || brdCd.equals("2");
    }
    // #endregion
}
