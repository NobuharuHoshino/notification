
package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
import com.windowsazure.messaging.NotificationHubsException;
import com.windowsazure.messaging.NotificationOutcome;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * プッシュ通知送信サービス実装クラス
 */
@Profile("me")
@Service
public class SendPushServiceImpl implements SendPushServiceIF {

    private NtfInfoRepositoryIF ntfInfoRepository;
    private NotificationHubUtil notificationHubUtil;
    private PropertiesUtil properties;

    public SendPushServiceImpl(
            NtfInfoRepositoryIF ntfInfoRepository,
            NotificationHubUtil notificationHubUtil,
            PropertiesUtil properties) {
        this.ntfInfoRepository = ntfInfoRepository;
        this.notificationHubUtil = notificationHubUtil;
        this.properties = properties;
    }

    private static final String PROCCESS_NAME = "プッシュ通知送信要求";
    private static final String FCM = "1";
    private static final String APN = "2";

    public static final String RESULT_SUCCESS = "SP_SUCCESS";
    public static final String RESULT_FIELD_MISSING = "SP_FIELD_MISSING";
    public static final String RESULT_GET_DEVICE_EMPTY = "SP_GET_DEVICE_EMPTY";
    public static final String RESULT_PUSH_EXCEPTION = "SP_PUSH_EXCEPTION";
    public static final String RESULT_EXCEPTION = "SP_EXCEPTION";

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
            LogUtil.info(SendPushServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            validate(request, header);

            // InstallationID取得
            List<NtfInfoEntity> deviceList = getAllDeviceData(request.getInternalUserId());
            if (deviceList.isEmpty()) {
                LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00013", request.getInternalUserId(), header.getCorrelationId()));
                throw new CustomException();
            }
            NtfInfoEntity deviceData = getLastData(deviceList);

            // プッシュ通知実行
            operationPostMessage(request, header, deviceData);

            String resultCode = CommonUtil.getResultCode("SUCCESS");

            // 正常終了ログ
            LogUtil.info(SendPushServiceImpl.class, CommonUtil.getMessage(
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
                    LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                            "RS07E00010", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                    throw new CustomException();
                } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                    // 操作エラー
                    LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                            "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                    throw new CustomSqlException();
                }
                LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException();
            } else {
                LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException();
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
    private void operationPostMessage(
            SendPushRequestDto request, RequestHeaderDto header, NtfInfoEntity deviceData) {
        // プッシュ通知開始ログ
        LogUtil.info(SendPushServiceImpl.class, CommonUtil.getMessage(
                "RS07I00007", request.getInternalUserId(), request.getBody(),
                deviceData.getBrdCd(), header.getCorrelationId()));
        // 実行
        executePostMessage(request, header, deviceData);
        // プッシュ通知完了ログ
        LogUtil.info(SendPushServiceImpl.class, CommonUtil.getMessage(
                "RS07I00008", request.getInternalUserId(), request.getBody(), deviceData.getInstallationId(),
                header.getCorrelationId()));
    }

    /**
     * ペイロード作成
     * 
     * @param request
     * @param header
     * @param deviceData
     * @return
     */
    private String createPayload(
            SendPushRequestDto request, RequestHeaderDto header, NtfInfoEntity deviceData) {
        try {
            switch (deviceData.getPlatformType()) {
                case APN:
                    return notificationHubUtil.buildApnsPayload(request.getBody());
                case FCM:
                    return notificationHubUtil.buildFcmV1Payload(request.getBody());
                default:
                    throw new CustomException(); // 1,2以外は登録されないので基本到達しない。
            }
        } catch (Exception e) {
            LogUtil.info(SendPushServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00014", request.getInternalUserId(), request.getBody(), header.getCorrelationId()));
            throw new CustomException();
        }

    }

    /**
     * InstallationAPI実行部
     * 
     * @param request    リクエストDTO
     * @param header     ヘッダーDTO
     * @param deviceData 端末情報エンティティ
     * @return NotificationOutcome（送信結果）
     */
    private NotificationOutcome executePostMessage(
            SendPushRequestDto request, RequestHeaderDto header, NtfInfoEntity deviceData) {
        int cnt = 0;
        while (cnt < properties.getRetryCount()) {
            try {
                String payload = createPayload(request, header, deviceData);
                return notificationHubUtil.postMessage(
                        deviceData.getInstallationId(), payload, deviceData.getBrdCd(),
                        deviceData.getPlatformType());
            } catch (NotificationHubsException ex) {
                if (ex.isTransient()) {
                    cnt++;
                    if (cnt >= properties.getRetryCount()) {
                        LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                                "RS07E00005", ex.httpStatusCode(), request.getInternalUserId(),
                                request.getBody(), deviceData.getInstallationId(), header.getCorrelationId()));
                        throw new TscNotificationHubsException(CommonUtil.getResultCode(RESULT_PUSH_EXCEPTION));
                    }
                    LogUtil.warn(SendPushServiceImpl.class, CommonUtil.getMessage(
                            "RS07W00003", cnt, ex.httpStatusCode(), request.getBody(),
                            deviceData.getInstallationId(), header.getCorrelationId()));
                    continue;
                }
                LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00005", ex.httpStatusCode(), request.getInternalUserId(),
                        request.getBody(), deviceData.getInstallationId(), header.getCorrelationId()));
                throw new TscNotificationHubsException(CommonUtil.getResultCode(RESULT_PUSH_EXCEPTION));
            } catch (Exception e) {
                throw e;
            }
        }
        return null;
    }

    /**
     * ユーザーIDで全端末情報を取得します。
     * 
     * @param internalUserId ユーザーID
     * @return 端末情報リスト
     */
    private List<NtfInfoEntity> getAllDeviceData(String internalUserId) {
        List<NtfInfoEntity> deviceList = new ArrayList<>();
        deviceList.addAll(ntfInfoRepository.selectAllByInternalUserId(internalUserId));
        return deviceList;
    }

    /**
     * 端末情報リストから最新データを取得します。
     * 
     * @param deviceList 端末情報リスト
     * @return 最新の端末情報エンティティ
     */
    private NtfInfoEntity getLastData(List<NtfInfoEntity> deviceList) {
        return deviceList.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .findFirst()
                .orElse(null);
    }

    // #region Validation Methods
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
            LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00012", missingField, header.getCorrelationId()));
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
    // #endregion

}
