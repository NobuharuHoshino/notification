package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.JsapUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.GetUserIdResponseDto;
import com.toyota.tsc.notificationhub.models.PushRequestResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.repositories.SaNtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.SaNtfInfoRepositoryIF;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * プッシュ通知送信サービス実装クラス
 */
@Profile("sa")
@Service
public class SaSendPushServiceImpl implements SendPushServiceIF {

    private SaNtfInfoRepositoryIF saNtfInfoRepository;
    private NotificationHubUtil notificationHubUtil;
    private JsapUtil jsapUtil;

    public SaSendPushServiceImpl(
            SaNtfInfoRepositoryIF saNtfInfoRepository,
            NotificationHubUtil notificationHubUtil,
            JsapUtil jsapUtil) {
        this.saNtfInfoRepository = saNtfInfoRepository;
        this.notificationHubUtil = notificationHubUtil;
        this.jsapUtil = jsapUtil;
    }

    private static final String PROCCESS_NAME = "サウジプッシュ通知送信要求";
    private static final String FCM = "1";
    private static final String APN = "2";

    public static final String RESULT_SUCCESS = "SA_SP_SUCCESS";
    public static final String RESULT_FIELD_MISSING = "SA_SP_FIELD_MISSING";
    public static final String RESULT_PLATFORM_EMPTY = "SA_SP_PLATFORM_EMPTY";
    public static final String RESULT_BODYEDIT_ERROR = "SA_SP_BODYEDIT_ERROR";
    public static final String RESULT_GET_USERID_ERROR = "SA_SP_GET_USERID_ERROR";
    public static final String RESULT_PUSH_ERROR = "SA_SP_PUSH_ERROR";
    public static final String RESULT_EXCEPTION = "SA_SP_EXCEPTION";

    private static final String RES_GETUSERID_SUCCESS = "00001548B123";
    private static final String RES_JSAPPUSH_SUCCESS = "000000";

    @Override
    /**
     * プッシュ通知送信処理を実行します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @return 結果コード
     */
    public ResponseDto sendPush(SendPushRequestDto request, RequestHeaderDto header) {

        try {
            // 開始ログ
            LogUtil.info(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00001", PROCCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            validate(request, header);

            // プラットフォーム取得
            SaNtfInfoEntity userData = getData(request.getInternalUserId());
            if (userData == null) {
                LogUtil.error(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00007", request.getInternalUserId(), header.getCorrelationId()));
                throw new TscApplicationException(CommonUtil.getResultCode(RESULT_PLATFORM_EMPTY));
            }

            // Body編集
            String payload = createPayload(request, header, userData);

            // JSAPトークン取得
            String token = "";

            // Push通知送信処理実行
            operationPostMessage(request, header, userData, payload, token);

            String resultCode = CommonUtil.getResultCode(RESULT_SUCCESS);

            // 正常終了ログ
            LogUtil.info(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));

            return new ResponseDto(resultCode);

        } catch (TscApplicationException e) {
            throw new TscApplicationException(e.getResultCode());

        } catch (TscNotificationHubsException e) {
            throw new TscNotificationHubsException(e.getResultCode());

        } catch (Exception e) {
            SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
            if (sqlEx != null) {
                if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                    // 接続エラー
                    LogUtil.error(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                            "RS07E00010", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                    throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
                } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                    // 操作エラー
                    LogUtil.error(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                            "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                    throw new CustomSqlException(CommonUtil.getResultCode(RESULT_EXCEPTION));
                }
                LogUtil.error(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
            } else {
                LogUtil.error(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
            }
        }
    }

    /**
     * NotificationHubのInstallation登録/更新APIを呼び出します。
     * 
     * @param request    リクエストDTO
     * @param header     ヘッダーDTO
     * @param deviceData 端末情報エンティティ
     * @return なし
     */
    private void operationPostMessage(SendPushRequestDto request, RequestHeaderDto header, SaNtfInfoEntity userData,
            String payload, String token) {
        try {
            // 認証規約 UserID取得
            ResponseEntity<String> getUserIdResponce = jsapUtil.executeGetUserId(
                    request.getInternalUserId(), header.getCorrelationId());
            ObjectMapper mapper = new ObjectMapper();
            GetUserIdResponseDto getUserIdDto = mapper.readValue(getUserIdResponce.getBody(),
                    GetUserIdResponseDto.class);
            if (!getUserIdDto.getResultCode().equals(RES_GETUSERID_SUCCESS)) {
                LogUtil.error(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00014", getUserIdDto.getResultCode(), request.getInternalUserId(),
                        header.getCorrelationId()));
                throw new TscApplicationException(CommonUtil.getResultCode(RESULT_GET_USERID_ERROR));
            }
            LogUtil.info(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00009", request.getInternalUserId(), header.getCorrelationId()));

            // NotificationHub送信処理実行(JSAP実行)
            LogUtil.info(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00005", request.getInternalUserId(), payload, userData.getPlatformType(),
                    header.getCorrelationId()));
            ResponseEntity<String> jsapNotificationResponce = jsapUtil.executePushRequest(
                    getUserIdDto.getUserId(), payload, token);
            PushRequestResponseDto pushRequestDto = mapper.readValue(
                    jsapNotificationResponce.getBody(), PushRequestResponseDto.class);
            if (!pushRequestDto.getResultCode().equals(RES_JSAPPUSH_SUCCESS)) {
                LogUtil.error(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00009", pushRequestDto.getResultCode(), request.getInternalUserId(), payload,
                        userData.getPlatformType(), header.getCorrelationId()));
                throw new TscNotificationHubsException(CommonUtil.getResultCode(RESULT_PUSH_ERROR));
            }
            LogUtil.info(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00006", request.getInternalUserId(), payload, userData.getPlatformType(),
                    header.getCorrelationId()));

        } catch (JsonProcessingException e) {
            throw new CustomException(e);
        }
    }

    /**
     * ペイロード作成
     * 
     * @param request
     * @param header
     * @param deviceData
     * @return
     */
    private String createPayload(SendPushRequestDto request, RequestHeaderDto header, SaNtfInfoEntity userData) {
        try {
            switch (userData.getPlatformType()) {
                case APN:
                    return notificationHubUtil.buildApnsPayload(request.getBody());
                case FCM:
                    return notificationHubUtil.buildFcmV1Payload(request.getBody());
                default:
                    throw new CustomException(); // 1,2以外は登録されないので基本到達しない。
            }
        } catch (Exception e) {
            LogUtil.info(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07E00008", request.getInternalUserId(), request.getBody(), header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_BODYEDIT_ERROR));
        }

    }

    /**
     * ユーザーIDで全端末情報を取得します。
     * 
     * @param internalUserId ユーザーID
     * @return 端末情報リスト
     */
    private SaNtfInfoEntity getData(String internalUserId) {
        return saNtfInfoRepository.select(internalUserId);
    }

    /**
     * リクエストの必須項目・値を検証します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @return 不足項目名（問題なければnull）
     */
    private String validate(SendPushRequestDto request, RequestHeaderDto header) {
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(SaSendPushServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07E00003", missingField, header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_FIELD_MISSING));
        }
        return null;
    }

    /**
     * リクエストDTOの必須項目を検証します。
     * 
     * @param request リクエストDTO
     * @return 不足項目名（問題なければnull）
     */
    private String validateRequired(SendPushRequestDto request) {
        if (request == null) {
            return "requestBody";
        }
        List<String> missingFields = new ArrayList<>();
        if (request.getInternalUserId() == null || request.getInternalUserId().isEmpty()) {
            missingFields.add("internalUserId");
        }
        if (request.getBody() == null || request.getBody().isEmpty()) {
            missingFields.add("body");
        }

        if (!missingFields.isEmpty()) {
            return String.join(",", missingFields);
        }
        return null;
    }
}
