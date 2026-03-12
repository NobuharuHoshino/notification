package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sendgrid.Response;
import com.sendgrid.helpers.mail.Mail;
import com.toyota.tsc.notificationhub.commons.BatApisUtil;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.commons.NotificationHubUtil;
import com.toyota.tsc.notificationhub.commons.PersonalInfoUtil;
import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
import com.toyota.tsc.notificationhub.commons.SendGridUtil;
import com.toyota.tsc.notificationhub.commons.SmsCountryUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.models.NotificationSendListDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendMessageNotificationRequestDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoResponseDto;
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
import com.windowsazure.messaging.NotificationHubsException;
import com.windowsazure.messaging.NotificationOutcome;

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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * お知らせ通知送信サービス実装クラス
 */
@Profile("me")
@Service
public class SendMessageNotificationServiceImpl implements SendMessageNotificationServiceIF {

    // #region DI
    private final NotificationRepositoryIF notificationRepository;
    private final NotificationVinListRepositoryIF notificationVinListRepository;
    private final NtfBatchExecErrorInfoRepositoryIF ntfBatchExecErrorInfoRepository;
    private final NtfBatchExecHistoryRepositoryIF ntfBatchExecHistoryRepository;
    private final NtfInfoRepositoryIF ntfInfoRepository;
    private final BatApisUtil batApisUtil;
    private final NotificationHubUtil notificationHubUtil;
    private final PersonalInfoUtil personalInfoUtil;
    private final PropertiesUtil properties;
    private final SendGridUtil sendGridUtil;
    private final SmsCountryUtil smsCountryUtil;

    public SendMessageNotificationServiceImpl(NotificationRepositoryIF notificationRepository,
            NotificationVinListRepositoryIF notificationVinListRepository,
            NtfBatchExecErrorInfoRepositoryIF ntfBatchExecErrorInfoRepository,
            NtfBatchExecHistoryRepositoryIF ntfBatchExecHistoryRepository, NtfInfoRepositoryIF ntfInfoRepository,
            BatApisUtil batApisUtil, NotificationHubUtil notificationHubUtil, PersonalInfoUtil personalInfoUtil,
            PropertiesUtil properties, SendGridUtil sendGridUtil, SmsCountryUtil smsCountryUtil) {
        this.notificationRepository = notificationRepository;
        this.notificationVinListRepository = notificationVinListRepository;
        this.ntfBatchExecErrorInfoRepository = ntfBatchExecErrorInfoRepository;
        this.ntfBatchExecHistoryRepository = ntfBatchExecHistoryRepository;
        this.ntfInfoRepository = ntfInfoRepository;
        this.batApisUtil = batApisUtil;
        this.notificationHubUtil = notificationHubUtil;
        this.personalInfoUtil = personalInfoUtil;
        this.properties = properties;
        this.sendGridUtil = sendGridUtil;
        this.smsCountryUtil = smsCountryUtil;
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
    // オンプレ個人情報リスト取得API 正常終了コード
    private static final String GETPERSONAL_SUCCESS = "00001581U000";
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
    private static final String CNT_ME = "ME";
    // 連携フラグ
    private static final int LINKTYPE_NOTLINKED = 0;
    private static final int LINKTYPE_LINKED = 2;
    // オンプレAPI多重度制御用ロックオブジェクト
    private static final Object ONPREM_LOCK = new Object();
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
            List<NotificationVinListEntity> vinList = getUserInfoAndUpdateFlag(request, header);

            // 4:ユーザー情報分のループ処理（親ループ）実行
            executeParentNotificationProcess(request, header, vinList);

            // 5:お知らせ情報連携フラグ更新
            updateNotification(request, header);

            // 6:終了ログ
            LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00002",
                    PROCESS_NAME, CommonUtil.getResultCode(RESULT_SUCCESS), header.getCorrelationId()));

            return new ResponseDto(CommonUtil.getResultCode(RESULT_SUCCESS));

        } catch (TscApplicationException e) {
            // アプリエラー：各発生源でログ出力 & GlobalExceptionHandlerで400返却
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
    private void validate(SendMessageNotificationRequestDto request, RequestHeaderDto header) {

        // 必須項目検証
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00003",
                    missingField,
                    header.getCorrelationId()));
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

        // 必須検証
        if (request == null) {
            return "requestBody";
        }
        List<String> missingFields = new ArrayList<>();
        if (request.getRegistrationSerialNumber() == null) {
            missingFields.add("registrationSerialNumber");
        }
        if (request.getNotificationType() == null || request.getNotificationType().isEmpty()) {
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

    // #region 4:ユーザー情報分のループ処理実行
    /**
     * ユーザー情報分のループ処理実行部品
     * 4-1:通知基盤連携情報リストのオブジェクトマッピング
     * 4-2:お知らせ通知処理履歴登録
     * 4-3:NotificationVinList連携フラグ更新
     * 4-4:オンプレ個人情報リスト取得
     * 4-5:非同期で通知処理実行
     * X:エラー発生時はログ出力とエラー情報登録して次のループへ
     *
     * @param request
     * @param header
     * @param vinList
     */
    private void executeParentNotificationProcess(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            List<NotificationVinListEntity> vinList) {

        // 並列スレッド定義
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                properties.getThreadPool(),
                properties.getThreadPool(),
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.getThreadQueue()), r -> {
                    Thread t = new Thread(r);
                    t.setName("ntf-worker-" + t.getName());
                    t.setDaemon(true);
                    return t;
                }, new ThreadPoolExecutor.AbortPolicy());

        // VINリストでループ実行（子プロセスを非同期でループ継続）
        List<CompletableFuture<Void>> f = new ArrayList<>();
        try {
            vinList.forEach(entity -> {

                LogUtil.info(getClass(), "Starting parent process" + entity.getSequenceNumber());
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
                    List<NotificationSendListDto> notificationSendList = convertToNotificationSendListDto(
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

                    // 4-4:非同期で通知処理実行
                    LogUtil.info(getClass(), "Starting child process" + entity.getSequenceNumber());
                    f.add(CompletableFuture.runAsync(() -> {
                        try {
                            // 通知処理実行
                            boolean errorFlag = this.executeNotificationProcess(
                                    request, header, entity, notificationSendList);
                            // お知らせ通知処理履歴更新
                            if (errorFlag) {
                                ntfBatchExecHistoryRepository.updateStatus(request.getRegistrationSerialNumber(),
                                        entity.getSequenceNumber().intValue(), STATUS_ERR);
                                LogUtil.info(getClass(),
                                        CommonUtil.getLogsMessage("RS07D00006",
                                                request.getRegistrationSerialNumber(),
                                                entity.getSequenceNumber(),
                                                STATUS_ERR + "（処理終了）",
                                                header.getCorrelationId()));
                            } else {
                                ntfBatchExecHistoryRepository.updateStatus(request.getRegistrationSerialNumber(),
                                        entity.getSequenceNumber().intValue(), STATUS_DONE);
                                LogUtil.info(getClass(),
                                        CommonUtil.getLogsMessage("RS07D00006",
                                                request.getRegistrationSerialNumber(),
                                                entity.getSequenceNumber(),
                                                STATUS_DONE + "（処理終了）",
                                                header.getCorrelationId()));
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
                    } else {
                        // その他エラー
                        LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00001",
                                PROCESS_NAME,
                                e.getMessage(),
                                e.getStackTrace(),
                                header.getCorrelationId()));
                    }
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
    // endregion

    // #region 4-3:通知基盤連携情報リスト取得
    /**
     * notification_vin_listテーブルから取得した通知基盤連携情報をList<DTO>にマッピングします。
     * 通知基盤連携情報はレコードカンマ区切り&フィールドコロン区切りのテキストデータです。
     * ※[vin]:[internalUserIId]:[lisenceCode]:[brdCd]
     * 
     * @param csvText notification_vin_list.notification_send_list
     * @return マッピングされたNotificationSendListDtoのリスト
     */
    private List<NotificationSendListDto> convertToNotificationSendListDto(String csvText) {
        try {
            return Arrays.stream(csvText.split(","))
                    .map(r -> r.split(":"))
                    .map(p -> new NotificationSendListDto(p[0], p[1], p[2], p[3]))
                    .toList();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    // endregion

    // #region 4-4:通知処理実行
    /**
     * 通知処理実行基幹
     * 1:Notification登録判定&実行
     * 2:PUSH実行判定&実行
     * 3:PrimaryContact送信判定&実行
     * NG発生時は共通のエラー処理（通知バッチエラーテーブル登録） X:想定外の例外時は例外スローして処理ループを抜ける
     *
     * @param request
     * @param header
     * @param userInfo
     * @param notificationSendList
     * @param personalInfoListResponse
     */
    private boolean executeNotificationProcess(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            NotificationVinListEntity vinList,
            List<NotificationSendListDto> notificationSendList) {

        boolean errorFlag = false;
        // UserList（個人情報リスト）でループ
        for (NotificationSendListDto notificationData : notificationSendList) {
            try {

                // 1:Notification登録判定&実行
                boolean registError = executeRegisterNotification(
                        request, header, notificationData, vinList);
                if (registError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(
                            request, header, vinList, notificationData.getInternalUserId(),
                            ERR_STATUS_ERR, ERR_STATUS_SKP, ERR_STATUS_SKP);
                    errorFlag = true;
                    continue;
                }
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00013",
                        notificationData.getInternalUserId(),
                        vinList.getNotificationId(),
                        header.getCorrelationId()));

                // 2:PUSH実行判定&実行
                boolean pushError = executePushNotification(
                        request, header, notificationData, vinList);
                if (pushError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(
                            request, header, vinList, notificationData.getInternalUserId(),
                            ERR_STATUS_SUCCESS, ERR_STATUS_ERR, ERR_STATUS_SKP);
                    errorFlag = true;
                    continue;
                }

                // 3:PrimaryContact送信判定&実行
                boolean primaryContactError = executePrimaryContact(
                        request, header, vinList, notificationData);
                if (primaryContactError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(
                            request, header, vinList, notificationData.getInternalUserId(),
                            ERR_STATUS_SUCCESS, ERR_STATUS_SUCCESS, ERR_STATUS_ERR);
                    errorFlag = true;
                    continue;
                }
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00008",
                        notificationData.getInternalUserId(),
                        header.getCorrelationId()));

            } catch (Exception e) {
                // X:システム例外時は例外スローして処理ループを抜ける
                throw new CustomException(e);
            }
        }

        return errorFlag;
    }
    // endregion

    // #region 4-4-1:Notification登録判定&実行
    /**
     * Notification登録判定&実行 リクエストボディ.通知区分=1,3（NTF / NTF + MAIL or
     * SMS）の場合、Notification登録を実行します。
     *
     * @param request          リクエストDTO
     * @param header           ヘッダーDTO
     * @param personalInfoList 個人情報リストレスポンスDTO
     * @return エラーフラグ（True:エラー発生, False:正常）
     */
    private boolean executeRegisterNotification(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            NotificationSendListDto notificationData,
            NotificationVinListEntity vinList) {

        try {
            if (Set.of(TYPE_NTF, TYPE_NTF_AND_MAILSMS).contains(request.getNotificationType())) {

                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00022",
                        "Notification登録", notificationData.getInternalUserId(), header.getCorrelationId()));
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
            NotificationSendListDto notificationData) {

        // Notification登録処理実行
        ResponseEntity<String> response = batApisUtil
                .executeRegisterNotification(
                        createRegisterNotificationRequestDto(request, notificationData));
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(
                    response.getBody(),
                    RegisterNotificationResponseDto.class);
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }

    /**
     * Notification登録APIリクエストDTO生成
     *
     * @param request          リクエストDTO
     * @param notificationData ループ中の通知データ
     * @return 登録用リクエストDTO
     */
    private RegisterNotificationRequestDto createRegisterNotificationRequestDto(
            SendMessageNotificationRequestDto request,
            NotificationSendListDto notificationData) {

        // NotificationTarget生成
        RegisterNotificationRequestDto.NotificationTarget notificationTarget = new RegisterNotificationRequestDto.NotificationTarget(
                notificationData.getVin(),
                "Administrator",
                CNT_ME,
                notificationData.getInternalUserId());
        // NotificationContent生成（リクエストのNotificationContentをRegisterNotification用に移し替え）
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

    // #region 4-4-2:PUSH実行判定&実行
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
            NotificationSendListDto notificationData,
            NotificationVinListEntity userInfo) {

        try {
            if (request.getIsPushNotificationRequired().equals("1")) {

                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00022",
                        "Push通知送信", notificationData.getInternalUserId(), header.getCorrelationId()));
                // 登録済みデバイス取得実行
                NtfInfoEntity deviceData = getLatestDeviceData(notificationData.getInternalUserId());
                if (deviceData == null) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00007",
                            notificationData.getInternalUserId(),
                            header.getCorrelationId()));
                    return true;
                }

                // ペイロード編集
                String payload = createPayload(request, header, userInfo, deviceData);
                // ペイロードに内部ライセンスコード組み込み
                payload = notificationHubUtil.replaceLcsSelected(payload, deviceData.getPlatformType(),
                        notificationData.getLicenseCode());

                // 通知送信実行
                NotificationOutcome outcome = executePostMessage(header, notificationData, deviceData, payload);
                if (outcome == null) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00009",
                            0,
                            notificationData.getInternalUserId(),
                            payload,
                            deviceData.getInstallationId(),
                            header.getCorrelationId()));
                    return true;
                }

                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00006",
                        notificationData.getInternalUserId(),
                        request.getPayload(),
                        deviceData.getPlatformType(),
                        header.getCorrelationId()));
            }
            return false;

        } catch (Exception e) {
            executeErrorProcess(
                    request, header, userInfo, notificationData.getInternalUserId(),
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
                    request.getRegistrationSerialNumber(),
                    userInfo.getSequenceNumber(),
                    request.getPayload(),
                    header.getCorrelationId()));
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
    private NotificationOutcome executePostMessage(
            RequestHeaderDto header,
            NotificationSendListDto notificationData,
            NtfInfoEntity deviceData,
            String payload) {

        LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00005",
                notificationData.getInternalUserId(),
                payload,
                deviceData.getPlatformType(),
                header.getCorrelationId()));
        int cnt = 0;

        while (cnt < properties.getRetryCount()) {
            try {
                return notificationHubUtil.postMessage(
                        deviceData.getInstallationId(),
                        payload,
                        deviceData.getBrdCd(),
                        deviceData.getPlatformType());

            } catch (NotificationHubsException ex) {
                if (ex.isTransient()) {
                    cnt++;
                    if (cnt >= properties.getRetryCount()) {
                        LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00009",
                                ex.httpStatusCode(),
                                notificationData.getInternalUserId(),
                                payload,
                                deviceData.getInstallationId(),
                                header.getCorrelationId()));
                        return null;
                    }
                    LogUtil.warn(getClass(), CommonUtil.getLogsMessage("RS07W00003",
                            cnt,
                            ex.httpStatusCode(),
                            notificationData.getInternalUserId(),
                            payload,
                            deviceData.getInstallationId(),
                            header.getCorrelationId(),
                            String.valueOf(cnt)));
                    continue;
                }
                LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00009",
                        ex.httpStatusCode(),
                        notificationData.getInternalUserId(),
                        payload, deviceData.getInstallationId(), header.getCorrelationId()));
                return null;

            } catch (Exception e) {
                throw new CustomException(e);
            }
        }
        return null;
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

    // #region 4-4-3:PrimaryContact送信判定&実行
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
            NotificationVinListEntity vinList,
            NotificationSendListDto notificationData) {

        try {

            if (request.getNotificationType().equals(TYPE_MAILSMS)
                    || request.getNotificationType().equals(TYPE_NTF_AND_MAILSMS)) {

                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00022",
                        "PrimaryContact送信", notificationData.getInternalUserId(), header.getCorrelationId()));

                // オンプレ個人情報取得
                PersonalInfoResponseDto personalInfoResponse = getPersonalInfo(
                        header, notificationData.getInternalUserId());
                if (personalInfoResponse == null) {
                    LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00015",
                            request.getRegistrationSerialNumber(),
                            vinList.getSequenceNumber(),
                            header.getCorrelationId()));
                    return true;
                }
                if (!personalInfoResponse.getResultCode().equals(GETPERSONAL_SUCCESS)) {
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
                return sendRequest(request, header, personalInfoResponse, notificationData.getBrdCd());
            }
            return false;

        } catch (Exception e) {
            executeErrorProcess(
                    request, header, vinList, notificationData.getInternalUserId(),
                    ERR_STATUS_SUCCESS, ERR_STATUS_SUCCESS, ERR_STATUS_ERR);
            throw e;
        }
    }

    /**
     * オンプレ個人情報取得APIから連絡先リストを取得します。
     *
     * @param request  リクエストDTO
     * @param header   ヘッダーDTO
     * @param userInfo ユーザー情報
     */
    private PersonalInfoResponseDto getPersonalInfo(
            RequestHeaderDto header,
            String internalUserId) {

        // API実行（synchronizedを利用してAPI呼出しのみ直列実行）
        PersonalInfoResponseDto response;
        synchronized (ONPREM_LOCK) {
            response = personalInfoUtil.getPersonalInfoApiResponse(
                    internalUserId, header.getCorrelationId());
        }

        if (response == null) {
            return null;
        }
        List<PersonalInfoResponseDto.ContactDto> contactList = response.getContactList();
        if (contactList == null || contactList.isEmpty()) {
            return null;
        }

        return response;
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
            PersonalInfoResponseDto personalInfo,
            String brdCd) {

        boolean errorFlag = false;
        for (var contact : personalInfo.getContactList()) {
            if (!contact.isPrimaryContactFlag()) {
                continue; // primaryContactFlagがfalseの場合はスキップ
            }
            if (contact.getContactType().equals(CONTACT_PHONE)) {
                // SMS送信要求
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00011",
                        personalInfo.getInternalUserId(),
                        brdCd,
                        contact.getContact(),
                        request.getTitle(),
                        header.getCorrelationId()));
                errorFlag |= executeSendSms(request, header, contact.getContact(), brdCd);

            } else if (contact.getContactType().equals(CONTACT_EMAIL)) {
                // メール送信要求
                LogUtil.info(getClass(), CommonUtil.getLogsMessage("RS07I00012",
                        personalInfo.getInternalUserId(),
                        brdCd,
                        contact.getContact(),
                        request.getTitle(),
                        header.getCorrelationId()));
                errorFlag |= executeSendEmail(request, header, contact.getContact(), brdCd);
            }
        }

        return errorFlag;
    }

    /**
     * SMS送信処理を実行します。
     *
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param phoneNo 送信先電話番号
     * @return エラーフラグ（True:エラー発生, False:正常）
     */
    private boolean executeSendSms(SendMessageNotificationRequestDto request, RequestHeaderDto header, String phoneNo,
            String brdCd) {

        ResponseEntity<String> smsResponse = smsCountryUtil.executeSendSms(phoneNo, request.getBodySms(), brdCd);
        if (!smsResponse.getStatusCode().is2xxSuccessful()) {
            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00010",
                    smsResponse.getStatusCode().value(),
                    CommonUtil.maskPhoneNumber(phoneNo),
                    header.getCorrelationId()));
            return true;
        }
        return false;
    }

    /**
     * メール送信処理を実行します。
     *
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param email   送信先メールアドレス
     * @return エラーフラグ（True:エラー発生, False:正常）
     */
    private boolean executeSendEmail(SendMessageNotificationRequestDto request, RequestHeaderDto header, String email,
            String brdCd) {

        Mail mail = sendGridUtil.generateEmail(email, request.getTitle(), request.getBodyText(), request.getBodyHtml(),
                brdCd);
        Response response = sendGridUtil.executeSendEmail(mail);
        if (!HttpStatusCode.valueOf(response.getStatusCode()).is2xxSuccessful()) {
            LogUtil.error(getClass(), CommonUtil.getLogsMessage("RS07E00010",
                    response.getStatusCode(),
                    CommonUtil.maskText(email),
                    header.getCorrelationId()));
            return true;
        }
        return false;
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
            int updateCnt = notificationRepository.update(CNT_ME, request.getRegistrationSerialNumber());
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