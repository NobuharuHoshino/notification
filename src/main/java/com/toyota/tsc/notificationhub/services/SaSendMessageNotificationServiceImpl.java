package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.commons.BatApisUtil;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.JsapUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.models.GetUserIdResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserInfoResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SaNotificationSendListDto;
import com.toyota.tsc.notificationhub.models.SendMessageNotificationRequestDto;
import com.toyota.tsc.notificationhub.models.SendMessageResponseDto;
import com.toyota.tsc.notificationhub.models.PushRequestResponseDto;
import com.toyota.tsc.notificationhub.models.RegisterNotificationRequestDto;
import com.toyota.tsc.notificationhub.models.RegisterNotificationResponseDto;
import com.toyota.tsc.notificationhub.repositories.NotificationRepositoryIF;
import com.toyota.tsc.notificationhub.repositories.NotificationVinListEntity;
import com.toyota.tsc.notificationhub.repositories.NotificationVinListRepositoryIF;
import com.toyota.tsc.notificationhub.repositories.NtfBatchExecErrorInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfBatchExecErrorInfoRepositoryIF;
import com.toyota.tsc.notificationhub.repositories.NtfBatchExecHistoryEntity;
import com.toyota.tsc.notificationhub.repositories.NtfBatchExecHistoryRepositoryIF;
import com.toyota.tsc.notificationhub.repositories.NtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.NtfInfoRepositoryIF;

import jp.toyota.res.common.auth.GetALJTokenResultDto;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * お知らせ通知送信サービス実装クラス
 */
@Profile("sa")
@Service
public class SaSendMessageNotificationServiceImpl implements SendMessageNotificationServiceIF {

    // #region DI
    private final NotificationRepositoryIF notificationRepository;
    private final NotificationVinListRepositoryIF notificationVinListRepository;
    private final NtfBatchExecErrorInfoRepositoryIF ntfBatchExecErrorInfoRepository;
    private final NtfBatchExecHistoryRepositoryIF ntfBatchExecHistoryRepository;
    private final NtfInfoRepositoryIF ntfInfoRepository;
    private final BatApisUtil batApisUtil;
    private final JsapUtil jsapUtil;
    private final NotificationHubUtil notificationHubUtil;
    private final PropertiesUtil properties;

    public SaSendMessageNotificationServiceImpl(
            NotificationRepositoryIF notificationRepository,
            NotificationVinListRepositoryIF notificationVinListRepository,
            NtfBatchExecErrorInfoRepositoryIF ntfBatchExecErrorInfoRepository,
            NtfBatchExecHistoryRepositoryIF ntfBatchExecHistoryRepository,
            NtfInfoRepositoryIF ntfInfoRepository,
            BatApisUtil batApisUtil,
            JsapUtil jsapUtil,
            NotificationHubUtil notificationHubUtil,
            PropertiesUtil properties) {
        this.notificationRepository = notificationRepository;
        this.notificationVinListRepository = notificationVinListRepository;
        this.ntfBatchExecErrorInfoRepository = ntfBatchExecErrorInfoRepository;
        this.ntfBatchExecHistoryRepository = ntfBatchExecHistoryRepository;
        this.ntfInfoRepository = ntfInfoRepository;
        this.batApisUtil = batApisUtil;
        this.jsapUtil = jsapUtil;
        this.notificationHubUtil = notificationHubUtil;
        this.properties = properties;
    }
    // #endregion

    // #region クラスメンバ定義
    // 処理名
    private static final String PROCESS_NAME = "お知らせ機能";
    // リザルトコード取得キー
    private static final String RESULT_SUCCESS = "ME_BAT_SUCCESS";
    private static final String RESULT_FIELD_MISSING = "ME_BAT_FIELD_MISSING";
    private static final String RESULT_USERINFO_EMPTY = "ME_BAT_GETUSERINFO_FAILED";
    private static final String RESULT_EXCEPTION = "ME_BAT_EXCEPTION";
    // Notification登録 APIレスポンス 正常終了コード
    private static final String REGISTNOTIFICATION_SUCCESS = "000000";
    // 内部UserID変換 APIレスポンス 正常終了コード
    private static final String GETUSERID_SUCCESS = "00001548B123";
    // Jsap 正常終了コード
    private static final String JSAP_SUCCESS = "000000";
    // テーブル名
    private static final String TBL_NTF_BATCH_EXEC_HISTORY = "ntf_batch_exec_history";
    private static final String TBL_NOTIFICATION_VIN_LIST = "notification_vin_list";
    private static final String TBL_NOTIFICATION = "notification";
    // 通知バッチ処理履歴テーブル 処理ステータス
    private static final String STATUS_STR = "0";
    private static final String STATUS_ERR = "2";
    private static final String STATUS_DONE = "1";
    // 通知バッチエラー情報テーブル 処理ステータス
    private static final String ERR_STATUS_SKP = "9";
    private static final String ERR_STATUS_SUCCESS = "1";
    private static final String ERR_STATUS_ERR = "2";
    // NotificationType
    private static final String TYPE_NTF = "1";
    private static final String TYPE_MAILSMS = "2";
    private static final String TYPE_NTF_AND_MAILSMS = "3";
    // プラットフォームタイプ
    private static final String FCM = "1";
    private static final String APN = "2";
    // 連絡先種別
    private static final String CONTACT_PHONE = "1";
    private static final String CONTACT_EMAIL = "2";
    // 国・言語コード
    private static final String CNT_SA = "SA";
    // 連携フラグ
    private static final int LINKTYPE_NOTLINKED = 0;
    private static final int LINKTYPE_LINKED = 2;
    // #endregion

    @Override
    public ResponseDto sendMessageNotification(SendMessageNotificationRequestDto request, RequestHeaderDto header) {

        try {
            // 1:開始ログ
            LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00001",
                    PROCESS_NAME,
                    header.getCorrelationId(),
                    CommonUtil.toJson(request)));

            // 2:リクエスト検証
            validate(request, header);

            // 3:ユーザー情報取得
            List<NotificationVinListEntity> userInfo = getUserInfoAndUpdateFlag(request, header);

            // 4:ユーザー情報分のループ処理（親ループ）実行
            executeNotificationProcess(request, header, userInfo);

            // 5:お知らせ情報連携フラグ更新
            updateNotification(request, header);

            // 6:終了ログ
            LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00002",
                    PROCESS_NAME, CommonUtil.getResultCode(RESULT_SUCCESS), header.getCorrelationId()));

            return new ResponseDto(CommonUtil.getResultCode(RESULT_SUCCESS));

        } catch (TscApplicationException e) {
            throw new TscApplicationException(e.getResultCode());

        } catch (CustomSqlException e) {
            // SQLエラー：テーブル名とcauseを受け取り
            SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
            if (sqlEx != null) {
                if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                    // 接続エラー
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00020",
                            e.getTable(),
                            sqlEx.getMessage(),
                            sqlEx.getStackTrace(),
                            header.getCorrelationId()));
                } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                    // 操作エラー
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00019",
                            e.getTable(),
                            sqlEx.getMessage(),
                            sqlEx.getStackTrace(),
                            sqlEx.getSQLState(),
                            sqlEx.getErrorCode(),
                            header.getCorrelationId()));
                }
            }
            throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));

        } catch (Exception e) {
            // その他エラー
            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00001",
                    PROCESS_NAME,
                    e.getMessage(),
                    e.getStackTrace(),
                    header.getCorrelationId()));
            throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
        }
    }

    // #region 2:バリデーション検証
    /**
     * リクエストの必須項目・値を検証します。
     *
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     */
    private void validate(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header) {

        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00003",
                    missingField, header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_FIELD_MISSING));
        }
    }

    /**
     * リクエストDTOの必須項目を検証し、未設定項目を返却します。
     *
     * @param request リクエストDTO
     * @return 不足項目名（問題なければnull）
     */
    private String validateRequired(SendMessageNotificationRequestDto request) {

        // 必須項目検証
        if (request == null) {
            return "requestBody";
        }
        List<String> missingFields = new ArrayList<>();
        if (request.getRegistrationSerialNumber() == null) {
            missingFields.add("registrationSerialNumber");
        }
        if (request.getNotificationType() == null ||
                request.getNotificationType().isEmpty()) {
            missingFields.add("notificationType");
        }

        // 相関必須検証（NotificationType=1,3の場合、コンテンツリストを検証）
        if (TYPE_NTF.equals(request.getNotificationType())
                || TYPE_NTF_AND_MAILSMS.equals(request.getNotificationType())) {
            if (request.getNotificationContents() == null || request.getNotificationContents().isEmpty()) {
                missingFields.add("notificationContents");
            }
        }

        // エラー項目があればカンマ区切りの文字列で返却、なければnullを返却
        if (!missingFields.isEmpty()) {
            return String.join(",", missingFields);
        }
        return null;
    }
    // endregion

    // #region 3:Vinリスト取得
    /**
     * NotificationVinListから未連携のユーザー情報を取得
     *
     * @param request
     * @param header
     * @return
     */
    private List<NotificationVinListEntity> getUserInfoAndUpdateFlag(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header) {

        try {
            List<NotificationVinListEntity> vinList = notificationVinListRepository
                    .select(request.getRegistrationSerialNumber(), LINKTYPE_NOTLINKED);

            if (vinList == null || vinList.isEmpty()) {
                LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00016",
                        request.getRegistrationSerialNumber().toString(),
                        header.getCorrelationId()));
                throw new TscApplicationException(CommonUtil.getResultCode(RESULT_USERINFO_EMPTY));
            }
            return vinList;

        } catch (TscApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomSqlException(TBL_NOTIFICATION_VIN_LIST, e);
        }
    }
    // endregion

    // #region 3:ユーザー情報分のループ処理実行
    /**
     * ユーザー情報分のループ処理実行
     * 3-1:通知基盤連携情報リストのオブジェクトマッピング
     * 3-2:お知らせ通知処理履歴登録
     * 3-3:NotificationVinList連携フラグ更新
     * 3-4:オンプレ個人情報リスト取得
     * 3-5:非同期で通知処理実行
     * X:想定外の例外時は例外スロー
     * 
     * @param request
     * @param header
     * @param userInfo
     */
    private void executeNotificationProcess(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            List<NotificationVinListEntity> userInfoList) {

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                properties.getThreadPool(), properties.getThreadPool(),
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.getThreadQueue()),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("ntf-worker-" + t.getName());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy());
        List<CompletableFuture<Void>> f = new ArrayList<>();

        try {

            userInfoList.forEach(entity -> {
                try {

                    // 4-1:お知らせ通知処理履歴登録
                    int insertCnt = this.insertHistory(request, entity);
                    if (insertCnt == 0) {
                        LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00021",
                                TBL_NTF_BATCH_EXEC_HISTORY,
                                "insertHistory",
                                request.getRegistrationSerialNumber(),
                                entity.getSequenceNumber(),
                                header.getCorrelationId()));
                        executeErrorProcess(request, header, entity, "",
                                ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                        return;
                    }
                    LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07D00006",
                            request.getRegistrationSerialNumber(),
                            entity.getSequenceNumber(),
                            STATUS_STR + "（処理開始）",
                            header.getCorrelationId()));

                    // 4-2:NotificationVinList連携フラグ更新
                    int updCnt = this.updateNotificationVinList(request, entity, LINKTYPE_NOTLINKED);
                    if (updCnt == 0) {
                        LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00021",
                                TBL_NOTIFICATION_VIN_LIST,
                                "updateNotificationVinList",
                                request.getRegistrationSerialNumber(),
                                entity.getSequenceNumber(),
                                header.getCorrelationId()));
                        executeErrorProcess(request, header, entity, "",
                                ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                        return;
                    }
                    LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00020",
                            TBL_NOTIFICATION_VIN_LIST,
                            request.getRegistrationSerialNumber(),
                            entity.getSequenceNumber(),
                            header.getCorrelationId()));

                    // 4-3:ユーザー情報.通知基盤連携情報（テキストデータ）をList<DTO>にマッピング
                    List<SaNotificationSendListDto> notificationSendList = convertToNotificationSendListDto(
                            entity.getNotificationSendList());
                    if (notificationSendList.isEmpty()) {
                        LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00018",
                                request.getRegistrationSerialNumber(),
                                entity.getSequenceNumber(),
                                header.getCorrelationId()));
                        executeErrorProcess(
                                request, header, entity, "",
                                ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                        return;
                    }
                    LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00021",
                            request.getRegistrationSerialNumber(),
                            entity.getSequenceNumber(),
                            header.getCorrelationId()));

                    // 4-4:内部UserIDをUserIDに変換
                    List<SaNotificationSendListDto> modNotificationSendList = convertInternalUserIdToUserId(
                            header, notificationSendList);
                    if (modNotificationSendList.isEmpty()) {
                        executeErrorProcess(
                                request, header, entity, "",
                                ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                        return;
                    }
                    LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00009",
                            request.getRegistrationSerialNumber(),
                            entity.getSequenceNumber(),
                            header.getCorrelationId()));

                    // 4-5:非同期で通知処理実行
                    f.add(CompletableFuture.runAsync(() -> {
                        try {
                            boolean errorFlag = this.executeNotificationProcess(
                                    request, header, entity, modNotificationSendList);
                            // お知らせ通知処理履歴更新
                            if (errorFlag) {
                                ntfBatchExecHistoryRepository.updateStatus(
                                        request.getRegistrationSerialNumber(),
                                        entity.getSequenceNumber().intValue(),
                                        STATUS_ERR);
                                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07D00006",
                                        request.getRegistrationSerialNumber(), entity.getSequenceNumber(),
                                        STATUS_ERR + "（処理終了）", header.getCorrelationId()));
                            } else {
                                ntfBatchExecHistoryRepository.updateStatus(
                                        request.getRegistrationSerialNumber(),
                                        entity.getSequenceNumber().intValue(),
                                        STATUS_DONE);
                                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07D00006",
                                        request.getRegistrationSerialNumber(), entity.getSequenceNumber(),
                                        STATUS_DONE + "（処理終了）", header.getCorrelationId()));
                            }
                            // NotificationVinList連携フラグ更新
                            this.updateNotificationVinList(request, entity, LINKTYPE_LINKED);

                        } catch (CustomSqlException e) {
                            // SQLエラー：テーブル名とcauseを受け取りログ出力 & 次ループ
                            SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
                            if (sqlEx != null) {
                                if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                                    // 接続エラー
                                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00020",
                                            e.getTable(),
                                            sqlEx.getMessage(),
                                            sqlEx.getStackTrace(),
                                            header.getCorrelationId()));
                                } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                                    // 操作エラー
                                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00019",
                                            e.getTable(),
                                            sqlEx.getMessage(),
                                            sqlEx.getStackTrace(),
                                            sqlEx.getSQLState(),
                                            sqlEx.getErrorCode(),
                                            header.getCorrelationId()));
                                }
                            }
                            ntfBatchExecHistoryRepository.updateStatus(request.getRegistrationSerialNumber(),
                                    entity.getSequenceNumber().intValue(), STATUS_ERR);
                            this.updateNotificationVinList(request, entity, LINKTYPE_LINKED);

                        } catch (Exception e) {
                            // 想定外のエラー：ステータス&フラグ更新
                            ntfBatchExecHistoryRepository.updateStatus(request.getRegistrationSerialNumber(),
                                    entity.getSequenceNumber().intValue(), STATUS_ERR);
                            this.updateNotificationVinList(request, entity, LINKTYPE_LINKED);
                        }
                    }, executor));

                } catch (CustomSqlException e) {
                    // SQLエラー：テーブル名とcauseを受け取りログ出力 & エラー登録して次ループ
                    SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
                    if (sqlEx != null) {
                        if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                            // 接続エラー
                            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00020",
                                    e.getTable(),
                                    sqlEx.getMessage(),
                                    sqlEx.getStackTrace(),
                                    header.getCorrelationId()));
                        } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                            // 操作エラー
                            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00019",
                                    e.getTable(),
                                    sqlEx.getMessage(),
                                    sqlEx.getStackTrace(),
                                    sqlEx.getSQLState(),
                                    sqlEx.getErrorCode(),
                                    header.getCorrelationId()));
                        }
                    }
                    executeErrorProcess(
                            request, header, entity, "",
                            ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                    return;

                } catch (Exception e) {
                    // 想定外のエラー：エラー登録 & 次ループ
                    executeErrorProcess(
                            request, header, entity, "",
                            ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                    return;
                }
            });

            // 全タスク完了待機
            CompletableFuture.allOf(f.toArray(new CompletableFuture[0])).join();

        } finally {
            executor.close();
        }
    }
    // endregion

    // #region 4-1:お知らせ通知処理履歴登録
    /**
     * お知らせ通知処理履歴テーブルに処理開始情報を新規登録
     * テーブルエンティティにINSERT項目を格納して登録処理を実行
     *
     * @param request
     * @param vinListEntity
     */
    private int insertHistory(
            SendMessageNotificationRequestDto request,
            NotificationVinListEntity vinListEntity) {

        try {
            NtfBatchExecHistoryEntity historyEntity = new NtfBatchExecHistoryEntity(
                    request.getRegistrationSerialNumber(),
                    vinListEntity.getSequenceNumber().intValue(),
                    STATUS_STR,
                    request.getScheduledSendData(),
                    null,
                    null,
                    null,
                    null);

            return ntfBatchExecHistoryRepository.insert(historyEntity);

        } catch (Exception e) {
            throw new CustomSqlException(TBL_NTF_BATCH_EXEC_HISTORY, e);
        }
    }
    // endregion

    // #region 4-2:連携フラグ更新
    /**
     * NotificationVinListの連携フラグを更新します。
     */
    private int updateNotificationVinList(
            SendMessageNotificationRequestDto request,
            NotificationVinListEntity vinListEntity,
            int linkType) {

        try {
            return notificationVinListRepository.update(
                    request.getRegistrationSerialNumber(),
                    vinListEntity.getSequenceNumber(),
                    linkType);

        } catch (Exception e) {
            throw new CustomSqlException(TBL_NOTIFICATION_VIN_LIST, e);
        }
    }
    // #endregion

    // #region 4-3:通知基盤連携情報をListオブジェクトで取得(Max1000件)
    /**
     * notification_vin_listテーブルから取得した通知基盤連携情報をList<DTO>にマッピングします。
     * 通知基盤連携情報はレコードカンマ区切り&フィールドコロン区切りのテキストデータです。
     * ※[vin]:[internalUserIId]:[lisenceCode]:[brdCd]
     * 
     * @param csvText notification_vin_list.notification_send_list
     * @return マッピングされたSaNotificationSendListDtoのリスト
     */
    private List<SaNotificationSendListDto> convertToNotificationSendListDto(String csvText) {
        try {
            return Arrays.stream(csvText.split(","))
                    .map(r -> r.split(":"))
                    .map(p -> new SaNotificationSendListDto(p[0], p[1], p[2], p[3], null))
                    .toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    // #endregion

    // #region 4-4:内部UserIDをUserIDに変換
    /**
     * 内部UserIDをUserIDに変換
     * 
     * @param request
     * @param header
     * @param userInfo
     * @return UserIDを含む通知送信リスト
     */
    private List<SaNotificationSendListDto> convertInternalUserIdToUserId(
            RequestHeaderDto header,
            List<SaNotificationSendListDto> notificationSendList) {

        try {

            boolean errorFlag = false;
            for (SaNotificationSendListDto dto : notificationSendList) {

                ResponseEntity<String> response = jsapUtil.executeGetUserId(
                        dto.getInternalUserId(), header.getCorrelationId());

                if (response == null || response.getBody() == null) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00014",
                            "response is null", dto.getInternalUserId(), header.getCorrelationId()));
                    errorFlag = true;
                    break;
                }
                ObjectMapper mapper = new ObjectMapper();
                GetUserIdResponseDto getUserIdDto = mapper.readValue(
                        response.getBody(), GetUserIdResponseDto.class);
                if (!getUserIdDto.getResultCode().equals(GETUSERID_SUCCESS)) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00014",
                            getUserIdDto.getResultCode(), dto.getInternalUserId(), header.getCorrelationId()));
                    errorFlag = true;
                    break;
                }

                // 正常時UserIDをセット
                dto.setUserId(getUserIdDto.getUserId());
            }

            if (errorFlag) {
                return new ArrayList<>();
            }
            return notificationSendList;

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }
    // #endregion

    // #region 4-5:通知処理実行
    /**
     * 通知処理実行基幹（1~Eを個人）
     * 1:Notification登録判定&実行
     * 2:PUSH実行判定&実行
     * 3:PrimaryContact送信判定&実行
     * E:NG発生時は共通のエラー処理（通知バッチエラーテーブル登録）
     * X:想定外の例外時は例外スローして処理ループを抜ける
     * 
     * @param request
     * @param header
     * @param vinList
     * @param personalInfoListResDto
     * @param notificationSendList
     */
    private boolean executeNotificationProcess(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            NotificationVinListEntity vinList,
            List<SaNotificationSendListDto> notificationSendList) {

        boolean errorFlag = false;
        for (SaNotificationSendListDto notificationData : notificationSendList) {
            try {

                // 0:トークン取得（期限切れを避けるため）
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00014",
                        request.getRegistrationSerialNumber(), vinList.getSequenceNumber(), header.getCorrelationId()));
                GetALJTokenResultDto tokenResult = jsapUtil.executeGetToken();
                if (tokenResult == null) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00013",
                            "tokenResult is null",
                            notificationData.getInternalUserId(), header.getCorrelationId()));
                    executeErrorProcess(request, header, vinList, "",
                            ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                    continue;
                }
                if (!tokenResult.getResult()) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00013",
                            tokenResult.getResult(),
                            notificationData.getInternalUserId(), header.getCorrelationId()));
                    executeErrorProcess(request, header, vinList, "",
                            ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                    continue;
                }
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00015",
                        tokenResult.getResult(),
                        request.getRegistrationSerialNumber(), vinList.getSequenceNumber(), header.getCorrelationId()));

                // 1:Notification登録判定&実行
                boolean registError = executeRegisterNotification(request, header, notificationData, vinList);
                if (registError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(request, header, vinList, notificationData.getInternalUserId(),
                            ERR_STATUS_ERR, ERR_STATUS_SKP, ERR_STATUS_SKP);
                    errorFlag = true;
                    continue;
                }
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00013",
                        notificationData.getInternalUserId(), vinList.getNotificationId(), header.getCorrelationId()));

                // 2:PUSH実行判定&実行
                boolean pushError = executePushNotification(
                        request, header, notificationData, vinList, tokenResult.getAljToken());
                if (pushError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(request, header, vinList, notificationData.getInternalUserId(),
                            ERR_STATUS_SUCCESS, ERR_STATUS_ERR, ERR_STATUS_SKP);
                    errorFlag = true;
                    continue;
                }

                // 3:PrimaryContact送信判定&実行
                boolean primaryContactError = executePrimaryContact(
                        request, header, notificationData, tokenResult.getAljToken(), vinList);
                if (primaryContactError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(request, header, vinList, notificationData.getInternalUserId(),
                            ERR_STATUS_SUCCESS, ERR_STATUS_SUCCESS, ERR_STATUS_ERR);
                    errorFlag = true;
                    continue;
                }
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00008",
                        notificationData.getInternalUserId(), header.getCorrelationId()));

            } catch (Exception e) {
                throw new CustomException(e);
            }
        }

        return errorFlag;
    }
    // endregion

    // #region 4-7-1:Notification登録判定&実行
    /**
     * Notification登録判定&実行
     * リクエストボディ.通知区分=1,3（NTF / NTF + MAIL or SMS）の場合、Notification登録を実行します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param vinList VINリストエンティティ
     * @return エラーフラグ（True:エラー発生, False:正常）
     */
    private boolean executeRegisterNotification(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            SaNotificationSendListDto notificationData,
            NotificationVinListEntity vinList) {

        try {
            if (Set.of(TYPE_NTF, TYPE_NTF_AND_MAILSMS).contains(request.getNotificationType())) {
                // Notification登録実行
                RegisterNotificationResponseDto responseDto = registerNotification(request, notificationData);
                if (responseDto == null) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00017",
                            notificationData.getInternalUserId(),
                            "Response DTO is null",
                            header.getCorrelationId()));
                    return true;
                }
                if (!responseDto.getReturnCode().equals(REGISTNOTIFICATION_SUCCESS)) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00017",
                            notificationData.getInternalUserId(),
                            responseDto.getMessage(),
                            header.getCorrelationId()));
                    return true;
                }
            }
            return false;

        } catch (Exception e) {
            executeErrorProcess(
                    request, header, vinList, notificationData.getInternalUserId(),
                    ERR_STATUS_ERR, ERR_STATUS_SKP, ERR_STATUS_SKP);
            throw e;
        }
    }

    /**
     * Notification登録実行部品
     * 
     * @param request          リクエストDTO
     * @param header           ヘッダーDTO
     * @param notificationData ループ中の通知データ
     */
    private RegisterNotificationResponseDto registerNotification(
            SendMessageNotificationRequestDto request,
            SaNotificationSendListDto notificationData) {

        // Notification登録処理実行
        ResponseEntity<String> response = batApisUtil.executeRegisterNotification(
                createRegisterNotificationRequestDto(request, notificationData));
        if (response == null || response.getBody() == null) {
            return null;
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(response.getBody(), RegisterNotificationResponseDto.class);
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    /**
     * Notification登録APIリクエストDTO生成
     * 
     * @param request リクエストDTO
     * @param user    ループ中のユーザー情報
     * @return 登録用リクエストDTO
     */
    private RegisterNotificationRequestDto createRegisterNotificationRequestDto(
            SendMessageNotificationRequestDto request,
            SaNotificationSendListDto notificationData) {
        RegisterNotificationRequestDto.NotificationTarget notificationTarget = new RegisterNotificationRequestDto.NotificationTarget(
                notificationData.getVin(),
                "Administrator",
                CNT_SA,
                notificationData.getInternalUserId());
        List<RegisterNotificationRequestDto.NotificationContent> contents = request.getNotificationContents().stream()
                .map(c -> new RegisterNotificationRequestDto.NotificationContent(
                        c.getLanguageCode(),
                        c.getTitle(),
                        c.getDlrMsgDatFmt(),
                        c.getDetail(),
                        c.getDlrSetUri(),
                        c.getDlrCntUrl()))
                .toList();
        return new RegisterNotificationRequestDto(notificationTarget, contents, "16");
    }
    // #endregion

    // #region 4-7-2:PUSH実行判定&実行
    /**
     * PUSH実行判定
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param user    ループ中のユーザー情報
     * @return エラーフラグ（True:エラー発生, False:正常）
     */
    private boolean executePushNotification(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            SaNotificationSendListDto notificationData,
            NotificationVinListEntity userInfo,
            String token) {

        try {

            if (request.getIsPushNotificationRequired().equals("1")) {
                // 登録済みデバイス取得実行
                NtfInfoEntity deviceData = getLatestDeviceData(notificationData.getInternalUserId());
                if (deviceData == null) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00007",
                            notificationData.getInternalUserId(), header.getCorrelationId()));
                    return true;
                }
                // ペイロード編集
                String payload = createPayload(request, header, userInfo, deviceData);
                payload = notificationHubUtil.replaceLcsSelected(payload, deviceData.getPlatformType(),
                        notificationData.getLicenseCode());

                // 通知送信実行
                PushRequestResponseDto obj = executePostMessage(header, notificationData, deviceData, payload, token);
                if (obj == null) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00009",
                            "response is null",
                            notificationData.getInternalUserId(),
                            payload, header.getCorrelationId()));
                    return true;
                }
                if (!obj.getResultCode().equals(JSAP_SUCCESS)) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00009",
                            obj.getResultCode(),
                            notificationData.getInternalUserId(),
                            payload, header.getCorrelationId()));
                    return true;
                }
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00006",
                        notificationData.getInternalUserId(), request.getPayload(),
                        deviceData.getPlatformType(), header.getCorrelationId()));
            }
            return false;

        } catch (Exception e) {
            executeErrorProcess(request, header, userInfo, notificationData.getInternalUserId(),
                    ERR_STATUS_SUCCESS, ERR_STATUS_ERR, ERR_STATUS_SKP);
            throw e;
        }
    }

    /**
     * ペイロードを編集します
     * 
     * @param request    リクエストDTO
     * @param deviceData 端末情報
     */
    private String createPayload(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            NotificationVinListEntity userInfo,
            NtfInfoEntity deviceData) {

        try {
            switch (deviceData.getPlatformType()) {
                case APN:
                    return notificationHubUtil.buildApnsPayload(request.getPayload());
                case FCM:
                    return notificationHubUtil.buildFcmV1Payload(request.getPayload());
                default:
                    throw new CustomException(); // 1,2以外は登録されないので基本到達しない。
            }

        } catch (Exception e) {
            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00008",
                    request.getRegistrationSerialNumber(), userInfo.getSequenceNumber(),
                    request.getPayload(), header.getCorrelationId()));
            throw new CustomException(e);
        }
    }

    /**
     * 通知送信を実行します
     * 
     * @param header       ヘッダーDTO
     * @param personalInfo 個人情報リストレスポンスDTO
     * @param deviceData   端末情報
     * @param payload      ペイロード文字列
     * @return NotificationOutcome
     */
    private PushRequestResponseDto executePostMessage(
            RequestHeaderDto header,
            SaNotificationSendListDto notificationData,
            NtfInfoEntity deviceData,
            String payload,
            String token) {

        try {
            LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00005",
                    notificationData.getInternalUserId(), payload, deviceData.getPlatformType(),
                    header.getCorrelationId()));
            ResponseEntity<String> jsapNotificationResponce = jsapUtil.executePushRequest(
                    notificationData.getUserId(), payload, token);
            if (jsapNotificationResponce == null || jsapNotificationResponce.getBody() == null) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                    jsapNotificationResponce.getBody(), PushRequestResponseDto.class);
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    /**
     * 更新日時最新の１件を抽出します
     * 
     * @param internalUserId
     * @return
     */
    private NtfInfoEntity getLatestDeviceData(String internalUserId) {
        List<NtfInfoEntity> deviceList = getAllDeviceData(internalUserId);
        if (deviceList.isEmpty()) {
            return null;
        }
        deviceList.sort((d1, d2) -> d2.getUpdatedAt().compareTo(d1.getUpdatedAt()));
        return deviceList.get(0);
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
    // #endregion

    // #region 4-7-3:PrimaryContact送信判定&実行
    /**
     * PrimaryContact送信判定
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param user    ループ中のユーザー情報
     */
    private boolean executePrimaryContact(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            SaNotificationSendListDto notificationData,
            String token,
            NotificationVinListEntity vinList) {

        try {

            if (request.getNotificationType().equals(TYPE_MAILSMS) ||
                    request.getNotificationType().equals(TYPE_NTF_AND_MAILSMS)) {

                // コンタクト取得
                GetUserInfoResponseDto personalInfoResponse = getPersonalInfo(
                        notificationData, token);
                if (personalInfoResponse == null || personalInfoResponse.getContactList().isEmpty()) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00015",
                            request.getRegistrationSerialNumber(),
                            vinList.getSequenceNumber(),
                            header.getCorrelationId()));
                    return true;
                }
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00010",
                        request.getRegistrationSerialNumber(),
                        vinList.getSequenceNumber(),
                        header.getCorrelationId()));

                // PrimaryContact送信処理実行
                return sendRequest(request, header, personalInfoResponse, token);
            }
            return false;

        } catch (Exception e) {
            executeErrorProcess(request, header, vinList, notificationData.getInternalUserId(),
                    ERR_STATUS_SUCCESS, ERR_STATUS_SUCCESS, ERR_STATUS_ERR);
            throw e;
        }
    }

    /**
     * ALJから連絡先リストを取得します。
     * 
     * @param request  リクエストDTO
     * @param header   ヘッダーDTO
     * @param userInfo ユーザー情報
     */
    private GetUserInfoResponseDto getPersonalInfo(
            SaNotificationSendListDto notificationSendList,
            String token) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            ResponseEntity<String> getUserInfoResponce = jsapUtil.executeGetUserInfo(
                    notificationSendList.getUserId(), token);
            GetUserInfoResponseDto getUserInfoDto = mapper.readValue(getUserInfoResponce.getBody(),
                    GetUserInfoResponseDto.class);
            if (!getUserInfoResponce.getStatusCode().is2xxSuccessful()) {
                return null;
            }
            return getUserInfoDto;
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    /**
     * 連絡先リストへ送信要求を実行します。
     * 
     * @param contactList 連絡先リスト
     * @param request     リクエストDTO
     * @param header      ヘッダーDTO
     * @return エラーフラグ（True:エラー発生, False:正常）
     */
    private boolean sendRequest(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            GetUserInfoResponseDto personalInfo,
            String token) {

        boolean errorFlag = false;
        for (var contact : personalInfo.getContactList()) {
            if (!contact.isPrimaryContactFlag()) {
                continue; // primaryContactFlagがfalseの場合はスキップ
            }
            Object context;
            if (contact.getContactType().equals(CONTACT_PHONE)) {
                // SMS送信要求
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00011",
                        personalInfo.getUserId(),
                        "",
                        contact.getContact(),
                        request.getTitle(),
                        header.getCorrelationId()));
                context = jsapUtil.createSmsContext(request.getBodyText());

            } else if (contact.getContactType().equals(CONTACT_EMAIL)) {
                // メール送信要求
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00012",
                        personalInfo.getUserId(),
                        "",
                        contact.getContact(),
                        request.getTitle(),
                        header.getCorrelationId()));
                context = jsapUtil.createMailContext(request.getBodyText(), request.getBodyHtml());

            } else {
                throw new CustomException();
            }

            errorFlag = executeSendMessage(request, contact, personalInfo.getUserId(), context, token);
        }
        return errorFlag;
    }

    /**
     * JSAPへメッセージ送信要求を実行します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param contact 連絡先DTO
     * @param userId  ユーザーID
     * @param token   JSAP認証トークン
     * @return なし
     */
    private boolean executeSendMessage(
            SendMessageNotificationRequestDto request,
            GetUserInfoResponseDto.ContactDto contact,
            String userId,
            Object context,
            String token) {

        try {
            ResponseEntity<String> sendMessageResponse = jsapUtil.executeSendMessage(
                    null, userId, contact.getContactType(),
                    request.getTitle(), context, token);
            if (sendMessageResponse == null || sendMessageResponse.getBody() == null) {
                return true;
            }

            ObjectMapper mapper = new ObjectMapper();
            SendMessageResponseDto sendMessageDto = mapper.readValue(sendMessageResponse.getBody(),
                    SendMessageResponseDto.class);

            return !sendMessageDto.getResultCode().equals(JSAP_SUCCESS);

        } catch (Exception e) {
            throw new CustomException(e);
        }
    }
    // #endregion

    // #region 5:連携フラグ更新
    /**
     * notificationテーブルを更新します。
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     */
    private int updateNotification(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header) {

        try {
            int updateCnt = notificationRepository.update(CNT_SA, request.getRegistrationSerialNumber());
            if (updateCnt == 0) {
                LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00021",
                        TBL_NOTIFICATION,
                        "updateNotification",
                        request.getRegistrationSerialNumber(),
                        header.getCorrelationId()));
                throw new CustomSqlException();
            }
            LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00020",
                    TBL_NOTIFICATION, "", "",
                    header.getCorrelationId()));
            return updateCnt;

        } catch (Exception e) {
            throw new CustomSqlException(TBL_NOTIFICATION, e);
        }
    }
    // #endregion

    // #region エラー処理
    /**
     * エラー処理部品
     * 
     * @param request
     * @param header
     * @param userInfo
     * @param personalInfo
     * @param notificationStatus
     * @param pushStatus
     * @param primaryContactStatus
     */
    private void executeErrorProcess(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            NotificationVinListEntity userInfo,
            String internalUserId,
            String notificationStatus,
            String pushStatus,
            String primaryContactStatus) {

        try {
            ntfBatchExecErrorInfoRepository.insert(
                    new NtfBatchExecErrorInfoEntity(
                            request.getRegistrationSerialNumber(),
                            userInfo.getSequenceNumber().intValue(),
                            internalUserId,
                            notificationStatus,
                            pushStatus,
                            primaryContactStatus,
                            null,
                            null,
                            null));

            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07D00007",
                    request.getRegistrationSerialNumber(),
                    userInfo.getSequenceNumber(),
                    notificationStatus,
                    pushStatus,
                    primaryContactStatus,
                    header.getCorrelationId()));

        } catch (Exception e) {
            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00024",
                    e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
        }
    }
    // #endregion
}