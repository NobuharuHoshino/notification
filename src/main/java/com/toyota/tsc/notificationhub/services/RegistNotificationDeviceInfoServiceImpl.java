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
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;
import com.windowsazure.messaging.NotificationHubsException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 端末情報登録サービス実装クラス
 */
@Profile("me")
@Service
public class RegistNotificationDeviceInfoServiceImpl implements RegistNotificationDeviceInfoServiceIF {

    private NtfInfoRepositoryIF ntfInfoRepository;
    private NotificationHubUtil notificationHubUtil;
    private PropertiesUtil propertiesUtil;

    public RegistNotificationDeviceInfoServiceImpl(
            NtfInfoRepositoryIF ntfInfoRepository,
            NotificationHubUtil notificationHubUtil,
            PropertiesUtil propertiesUtil) {
        this.ntfInfoRepository = ntfInfoRepository;
        this.notificationHubUtil = notificationHubUtil;
        this.propertiesUtil = propertiesUtil;
    }

    private static final String PROCCESS_NAME = "通知端末情報登録";
    private static final String RESULT_SUCCESS = "RND_SUCCESS";
    private static final String RESULT_FIELD_MISSING = "RND_FIELD_MISSING";
    private static final String RESULT_INVALID_BRAND = "RND_INVALID_BRAND";
    private static final String RESULT_INVALID_PLATFORM = "RND_INVALID_PLATFORM";
    private static final String RESULT_DEL_INSTALLATION_EXCEPTION = "RND_DEL_INSTALLATION_EXCEPTION";
    private static final String RESULT_PUT_INSTALLATION_EXCEPTION = "RND_PUT_INSTALLATION_EXCEPTION";
    private static final String RESULT_EXCEPTION = "RND_EXCEPTION";

    @Override
    /**
     * 端末情報を登録します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @return 結果コード
     */
    @Transactional(rollbackFor = Exception.class)
    public ResponseDto registDeviceInfo(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header) {

        try {

            // 開始ログ
            LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                    "RS07I00001", PROCCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            validate(request, header);

            // Installation実行
            List<NtfInfoEntity> deviceList = getAllDeviceData(request.getInternalUserId());
            if (!extractByDeviceToken(deviceList, request.getDeviceToken()).isEmpty()) {
                LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                        "RS07D00002", request.getDeviceToken(), "InstallationID生成SKIP",
                        CommonUtil.toJson(extractToDeviceTokenList(deviceList)), header.getCorrelationId()));
            } else {
                execRegistNotificationInfo(request, header, deviceList);
            }

            String resultCode = CommonUtil.getResultCode(RESULT_SUCCESS);

            // 正常終了ログ
            LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
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
                    LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                            "RS07E00010", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                    throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
                } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                    // 操作エラー
                    LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                            "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                    throw new CustomSqlException(CommonUtil.getResultCode(RESULT_EXCEPTION));
                }
                LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
            } else {
                // その他予期せぬエラー
                LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
            }
        }

    }

    private void execRegistNotificationInfo(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header,
            List<NtfInfoEntity> deviceList) {
        String installationId = generateInstallationId(request, header);
        int upsertCount = upsertDeviceInfo(request, header, installationId);
        if (upsertCount == 0) {
            throw new CustomException();
        }
        operationDeleteInstallation(request, header, deviceList);
        operationUpsertInstallation(request, header, installationId);
        if (deviceList.size() >= 2) {
            int deleteCount = deleteDeviceData(request, header, deviceList);
            if (deleteCount == 0) {
                throw new CustomException();
            }
        }
    }

    // #region NotificationHub Methods
    /**
     * Installation IDを生成します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @return 生成したInstallation ID
     */
    private String generateInstallationId(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header) {
        String installationId = UUID.randomUUID().toString();
        LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                "RS07D00004", request.getInternalUserId(), installationId, header.getCorrelationId()));
        return installationId;
    }

    /**
     * 既存Installation情報の削除処理を行います。
     * 
     * @param request    リクエストDTO
     * @param header     ヘッダーDTO
     * @param deviceList 端末情報リスト
     * @return なし
     */
    private void operationDeleteInstallation(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, List<NtfInfoEntity> deviceList) {
        // 2件以上存在する場合、古いものを削除
        if (deviceList.size() >= 2) {
            List<NtfInfoEntity> deleteTarget = getDeleteTargetList(deviceList);
            deleteTarget.forEach(entity -> {
                // 削除開始ログ
                LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                        "RS07D00001", request.getDeviceToken(), entity.getInstallationId(), entity.getDeviceToken(),
                        header.getCorrelationId()));
                // 削除API実行（リトライ内包、失敗の場合はthrowされる）
                executeDeleteInstallation(request, header, entity);
                // 削除完了ログ
                LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                        "RS07I00004", CommonUtil.getBrd(request.getBrdCd()), request.getInternalUserId(),
                        header.getCorrelationId()));
            });
        }
    }

    /**
     * 削除対象のInstallation情報リストを取得します。（最新1件を除く）
     * 
     * @param deviceList 端末情報リスト
     * @return 削除対象の端末情報リスト
     */
    private List<NtfInfoEntity> getDeleteTargetList(List<NtfInfoEntity> deviceList) {
        var sorted = deviceList.stream()
                .sorted(Comparator.comparing(
                        NtfInfoEntity::getUpdatedAt,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed())
                .toList();
        // 先頭（最新1件）を除いた残りを返す
        return sorted.subList(1, sorted.size());
    }

    /**
     * Installation削除APIを実行します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param entity  端末情報エンティティ
     * @return なし
     */
    private void executeDeleteInstallation(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, NtfInfoEntity entity) {
        int cnt = 0;
        while (cnt < propertiesUtil.getRetryCount()) {
            try {
                notificationHubUtil.deleteInstallation(entity.getInstallationId(), request.getBrdCd());
                return;
            } catch (NotificationHubsException ex) {
                if (ex.isTransient()) {
                    cnt++;
                    if (cnt >= propertiesUtil.getRetryCount()) {
                        LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                                "RS07E00003", ex.httpStatusCode(), CommonUtil.getBrd(request.getBrdCd()),
                                request.getInternalUserId(),
                                entity.getInstallationId(), CommonUtil.getPlt(request.getPlatform()),
                                request.getDeviceToken(),
                                request.getDvcId(), header.getCorrelationId()));
                        throw new TscNotificationHubsException(
                                CommonUtil.getResultCode(RESULT_DEL_INSTALLATION_EXCEPTION));
                    }
                    LogUtil.warn(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                            "RS07W00001", cnt, CommonUtil.toJson(entity), ex.httpStatusCode(),
                            header.getCorrelationId()));
                    continue;
                }
                LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00003", ex.httpStatusCode(), CommonUtil.getBrd(request.getBrdCd()),
                        request.getInternalUserId(),
                        entity.getInstallationId(), CommonUtil.getPlt(request.getPlatform()), request.getDeviceToken(),
                        request.getDvcId(), header.getCorrelationId()));
                throw new TscNotificationHubsException(CommonUtil.getResultCode(RESULT_DEL_INSTALLATION_EXCEPTION));
            } catch (Exception e) {
                throw e;
            }
        }
    }

    /**
     * Installation情報のUpsert処理を行います。
     * 
     * @param request        リクエストDTO
     * @param header         ヘッダーDTO
     * @param installationId Installation ID
     * @return なし
     */
    private void operationUpsertInstallation(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, String installationId) {
        // installation開始ログ
        LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                "RS07I00005", CommonUtil.getBrd(request.getBrdCd()), request.getInternalUserId(), installationId,
                CommonUtil.getPlt(request.getPlatform()), header.getCorrelationId()));
        // insertion実行
        executeUpsertInstallation(request, header, installationId);
        // installation完了ログ
        LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                "RS07I00006", CommonUtil.getBrd(request.getBrdCd()), request.getInternalUserId(),
                header.getCorrelationId()));
    }

    /**
     * Installation Upsert APIを実行します。
     * 
     * @param request        リクエストDTO
     * @param header         ヘッダーDTO
     * @param installationId Installation ID
     * @return なし
     */
    private void executeUpsertInstallation(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, String installationId) {
        int cnt = 0;
        while (cnt < propertiesUtil.getRetryCount()) {
            try {
                notificationHubUtil.upsertInstallation(
                        installationId, request.getBrdCd(), request.getInternalUserId(),
                        request.getPlatform(), request.getDeviceToken());
                return;
            } catch (NotificationHubsException ex) {
                if (ex.isTransient()) {
                    cnt++;
                    if (cnt >= propertiesUtil.getRetryCount()) {
                        LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                                "RS07E00004", ex.httpStatusCode(), request.getInternalUserId(),
                                installationId, request.getDvcId(), header.getCorrelationId()));
                        throw new TscNotificationHubsException(
                                CommonUtil.getResultCode(RESULT_PUT_INSTALLATION_EXCEPTION));
                    }
                    LogUtil.warn(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                            "RS07W00002", cnt, installationId, ex.httpStatusCode(),
                            header.getCorrelationId()));
                    continue;
                }
                LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                        "RS07E00004", ex.httpStatusCode(), request.getInternalUserId(),
                        installationId, request.getDvcId(), header.getCorrelationId()));
                throw new TscNotificationHubsException(CommonUtil.getResultCode(RESULT_PUT_INSTALLATION_EXCEPTION));
            } catch (Exception e) {
                throw e;
            }
        }
    }
    // #endregion

    // #region Action DB/DO Methods
    /**
     * デバイストークンで端末情報を抽出します。
     * 
     * @param deviceList  端末情報リスト
     * @param deviceToken デバイストークン
     * @return 抽出した端末情報リスト
     */
    private List<NtfInfoEntity> extractByDeviceToken(List<NtfInfoEntity> deviceList, String deviceToken) {
        return deviceList.stream()
                .filter(entity -> entity.getDeviceToken().equals(deviceToken)).toList();
    }

    /**
     * 端末情報リストからデバイストークンリストを抽出します。
     * 
     * @param deviceList 端末情報リスト
     * @return デバイストークンリスト
     */
    private List<String> extractToDeviceTokenList(List<NtfInfoEntity> deviceList) {
        return deviceList.stream()
                .map(NtfInfoEntity::getDeviceToken)
                .toList();
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
     * 端末情報をUpsert（登録または更新）します。
     * 
     * @param request        リクエストDTO
     * @param header         ヘッダーDTO
     * @param installationId Installation ID
     * @return Upsert件数
     */
    private int upsertDeviceInfo(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, String installationId) {
        NtfInfoEntity entity = new NtfInfoEntity(
                request.getInternalUserId(), installationId, request.getDeviceToken(), request.getDvcId(),
                request.getBrdCd(), request.getPlatform(), LocalDateTime.now(), LocalDateTime.now());
        int upsertCount = ntfInfoRepository.upsert(entity);
        LogUtil.info(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                "RS07D00005", request.getInternalUserId(), installationId, request.getDeviceToken(),
                request.getDvcId(), CommonUtil.getBrd(request.getBrdCd()), CommonUtil.getPlt(request.getPlatform()),
                header.getCorrelationId()));
        return upsertCount;
    }

    /**
     * 端末情報を削除します（2件を残して古いものを削除）。
     * 
     * @param request    リクエストDTO
     * @param header     ヘッダーDTO
     * @param deviceList 端末情報リスト
     * @return 削除件数
     */
    private int deleteDeviceData(
            RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header, List<NtfInfoEntity> deviceList) {
        List<NtfInfoEntity> deleteTarget = deviceList.stream()
                .sorted(Comparator.comparing(
                        NtfInfoEntity::getUpdatedAt,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())).reversed())
                .skip(2)
                .toList();
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
     * リクエストの必須項目・値を検証します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @return 不足項目名（問題なければnull）
     */
    private String validate(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header) {
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00012", missingField, header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_FIELD_MISSING));
        }
        if (!isValidBrdCd(request.getBrdCd())) {
            LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00008", request.getBrdCd(), header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_INVALID_BRAND));
        }
        if (!isValidPlatform(request.getPlatform())) {
            LogUtil.error(RegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getMessage(
                    "RS07E00009", request.getPlatform(), header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_INVALID_PLATFORM));
        }
        return null;
    }

    /**
     * リクエストDTOの必須項目を検証します。
     * 
     * @param request リクエストDTO
     * @return 不足項目名（問題なければnull）
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
     * プラットフォーム値が有効か判定します。
     * 
     * @param platform プラットフォーム値
     * @return 有効ならtrue
     */
    private boolean isValidPlatform(String platform) {
        return platform.equals("1") || platform.equals("2");
    }

    /**
     * ブランドコード値が有効か判定します。
     * 
     * @param brdCd ブランドコード
     * @return 有効ならtrue
     */
    private boolean isValidBrdCd(String brdCd) {
        return brdCd.equals("1") || brdCd.equals("2");
    }
    // #endregion
}
