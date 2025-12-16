package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.JsapUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscPrimaryContactException;
import com.toyota.tsc.notificationhub.models.GetUserIdResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserInfoResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendMessageResponseDto;
import com.toyota.tsc.notificationhub.models.SendPrimaryContactRequestDto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * プライマリ連絡先送信サービス実装クラス
 */
@Profile("sa")
@Service
public class SaSendPrimaryContactServiceImpl implements SendPrimaryContactServiceIF {

    private static final String PROCCESS_NAME = "PrimaryContactメッセージ送信要求";

    private static final String CONTACT_PHONE = "0";
    private static final String CONTACT_EMAIL = "1";

    private static final String RESULT_SUCCESS = "SA_PC_SUCCESS";
    private static final String RESULT_FIELD_MISSING = "SA_PC_FIELD_MISSING";
    private static final String RESULT_INVALID_BRAND = "SA_PC_INVALID_BRAND";
    private static final String RESULT_GET_USERID_ERROR = "SA_PC_GET_USERID_ERROR";
    private static final String RESULT_GET_ALJ_ERROR = "SA_PC_GET_ALJ_ERROR";
    private static final String RESULT_SEND_ERROR = "SA_PC_SEND_ERROR";

    private static final String RES_GETUSERID_SUCCESS = "00001548B123";
    private static final String RES_JSAPPUSH_SUCCESS = "000000";

    private JsapUtil jsapUtil;

    public SaSendPrimaryContactServiceImpl(
            JsapUtil jsapUtil) {
        this.jsapUtil = jsapUtil;
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
            LogUtil.info(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            validate(request, header);

            // JSAPトークン取得
            String token = "";

            // 送信要求
            sendRequest(token, request, header);

            String resultCode = CommonUtil.getResultCode(RESULT_SUCCESS);

            // 正常終了ログ
            LogUtil.info(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));
            return new ResponseDto(resultCode);

        } catch (TscPrimaryContactException e) {
            throw new TscPrimaryContactException(e.getResultCode());

        } catch (TscApplicationException e) {
            throw new TscApplicationException(e.getResultCode());

        } catch (Exception e) {
            LogUtil.error(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
            throw new CustomException();
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
    private void sendRequest(String token, SendPrimaryContactRequestDto request, RequestHeaderDto header) {
        try {
            // 認証規約 UserID取得
            ResponseEntity<String> getUserIdResponce = jsapUtil.executeGetUserId(
                    request.getInternalUserId(), header.getCorrelationId());
            ObjectMapper mapper = new ObjectMapper();
            GetUserIdResponseDto getUserIdDto = mapper.readValue(getUserIdResponce.getBody(),
                    GetUserIdResponseDto.class);
            if (!getUserIdDto.getResultCode().equals(RES_GETUSERID_SUCCESS)) {
                LogUtil.error(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00014", getUserIdDto.getResultCode(), request.getInternalUserId(),
                        header.getCorrelationId()));
                throw new TscApplicationException(CommonUtil.getResultCode(RESULT_GET_USERID_ERROR));
            }
            LogUtil.info(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00009", request.getInternalUserId(), header.getCorrelationId()));

            // ユーザー情報取得
            ResponseEntity<String> getUserInfoResponce = jsapUtil.executeGetUserInfo(
                    getUserIdDto.getUserId());
            GetUserInfoResponseDto getUserInfoDto = mapper.readValue(getUserInfoResponce.getBody(),
                    GetUserInfoResponseDto.class);
            if (!getUserInfoResponce.getStatusCode().is2xxSuccessful()) {
                LogUtil.error(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00014", request.getInternalUserId(), header.getCorrelationId()));
                throw new TscApplicationException(CommonUtil.getResultCode(RESULT_GET_ALJ_ERROR));
            }
            LogUtil.info(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00010", request.getInternalUserId(), header.getCorrelationId()));

            // 送信実行
            sendOperation(getUserInfoDto.getContactList(), request, header,
                    getUserIdDto.getUserId(), token);

        } catch (Exception e) {
            LogUtil.error(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
            throw new CustomException();
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
    private void sendOperation(List<GetUserInfoResponseDto.ContactDto> contactList,
            SendPrimaryContactRequestDto request, RequestHeaderDto header, String userId, String token) {
        boolean hasUserId = request.getInternalUserId() != null && !request.getInternalUserId().isEmpty();
        contactList.stream().forEach(contact -> {
            if (!contact.isPrimaryContactFlag()) {
                return; // primaryContactFlagがfalseの場合はスキップ
            }
            // コンテキスト作成
            Object context;
            if (contact.getContactType().equals(CONTACT_PHONE)) {
                context = jsapUtil.createSmsContext(request.getBodyText());
            } else if (contact.getContactType().equals(CONTACT_EMAIL)) {
                context = jsapUtil.createMailContext(request.getBodyText(), request.getBodyHtml());
            } else {
                throw new CustomException(); // 到達想定なし
            }

            executeSendMessage(request, header, contact, userId, hasUserId, context, token);
        });
    }

    /**
     * JSAPへメッセージ送信要求を実行します。
     * 
     * @param request   リクエストDTO
     * @param header    ヘッダーDTO
     * @param contact   連絡先DTO
     * @param userId    ユーザーID
     * @param hasUserId 内部UserIDがあるかどうか
     * @param token     JSAP認証トークン
     * @return なし
     */
    private void executeSendMessage(SendPrimaryContactRequestDto request, RequestHeaderDto header,
            GetUserInfoResponseDto.ContactDto contact, String userId, boolean hasUserId, Object context, String token) {
        try {
            ResponseEntity<String> sendMessageResponse = jsapUtil.executeSendMessage(
                    request.getProcessId(),
                    userId,
                    contact.getContactType(),
                    request.getTitle(),
                    context,
                    token);
            ObjectMapper mapper = new ObjectMapper();
            SendMessageResponseDto sendMessageDto = mapper.readValue(sendMessageResponse.getBody(),
                    SendMessageResponseDto.class);
            if (!sendMessageDto.getResultCode().equals(RES_JSAPPUSH_SUCCESS)) {
                LogUtil.error(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00010", sendMessageDto.getResultCode(),
                        hasUserId ? "内部UserID" : "プロセスID",
                        hasUserId ? request.getInternalUserId() : request.getProcessId(),
                        request.getBrdCd(), header.getCorrelationId()));
                throw new TscPrimaryContactException(CommonUtil.getResultCode(RESULT_SEND_ERROR));
            }
            LogUtil.info(SaSendPrimaryContactServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00008", hasUserId ? "内部UserID" : "プロセスID",
                    hasUserId ? request.getInternalUserId() : request.getProcessId(),
                    request.getBrdCd(), header.getCorrelationId()));
        } catch (Exception e) {
            throw new CustomException(e);
        }
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
