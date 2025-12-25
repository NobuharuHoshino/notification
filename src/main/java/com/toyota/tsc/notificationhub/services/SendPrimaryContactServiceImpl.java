
package com.toyota.tsc.notificationhub.services;

import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.PersonalInfoUtil;
import com.toyota.tsc.notificationhub.commons.SendGridUtil;
import com.toyota.tsc.notificationhub.commons.SmsCountryUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscEMailException;
import com.toyota.tsc.notificationhub.exceptions.TscSMSException;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * プライマリ連絡先送信サービス実装クラス
 */
@Profile("me")
@Service
public class SendPrimaryContactServiceImpl implements SendPrimaryContactServiceIF {

    private static final String PROCCESS_NAME = "PrimaryContactメッセージ送信要求";
    private static final String CONTACT_PHONE = "1";
    private static final String CONTACT_EMAIL = "2";

    public static final String RESULT_SUCCESS = "PC_SUCCESS";
    public static final String RESULT_FIELD_MISSING = "PC_FIELD_MISSING";
    public static final String RESULT_INVALID_BRAND = "PC_INVALID_BRAND";
    public static final String RESULT_GET_PERSONALINFO_EMPTY = "PC_GET_PERSONALINFO_EMPTY";
    public static final String RESULT_EXCEPTION = "PC_EXCEPTION";

    private SmsCountryUtil smsCountryUtil;
    private SendGridUtil sendGridUtil;
    private PersonalInfoUtil personalInfoUtil;

    public SendPrimaryContactServiceImpl(
            SmsCountryUtil smsCountryUtil,
            SendGridUtil sendGridUtil,
            PersonalInfoUtil personalInfoUtil) {
        this.smsCountryUtil = smsCountryUtil;
        this.sendGridUtil = sendGridUtil;
        this.personalInfoUtil = personalInfoUtil;
    }

    @Override
    /**
     * プライマリ連絡先へメッセージ送信を実行します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @return 結果コード
     */
    public ResponseDto sendPrimaryContact(SendPrimaryContactRequestDto request, RequestHeaderDto header) {
        try {

            // 開始ログ
            LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            validate(request, header);

            // 個人情報取得
            PersonalInfoResponseDto response = personalInfoUtil.getPersonalInfoApiResponse(
                    request.getInternalUserId(), header.getCorrelationId());
            if (response == null) {
                LogUtil.error(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00015", request.getInternalUserId(), header.getCorrelationId()));
                throw new TscApplicationException(CommonUtil.getResultCode(RESULT_GET_PERSONALINFO_EMPTY));
            }
            List<PersonalInfoResponseDto.ContactDto> contactList = response.getContactList();

            // 送信要求
            sendRequest(contactList, request, header);

            String resultCode = CommonUtil.getResultCode(RESULT_SUCCESS);

            // 正常終了ログ
            LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));
            return new ResponseDto(resultCode);

        } catch (TscEMailException e) {
            throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));

        } catch (TscSMSException e) {
            throw new CustomException(e.getResultCode());

        } catch (TscApplicationException e) {
            throw new TscApplicationException(e.getResultCode());

        } catch (Exception e) {
            LogUtil.error(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
            throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
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
                        "RS07I00013", request.getInternalUserId(), request.getBrdCd(), contact.getContact(),
                        request.getTitle(), header.getCorrelationId()));
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
        ResponseEntity<String> smsResponse = smsCountryUtil.executeSendSms(
                phoneNo, request.getBodyText(), request.getBrdCd());
        if (!smsResponse.getStatusCode().is2xxSuccessful()) {
            LogUtil.error(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00006", smsResponse.getStatusCode(), CommonUtil.maskPhoneNumber(phoneNo),
                    header.getCorrelationId()));
            throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
        }
        LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                "RS07I00010", request.getBrdCd(), phoneNo, header.getCorrelationId()));
        LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getSaMessage(
                "RS99I99999", smsResponse.getStatusCode(), smsResponse.getBody()));
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
                email, request.getTitle(), request.getBodyText(),
                request.getBodyHtml(), request.getBrdCd());
        Response response = sendGridUtil.executeSendEmail(mail);
        if (!HttpStatusCode.valueOf(response.getStatusCode()).is2xxSuccessful()) {
            LogUtil.error(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00007", response.getStatusCode(), CommonUtil.maskText(email),
                    request.getTitle(), header.getCorrelationId()));
            throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
        }
        LogUtil.info(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                "RS07I00012", request.getBrdCd(), email, header.getCorrelationId()));
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
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_FIELD_MISSING));
        }
        if (!isValidBrdCd(request.getBrdCd())) {
            LogUtil.error(SendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00008", request.getBrdCd(), header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_INVALID_BRAND));
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
