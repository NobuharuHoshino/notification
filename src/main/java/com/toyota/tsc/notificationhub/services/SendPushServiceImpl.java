
package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.SendPushRequestDto;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
import com.windowsazure.messaging.NotificationHubsException;
import com.windowsazure.messaging.NotificationOutcome;

import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SendPushServiceImpl implements SendPushServiceIF {

    @Value("${azure.notification-hub.retry-count}")
    private int retryCount;

    @Autowired
    private NtfInfoRepositoryIF ntfInfoRepository;
    @Autowired
    private NotificationHubUtil notificationHubUtil;

    private static final String PROCCESS_NAME = "プッシュ通知送信要求";

    private static final Pattern TRACKING_ID_PATTERN = Pattern.compile("Tracking ID:\\s*(\\S+)");

    @Override
    public String sendPush(SendPushRequestDto request, RequestHeaderDto header) {

        try {
            // 開始ログ
            LogUtil.info(SendPushServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            String validateResult = validate(request, header);
            if (validateResult != null) {
                return validateResult;
            }

            // InstallationID取得
            List<NtfInfoEntity> deviceList = getAllDeviceData(request.getInternalUserId());
            NtfInfoEntity deviceData = getLastData(deviceList);

            // プッシュ通知実行
            operationPostMessage(request, header, deviceData);

            String resultCode = CommonUtil.getResultCode("SUCCESS");

            // 正常終了ログ
            LogUtil.info(SendPushServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));

            return resultCode;

        } catch (Exception e) {
            handleException(e, header);
            throw e;
        }
    }

    /**
     * NotificationHubのInstallation登録/更新APIを呼び出します。
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
                deviceData.getInternalUserId(), header.getCorrelationId()));
    }

    /**
     * InstallationAPI実行部
     */
    private NotificationOutcome executePostMessage(
            SendPushRequestDto request, RequestHeaderDto header, NtfInfoEntity deviceData) {
        int cnt = 0;
        while (cnt < this.retryCount) {
            try {
                NotificationOutcome outcome = notificationHubUtil.postMessage(
                        deviceData.getInstallationId(), request.getBody(), deviceData.getBrdCd(),
                        deviceData.getPlatformType());
                return outcome;
            } catch (NotificationHubsException ex) {
                if (ex.isTransient()) {
                    cnt++;
                    if (cnt >= this.retryCount) {
                        LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                                "RS07E00005", ex.httpStatusCode(), request.getInternalUserId(),
                                request.getBody(), deviceData.getInstallationId(), extractTrackingId(ex),
                                header.getCorrelationId()));
                        throw new TscNotificationHubsException(ex);
                    }
                    LogUtil.warn(SendPushServiceImpl.class, CommonUtil.getMessage(
                            "RS07W00003", cnt, ex.httpStatusCode(), request.getBody(),
                            deviceData.getInstallationId(), header.getCorrelationId()));
                    continue;
                }
                LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00005", ex.httpStatusCode(), request.getInternalUserId(),
                        request.getBody(), deviceData.getInstallationId(), extractTrackingId(ex),
                        header.getCorrelationId()));
                throw new TscNotificationHubsException(ex);
            } catch (Exception e) {
                throw e;
            }
        }
        return null;
    }

    private String extractTrackingId(NotificationHubsException ex) {
        Matcher matcher = TRACKING_ID_PATTERN.matcher(ex.getMessage());
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<NtfInfoEntity> getAllDeviceData(String internalUserId) {
        List<NtfInfoEntity> deviceList = new ArrayList<>();
        deviceList.addAll(ntfInfoRepository.selectAllByInternalUserId(internalUserId));
        return deviceList;
    }

    private NtfInfoEntity getLastData(List<NtfInfoEntity> deviceList) {
        return deviceList.stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .findFirst()
                .orElse(null);
    }

    // #region Validation Methods
    private String validate(SendPushRequestDto request, RequestHeaderDto header) {
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00012", missingField, header.getCorrelationId()));
            throw new TscApplicationException();
        }
        return null;
    }

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

    // #region Exception Handling Methods
    private void handleException(Exception e, RequestHeaderDto header) {
        if (e instanceof SQLException || e.getCause() instanceof SQLException) {
            SQLException sqlEx = e instanceof SQLException ? (SQLException) e : (SQLException) e.getCause();
            if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00010", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                throw new RuntimeException();

            } else if (sqlEx instanceof SQLTransientException || sqlEx instanceof SQLNonTransientException) {
                LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                throw new CustomSqlException();
            }
        } else if (e instanceof TscApplicationException) {
            throw new TscApplicationException();
        } else if (e instanceof TscNotificationHubsException) {
            throw new RuntimeException();
        } else {
            LogUtil.error(SendPushServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
            throw new RuntimeException();
        }
    }
    // #endregion

}
