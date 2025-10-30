package com.toyota.tsc.notificationhub.services;

import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.exceptions.TscNotificationHubsException;
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
import java.util.List;
import com.windowsazure.messaging.NotificationHubsException;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;

import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RegistNotificationDeviceInfoServiceImpl implements RegistNotificationDeviceInfoServiceIF {

    @Autowired
    private NtfInfoRepositoryIF ntfInfoRepository;
    @Autowired
    private NotificationHubUtil notificationHubUtil;

    @Value("${azure.notification-hub.retry-count}")
    private int retryCount;

    private static final String PROCCESS_NAME = "通知端末情報登録";

    /**
     * 通知先デバイス情報をNotificationHubおよびDBに登録します。
     * - リクエストされたデバイストークンに一致するDBレコード情報がない場合に登録処理を実行します。
     * - 既存で登録済みの場合は、何も処理せず正常終了します。
     */
    @Override
    public String registDeviceInfo(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header) {
        try {
            // 開始ログ
            LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, CommonUtil.toJson(request), header.getCorrelationId()));

            // リクエスト検証
            String validateResult = validate(request, header);
            if (validateResult != null) {
                return validateResult;
            }

            // 内部UserIDで既存登録情報を取得
            // リクエスト.デバイストークンに一致するレコードがない場合、installationID発番・DB登録・installation実行
            List<NtfInfoEntity> deviceList = getAllDeviceData(request.getInternalUserId());
            if (extractByDeviceToken(deviceList, request.getDeviceToken()).size() == 0) {
                // generate installationID
                String installationId = generateInstallationId(request, header);
                // upsert
                int upsertCount = upsertDeviceInfo(request, header, installationId);
                if (upsertCount == 0) {
                    throw new RuntimeException();
                }
                // installation削除APIリクエストをUtil経由で実行
                operationDeleteInstallation(request, header, deviceList);
                // installation実行
                operationUpsertInstallation(request, header, installationId);
                // 不要データ削除実行(最新+1件以外のデータがある場合、当該データを削除)
                if (deviceList.size() > 2) {
                    int deleteCount = deleteDeviceData(request, header, deviceList);
                    if (deleteCount == 0) {
                        throw new RuntimeException();
                    }
                }
            }

            String resultCode = CommonUtil.getResultCode("SUCCESS");

            // 正常終了ログ
            LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));

            return resultCode;

        } catch (Exception e) {
            handleException(e, header);
            throw e;
        }
    }

    // #region NotificationHub Methods
    /**
     * InstallationIDを発番します。
     */
    private String generateInstallationId(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header) {
        String installationId = UUID.randomUUID().toString();
        LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                "RS07D00004", request.getInternalUserId(), installationId, header.getCorrelationId()));
        return installationId;
    }

    /**
     * NotificationHubのInstallation削除APIを呼び出します。
     */
    private void operationDeleteInstallation(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, List<NtfInfoEntity> deviceList) {

        // installation削除APIリクエストをUtil経由で実行
        deviceList.forEach(entity -> {
            // 削除開始ログ
            LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                    "RS07D00001", request.getDeviceToken(), entity.getInstallationId(), entity.getDeviceToken(),
                    header.getCorrelationId()));
            // 削除API実行（リトライ内包、失敗の場合はthrowされる）
            executeDeleteInstallation(request, header, entity);
            // 削除完了ログ
            LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00004", "StatusCode", request.getBrdCd(), request.getInternalUserId(),
                    header.getCorrelationId()));
        });
    }

    /**
     * 削除API実行部
     */
    private void executeDeleteInstallation(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, NtfInfoEntity entity) {
        int cnt = 0;
        while (cnt < this.retryCount) {
            try {
                notificationHubUtil.deleteInstallation(entity.getInstallationId(), request.getBrdCd());
                return;
            } catch (NotificationHubsException ex) {
                if (ex.isTransient()) {
                    cnt++;
                    if (cnt >= this.retryCount) {
                        LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                                "RS07E00003", ex.httpStatusCode(), request.getBrdCd(), request.getInternalUserId(),
                                entity.getInstallationId(), request.getPlatform(), request.getDeviceToken(),
                                request.getDvcId(), header.getCorrelationId()));
                        throw new TscNotificationHubsException(ex);
                    }
                    LogUtil.warn(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                            "RS07W00001", cnt, CommonUtil.toJson(entity), ex.httpStatusCode(),
                            header.getCorrelationId()));
                    continue;
                }
                LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00003", ex.httpStatusCode(), request.getBrdCd(), request.getInternalUserId(),
                        entity.getInstallationId(), request.getPlatform(), request.getDeviceToken(),
                        request.getDvcId(), header.getCorrelationId()));
                throw new TscNotificationHubsException(ex);
            } catch (Exception e) {
                throw e;
            }
        }
    }

    /**
     * NotificationHubのInstallation登録/更新APIを呼び出します。
     */
    private void operationUpsertInstallation(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, String installationId) {
        // installation開始ログ
        LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                "RS07I00005", request.getBrdCd(), request.getInternalUserId(), installationId,
                request.getPlatform(), header.getCorrelationId()));
        // insertion実行
        executeUpsertInstallation(request, header, installationId);
        // installation完了ログ
        LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                "StatusCode", request.getBrdCd(), request.getInternalUserId(), header.getCorrelationId()));
    }

    /**
     * InstallationAPI実行部
     */
    private void executeUpsertInstallation(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, String installationId) {
        int cnt = 0;
        while (cnt < this.retryCount) {
            try {
                notificationHubUtil.upsertInstallation(
                        installationId, request.getBrdCd(), request.getInternalUserId(),
                        request.getPlatform(), request.getDeviceToken());
                return;
            } catch (NotificationHubsException ex) {
                if (ex.isTransient()) {
                    cnt++;
                    if (cnt >= this.retryCount) {
                        LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                                "RS07E00004", ex.httpStatusCode(), request.getInternalUserId(),
                                installationId, request.getDvcId(), header.getCorrelationId()));
                        throw new TscNotificationHubsException(ex);
                    }
                    LogUtil.warn(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                            "RS07W00002", cnt, installationId, ex.httpStatusCode(),
                            header.getCorrelationId()));
                    continue;
                }
                LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00004", ex.httpStatusCode(), request.getInternalUserId(),
                        installationId, request.getDvcId(), header.getCorrelationId()));
                throw new TscNotificationHubsException(ex);
            } catch (Exception e) {
                throw e;
            }
        }
    }
    // #endregion

    // #region Action DB/DO Methods
    private List<NtfInfoEntity> extractByDeviceToken(List<NtfInfoEntity> deviceList, String deviceToken) {
        return deviceList.stream()
                .filter(entity -> entity.getDeviceToken().equals(deviceToken)).collect(Collectors.toList());
    }

    private List<NtfInfoEntity> getAllDeviceData(String internalUserId) {
        List<NtfInfoEntity> deviceList = new ArrayList<>();
        deviceList.addAll(ntfInfoRepository.selectAllByInternalUserId(internalUserId));
        return deviceList;
    }

    private int upsertDeviceInfo(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, String installationId) {
        NtfInfoEntity entity = new NtfInfoEntity(
                request.getInternalUserId(), installationId, request.getDeviceToken(), request.getDvcId(),
                request.getBrdCd(), request.getPlatform(), LocalDateTime.now(), LocalDateTime.now());
        int upsertCount = ntfInfoRepository.upsert(entity);
        LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                "RS07D00005", request.getInternalUserId(), CommonUtil.toJson(entity), header.getCorrelationId()));
        return upsertCount;
    }

    private int deleteDeviceData(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, List<NtfInfoEntity> deviceList) {
        List<NtfInfoEntity> deleteTarget = deviceList.stream()
                .sorted(Comparator.comparing(
                        NtfInfoEntity::getUpdatedAt,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed())
                .skip(2)
                .collect(Collectors.toList());
        int deleteCount = deleteTarget.stream()
                .mapToInt(t -> ntfInfoRepository.delete(t.getInternalUserId(), t.getInstallationId()))
                .sum();
        LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                "RS07D00003", request.getInternalUserId(), deleteTarget.get(0).getInstallationId(),
                header.getCorrelationId()));
        return deleteCount;
    }
    // #endregion

    // #region Validation Methods
    /**
     * リクエスト内容の検証処理を呼び出します。
     * 
     * @param request
     * @param header
     * @return
     */
    private String validate(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header) {
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(
                    RegistNotificationDeviceInfoServiceImpl.class,
                    CommonUtil.getMessage(
                            "RS07E00012",
                            missingField,
                            header.getCorrelationId()));
            throw new TscApplicationException("Missing required field: " + missingField);
        }
        if (!isValidBrdCd(request.getBrdCd())) {
            LogUtil.error(
                    RegistNotificationDeviceInfoServiceImpl.class,
                    CommonUtil.getMessage(
                            "RS07E00008",
                            request.getBrdCd(),
                            header.getCorrelationId()));
            throw new TscApplicationException("Invalid brdCd: " + request.getBrdCd());
        }
        if (!isValidPlatform(request.getPlatform())) {
            LogUtil.error(
                    RegistNotificationDeviceInfoServiceImpl.class,
                    CommonUtil.getMessage(
                            "RS07E00009",
                            request.getPlatform(),
                            header.getCorrelationId()));
            throw new TscApplicationException("Invalid platform: " + request.getPlatform());
        }
        return null;
    }

    /**
     * リクエスト内容の必須項目検証を行います。
     * 
     * @return 必須エラーの項目名（Swagger定義の項目名）、またはnull（エラーなし）
     */
    private String validateRequired(RegistNotificationDeviceInfoRequestDto request) {
        if (request == null) {
            return "requestBody";
        }
        List<String> missingFields = new ArrayList<>();
        if (request.getInternalUserId() == null || request.getInternalUserId().isEmpty()) {
            missingFields.add("internalUserId");
        }
        if (request.getPlatform() == null || request.getPlatform().isEmpty()) {
            missingFields.add("platform");
        }
        if (request.getDeviceToken() == null || request.getDeviceToken().isEmpty()) {
            missingFields.add("deviceToken");
        }
        if (request.getDvcId() == null || request.getDvcId().isEmpty()) {
            missingFields.add("dvcId");
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
     * プラットフォームの妥当性検証を行います。
     * 
     * @return true: Android(1)またはiOS(2)、false: その他の非対応プラットフォーム
     */
    private boolean isValidPlatform(String platform) {
        return platform.equals("1") || platform.equals("2");
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

    // #region Exception Handling Methods
    /**
     * Exceptionハンドリング共通処理
     */
    private void handleException(Exception e, RequestHeaderDto header) {
        if (e instanceof SQLException || e.getCause() instanceof SQLException) {
            SQLException sqlEx = e instanceof SQLException ? (SQLException) e : (SQLException) e.getCause();
            if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                LogUtil.error(
                        RegistNotificationDeviceInfoServiceImpl.class,
                        CommonUtil.getMessage(
                                "RS07E00010",
                                sqlEx.getMessage(),
                                sqlEx.getStackTrace(),
                                header.getCorrelationId()));
                throw new RuntimeException();

            } else if (sqlEx instanceof SQLTransientException || sqlEx instanceof SQLNonTransientException) {
                LogUtil.error(
                        RegistNotificationDeviceInfoServiceImpl.class,
                        CommonUtil.getMessage(
                                "RS07E00011",
                                sqlEx.getMessage(),
                                sqlEx.getStackTrace(),
                                header.getCorrelationId()));
                throw new CustomSqlException();
            }
        } else if (e instanceof TscApplicationException) {
            throw new TscApplicationException();
        } else if (e instanceof TscNotificationHubsException) {
            throw new RuntimeException();
        } else {
            LogUtil.error(
                    RegistNotificationDeviceInfoServiceImpl.class,
                    CommonUtil.getMessage(
                            "RS07E00001",
                            e.getMessage(),
                            e.getStackTrace(),
                            header.getCorrelationId()));
            throw new RuntimeException();
        }
    }
    // #endregion
}
