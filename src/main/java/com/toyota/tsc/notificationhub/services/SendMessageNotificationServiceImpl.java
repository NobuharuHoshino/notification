
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
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.models.NotificationSendListDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoListRequestDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SendMessageNotificationRequestDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto.PersonalInfoList;
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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
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

    public SendMessageNotificationServiceImpl(
            NotificationRepositoryIF notificationRepository,
            NotificationVinListRepositoryIF notificationVinListRepository,
            NtfBatchExecErrorInfoRepositoryIF ntfBatchExecErrorInfoRepository,
            NtfBatchExecHistoryRepositoryIF ntfBatchExecHistoryRepository,
            NtfInfoRepositoryIF ntfInfoRepository,
            BatApisUtil batApisUtil,
            NotificationHubUtil notificationHubUtil,
            PersonalInfoUtil personalInfoUtil,
            PropertiesUtil properties,
            SendGridUtil sendGridUtil,
            SmsCountryUtil smsCountryUtil) {
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
    // 内部UserIDリスト 分割チャンクサイズ
    private static final int CHUNK_SIZE = 100;
    // スレッド数, 待機キュー
    private static final int THREADPOOLS = 200;
    private static final int THREADPOOLS_QUEUE = 1000;
    // 連携フラグ
    private static final int LINKTYPE_NOTLINKED = 0;
    private static final int LINKTYPE_LINKED = 2;
    // オンプレAPI多重度制御用ロックオブジェクト
    private static final Object ONPREM_LOCK = new Object();
    // #endregion

    @Override
    public ResponseDto sendMessageNotification(SendMessageNotificationRequestDto request, RequestHeaderDto header) {

        try {
            // 開始ログ
            LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00001",
                    PROCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            validate(request, header);

            // ユーザー情報取得
            List<NotificationVinListEntity> userInfo = getUserInfoAndUpdateFlag(request, header);

            // 登録連番分のループ処理実行
            executeNotificationProcess(request, header, userInfo);

            // お知らせ情報連携フラグ更新
            System.out.println("到達確認nf！！！！！！");
            this.notificationRepository.update(CNT_ME, request.getRegistrationSerialNumber());

            // 終了ログ
            LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00002",
                    PROCESS_NAME, CommonUtil.getResultCode(RESULT_SUCCESS), header.getCorrelationId()));

            return new ResponseDto(CommonUtil.getResultCode(RESULT_SUCCESS));

        } catch (TscApplicationException e) {
            throw new TscApplicationException(e.getResultCode());

        } catch (Exception e) {

            // notification_vin_list取得でエラー発生時、処理を終了
            SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
            if (sqlEx != null) {
                if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                    // 接続エラーログ出力
                    LogUtil.error(getClass(), CommonUtil.getBatMessage(
                            "RS07E00010", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                    // 操作エラーログ出力
                    System.out.println("Exception 非同期メイン！！！！！！");
                    LogUtil.error(getClass(), CommonUtil.getBatMessage(
                            "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(), sqlEx.getSQLState(),
                            sqlEx.getErrorCode(), header.getCorrelationId()));
                }
                // エラー共通ログ出力
                LogUtil.error(getClass(), CommonUtil.getBatMessage(
                        "RS07E00001", PROCESS_NAME, e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));

            } else {
                // エラー共通ログ出力
                LogUtil.error(getClass(), CommonUtil.getBatMessage(
                        "RS07E00001", PROCESS_NAME, e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
            }
        }
    }

    // #region 1:バリデーション検証
    /**
     * リクエストの必須項目・値を検証します。
     *
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     */
    private void validate(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header) {

        // 必須項目検証
        String missingField = validateRequired(request);
        if (missingField != null) {
            LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00012",
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

        // 必須検証
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
        if (!missingFields.isEmpty()) {
            return String.join(",", missingFields);
        }
        return null;
    }
    // endregion

    // #region 2:ユーザー情報取得
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

        List<NotificationVinListEntity> userInfo = notificationVinListRepository.select(
                request.getRegistrationSerialNumber(), LINKTYPE_NOTLINKED);
        if (userInfo == null || userInfo.isEmpty()) {
            LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07I00024",
                    request.getRegistrationSerialNumber(), header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_USERINFO_EMPTY));
        }
        return userInfo;
    }
    // endregion

    // #region 3:登録連番分のループ処理実行
    /**
     * 登録連番分のループ処理実行部品
     * 3-1:通知基盤連携情報リストのオブジェクトマッピング
     * 3-2:お知らせ通知処理履歴登録
     * 3-3:NotificationVinList連携フラグ更新
     * 3-4:オンプレ個人情報リスト取得
     * 3-5:非同期で通知処理実行
     * X:エラー発生時はログ出力とエラー情報登録して次のループへ
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
                THREADPOOLS, THREADPOOLS,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(THREADPOOLS_QUEUE),
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

                    // 3-1:通知基盤連携情報をListオブジェクトで取得(Max1000件)
                    List<NotificationSendListDto> notificationSendList = convertToNotificationSendListDto(
                            entity.getNotificationSendList());
                    if (notificationSendList.isEmpty()) {
                        LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00021",
                                request.getRegistrationSerialNumber(), entity.getSequenceNumber(),
                                header.getCorrelationId()));
                        executeErrorProcess(request, header, entity,
                                "", ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                        return;
                    }
                    // 3-2:お知らせ通知処理履歴登録（更新0の場合は異常終了）
                    int insertCnt = this.insertHistory(request, header, entity);
                    if (insertCnt == 0) {
                        throw new SQLException("Insert count for ntf_batch_exec_history was 0.");
                    }
                    // 3-3:NotificationVinList連携フラグ更新（更新0の場合は異常終了）
                    int updCnt = this.updateNotificationVinList(request, entity, LINKTYPE_NOTLINKED);
                    if (updCnt == 0) {
                        throw new SQLException("Update count for notification_vin_list was 0.");
                    }
                    // 3-4:連絡先リスト取得
                    PersonalInfoListResponseDto personalInfoListResDto = this.getPersonalInfoList(
                            header, notificationSendList);
                    if (personalInfoListResDto == null) {
                        LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00015",
                                request.getRegistrationSerialNumber(), entity.getSequenceNumber(),
                                header.getCorrelationId()));
                        executeErrorProcess(request, header, entity,
                                "", ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                        return;
                    }
                    // 3-5:非同期で通知処理実行
                    f.add(CompletableFuture.runAsync(() -> {
                        try {
                            boolean errorFlag = this.executeNotificationProcess(
                                    request, header, entity, personalInfoListResDto, notificationSendList);
                            // お知らせ通知処理履歴更新
                            if (errorFlag) {

                                System.out.println("到達確認his！！！！！！");
                                ntfBatchExecHistoryRepository.updateStatus(
                                        request.getRegistrationSerialNumber(),
                                        Integer.valueOf(entity.getSequenceNumber()),
                                        STATUS_ERR);
                                LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07D00007",
                                        request.getRegistrationSerialNumber(), entity.getSequenceNumber(),
                                        STATUS_ERR + "（処理終了）", header.getCorrelationId()));
                            } else {

                                System.out.println("到達確認his！！！！！！");
                                ntfBatchExecHistoryRepository.updateStatus(
                                        request.getRegistrationSerialNumber(),
                                        Integer.valueOf(entity.getSequenceNumber()),
                                        STATUS_DONE);
                                LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07D00007",
                                        request.getRegistrationSerialNumber(), entity.getSequenceNumber(),
                                        STATUS_DONE + "（処理終了）", header.getCorrelationId()));
                            }
                            // NotificationVinList連携フラグ更新
                            System.out.println("到達確認vin！！！！！！");
                            this.updateNotificationVinList(request, entity, LINKTYPE_LINKED);
                        } catch (Exception e) {
                            // 非同期処理内で発生した例外はログ出力と履歴更新
                            SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
                            if (sqlEx != null) {
                                if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                                    // 接続エラーログ出力
                                    LogUtil.error(getClass(), CommonUtil.getBatMessage(
                                            "RS07E00010", sqlEx.getMessage(), sqlEx.getStackTrace(),
                                            header.getCorrelationId()));
                                } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                                    // 操作エラーログ出力
                                    System.out.println("Exception 非同期内部！！！！！！");
                                    LogUtil.error(getClass(), CommonUtil.getBatMessage(
                                            "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(),
                                            sqlEx.getSQLState(), sqlEx.getErrorCode(), header.getCorrelationId()));
                                }
                                // エラー共通ログ出力
                                LogUtil.error(getClass(), CommonUtil.getBatMessage(
                                        "RS07E00001", PROCESS_NAME, e.getMessage(), e.getStackTrace(),
                                        header.getCorrelationId()));
                            } else {
                                // エラー共通ログ出力
                                LogUtil.error(getClass(), CommonUtil.getBatMessage(
                                        "RS07E00001", PROCESS_NAME, e.getMessage(), e.getStackTrace(),
                                        header.getCorrelationId()));
                            }
                            ntfBatchExecHistoryRepository.updateStatus(
                                    request.getRegistrationSerialNumber(),
                                    Integer.valueOf(entity.getSequenceNumber()),
                                    STATUS_ERR);
                            this.updateNotificationVinList(request, entity, LINKTYPE_LINKED);
                        }
                    }, executor));

                } catch (Exception e) {

                    // ループ処理内で発生した例外はログ出力とエラー情報登録して次のループへ
                    SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
                    if (sqlEx != null) {
                        if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {

                            // 接続エラーログ出力
                            LogUtil.error(getClass(), CommonUtil.getBatMessage(
                                    "RS07E00010", sqlEx.getMessage(), sqlEx.getStackTrace(),
                                    header.getCorrelationId()));
                        } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                            // 操作エラーログ出力
                            System.out.println("Exception 非同期外部！！！！！！");
                            LogUtil.error(getClass(), CommonUtil.getBatMessage(
                                    "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(), sqlEx.getSQLState(),
                                    sqlEx.getErrorCode(), header.getCorrelationId()));
                        } else {
                            // 更新数0などの理由でthrowされたSQLExceptionのログ出力（操作エラーとして出力）
                            LogUtil.error(getClass(), CommonUtil.getBatMessage(
                                    "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(), sqlEx.getSQLState(),
                                    sqlEx.getErrorCode(), header.getCorrelationId()));
                        }
                    } else {
                        // エラー共通ログ出力
                        LogUtil.error(getClass(), CommonUtil.getBatMessage(
                                "RS07E00001", PROCESS_NAME, e.getMessage(), e.getStackTrace(),
                                header.getCorrelationId()));
                    }
                    // エラー登録して次のループへ
                    executeErrorProcess(request, header, entity,
                            "", ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
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

    // #region 3-1:通知基盤連携情報をListオブジェクトで取得(Max1000件)
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
    // #endregion

    // #region 3-2:お知らせ通知処理履歴登録
    /**
     * お知らせ通知処理履歴テーブルに処理開始情報を新規登録
     * テーブルエンティティにINSERT項目を格納して登録処理を実行
     *
     * @param request
     * @param header
     * @param internalUserIdList
     */
    private int insertHistory(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            NotificationVinListEntity userInfo) {

        NtfBatchExecHistoryEntity historyEntity = new NtfBatchExecHistoryEntity(
                request.getRegistrationSerialNumber(),
                Integer.valueOf(userInfo.getSequenceNumber()),
                STATUS_STR,
                request.getScheduledSendData(),
                null,
                null,
                null,
                null);
        int insertCnt = ntfBatchExecHistoryRepository.insert(historyEntity);
        LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07D00007",
                request.getRegistrationSerialNumber(), userInfo.getSequenceNumber(),
                STATUS_STR + "（処理開始）", header.getCorrelationId()));
        return insertCnt;
    }
    // endregion

    // #region 3-3:連携フラグ更新
    /**
     * NotificationVinListの連携フラグを更新します。
     */
    private int updateNotificationVinList(
            SendMessageNotificationRequestDto request,
            NotificationVinListEntity userInfo,
            int status) {

        return notificationVinListRepository.update(
                request.getRegistrationSerialNumber(),
                userInfo.getSequenceNumber(),
                status);
    }
    // endregion

    // #region 3-4:連絡先リスト取得
    /**
     * オンプレ個人情報取得APIから連絡先リストを取得します。
     * 
     * @param request  リクエストDTO
     * @param header   ヘッダーDTO
     * @param userInfo ユーザー情報
     */
    private PersonalInfoListResponseDto getPersonalInfoList(
            RequestHeaderDto header,
            List<NotificationSendListDto> notificationSendList) {

        final ObjectMapper mapper = new ObjectMapper();
        final int pal = properties.getParallelCurrent();
        final Semaphore semaphore = new Semaphore(pal);

        // 多重度=palで並列処理実行
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Max1000件のリストを100件単位のリスト<リスト>に分割して取得
            List<List<NotificationSendListDto>> notificationSendListChunkList = getNotificationChunkList(
                    notificationSendList);
            final CompletionService<PersonalInfoListResponseDto> completion = new ExecutorCompletionService<>(executor);
            final List<Future<PersonalInfoListResponseDto>> futures = new ArrayList<>(
                    notificationSendListChunkList.size());

            // 並列処理タスク投入（100件単位の内部UserIDリスト数に応じたAPI呼び出し）
            for (List<NotificationSendListDto> notificationSendListChunk : notificationSendListChunkList) {

                futures.add(completion.submit(() -> {

                    // 処理枠（=多重度）を確保
                    semaphore.acquire();
                    try {

                        // リクエストDTOを生成
                        PersonalInfoListRequestDto request = new PersonalInfoListRequestDto(
                                "2", "", "",
                                notificationSendListChunk.stream()
                                        .map(NotificationSendListDto::getInternalUserId)
                                        .toList());
                        // API実行（synchronizedを利用してAPI呼出しのみ直列実行）
                        ResponseEntity<String> response;
                        synchronized (ONPREM_LOCK) {
                            int index = notificationSendListChunkList.indexOf(notificationSendListChunk) + 1;
                            System.out.println(
                                    "直列実行確認：" + notificationSendListChunk.get(0).getInternalUserId() + " - " + index);
                            response = personalInfoUtil.getPersonalInfoListApiResponse(
                                    request, header.getCorrelationId());
                        }
                        // 異常検知の場合null返却 ※レスポンス無しorレスポンス200以外
                        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
                            return null;
                        }
                        // 異常検知の場合null返却 ※レスポンスボディなし
                        var resBody = response.getBody();
                        if (resBody == null || resBody.isEmpty()) {
                            return null;
                        }
                        // 異常検知の場合null返却 ※リザルトコードが正常でない
                        PersonalInfoListResponseDto dto = mapper.readValue(resBody, PersonalInfoListResponseDto.class);
                        if (!dto.getResultCode().equals(GETPERSONAL_SUCCESS)) {
                            return null;
                        }
                        // 異常検知の場合はnull返却 ※個人情報リストが空
                        if (dto.getPersonalInfoList() == null || dto.getPersonalInfoList().isEmpty()) {
                            return null;
                        }
                        // 正常応答の場合DTOを返却
                        return dto;

                    } finally {
                        // 処理枠を開放
                        semaphore.release();
                    }
                }));
            }

            // 結果取得（リザルトコードが正常であればPersonalInfoListをマージしていく）
            PersonalInfoListResponseDto mgDto = new PersonalInfoListResponseDto();
            mgDto.setPersonalInfoList(new ArrayList<>());
            for (int i = 0; i < notificationSendListChunkList.size(); i++) {

                // タスク完了分の結果（実行結果DTO）取得
                Future<PersonalInfoListResponseDto> done = completion.take();
                try {
                    PersonalInfoListResponseDto dto = done.get();
                    // null返却されてきた場合は残リスト分のタスクをキャンセルして個人情報取得処理を終了。
                    if (dto == null) {
                        for (Future<PersonalInfoListResponseDto> f : futures) {
                            f.cancel(true);
                        }
                        return null;
                    }
                    // 正常に個人情報取得できた場合、DTOの個人情報リストをマージ
                    mgDto.setResultCode(dto.getResultCode());
                    mgDto.getPersonalInfoList().addAll(dto.getPersonalInfoList());

                } catch (Exception e) {
                    // completion.takeで検知された例外発生時、残タスクキャンセルして個人情報取得処理を終了。
                    Thread.currentThread().interrupt();
                    for (Future<PersonalInfoListResponseDto> f : futures) {
                        f.cancel(true);
                    }
                    return null;
                }
            }

            // すべてのタスクが正常に完了した場合、マージ済みDTOを返却
            return mgDto;

        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new CustomException(e);
        }
    }

    /**
     * NotificationSendListを100件単位で分割します。
     * 
     * @param notificationSendList
     * @return 100件単位のリストオブジェクト
     */
    private List<List<NotificationSendListDto>> getNotificationChunkList(
            List<NotificationSendListDto> notificationSendList) {
        List<List<NotificationSendListDto>> chunkList = new ArrayList<>();

        for (int i = 0; i < notificationSendList.size(); i += CHUNK_SIZE) {
            chunkList.add(notificationSendList.subList(i, Math.min(i + CHUNK_SIZE, notificationSendList.size())));
        }
        return chunkList;
    }
    // endregion

    // #region 3-5:通知処理実行
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
     * @param userInfo
     * @param personalInfoListResDto
     * @param notificationSendList
     */

    private boolean executeNotificationProcess(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            NotificationVinListEntity userInfo,
            PersonalInfoListResponseDto personalInfoListResDto,
            List<NotificationSendListDto> notificationSendList) {

        boolean errorFlag = false;
        // UserList（個人情報リスト）でループ
        List<PersonalInfoList> personalInfoList = personalInfoListResDto.getPersonalInfoList();
        for (PersonalInfoList personalInfo : personalInfoList) {
            try {

                // 1:Notification登録判定&実行
                boolean registError = executeRegisterNotification(request, header, personalInfo);
                if (registError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(request, header, userInfo, personalInfo.getInternalUserId(),
                            ERR_STATUS_ERR, ERR_STATUS_SKP, ERR_STATUS_SKP);
                    errorFlag = true;
                    continue;
                }
                LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00022",
                        personalInfo.getInternalUserId(), userInfo.getNotificationId(), header.getCorrelationId()));

                // 2:PUSH実行判定&実行
                boolean pushError = executePushNotification(
                        request, header, personalInfo, userInfo, notificationSendList);
                if (pushError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(request, header, userInfo, personalInfo.getInternalUserId(),
                            ERR_STATUS_SUCCESS, ERR_STATUS_ERR, ERR_STATUS_SKP);
                    errorFlag = true;
                    continue;
                }

                // 3:PrimaryContact送信判定&実行
                boolean primaryContactError = executePrimaryContact(
                        request, header, personalInfo, notificationSendList);
                if (primaryContactError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(request, header, userInfo, personalInfo.getInternalUserId(),
                            ERR_STATUS_SUCCESS, ERR_STATUS_SUCCESS, ERR_STATUS_ERR);
                    errorFlag = true;
                    continue;
                }
                LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00012",
                        personalInfo.getInternalUserId(), header.getCorrelationId()));

            } catch (Exception e) {
                // X:システム例外時は例外スローして処理ループを抜ける
                executeErrorProcess(request, header, userInfo, personalInfo.getInternalUserId(),
                        ERR_STATUS_ERR, ERR_STATUS_ERR, ERR_STATUS_ERR);
                throw new CustomException(e);
            }
        }

        return errorFlag;
    }
    // endregion

    // #region 3-4-1:Notification登録判定&実行
    /**
     * Notification登録判定&実行
     * リクエストボディ.通知区分=1,3（NTF / NTF + MAIL or SMS）の場合、Notification登録を実行します。
     * 
     * @param request          リクエストDTO
     * @param header           ヘッダーDTO
     * @param personalInfoList 個人情報リストレスポンスDTO
     * @return エラーフラグ（True:エラー発生, False:正常）
     */
    private boolean executeRegisterNotification(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            PersonalInfoList personalInfo) {

        if (Set.of(TYPE_NTF, TYPE_NTF_AND_MAILSMS).contains(request.getNotificationType())) {
            // Notification登録実行
            RegisterNotificationResponseDto responseDto = registerNotification(
                    request, personalInfo);
            if (responseDto == null) {
                LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00020",
                        personalInfo.getInternalUserId(), "Response DTO is null", header.getCorrelationId()));
                return true;
            }
            if (!responseDto.getReturnCode().equals(REGISTNOTIFICATION_SUCCESS)) {
                LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00020",
                        personalInfo.getInternalUserId(), responseDto.getMessage(), header.getCorrelationId()));
                return true;
            }
        }
        return false;
    }

    /**
     * Notification登録実行部品
     * 
     * @param request リクエストDTO
     * @param header  ヘッダーDTO
     * @param user    ループ中のユーザー情報
     */
    private RegisterNotificationResponseDto registerNotification(
            SendMessageNotificationRequestDto request,
            PersonalInfoList personalInfo) {

        // Notification登録処理実行
        ResponseEntity<String> response = batApisUtil.executeRegisterNotification(
                createRegisterNotificationRequestDto(request, personalInfo));
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
            PersonalInfoList personalInfo) {
        return new RegisterNotificationRequestDto(
                request.getRegistrationSerialNumber().toString(),
                CNT_ME,
                personalInfo.getInternalUserId(),
                request.getNotificationContents(),
                request.getNotificationType());
    }
    // #endregion

    // #region 3-4-2:PUSH実行判定&実行
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
            PersonalInfoList personalInfo,
            NotificationVinListEntity userInfo,
            List<NotificationSendListDto> notificationSendList) {

        if (request.getIsPushNotificationRequired().equals("1")) {
            // 登録済みデバイス取得実行
            NtfInfoEntity deviceData = getLatestDeviceData(personalInfo.getInternalUserId());
            if (deviceData == null) {
                LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00013",
                        personalInfo.getInternalUserId(), header.getCorrelationId()));
                return true;
            }
            // ペイロード編集
            String payload = createPayload(request, header, userInfo, deviceData);
            // ペイロードに内部ライセンスコード組み込み
            String lisence = notificationSendList.stream()
                    .filter(n -> n.getInternalUserId().equals(personalInfo.getInternalUserId()))
                    .findFirst().map(NotificationSendListDto::getLicenseCode).orElse("");
            payload = notificationHubUtil.replaceLcsSelected(payload, deviceData.getPlatformType(), lisence);

            // 通知送信実行
            NotificationOutcome outcome = executePostMessage(header, personalInfo, deviceData, payload);
            if (outcome == null) {
                LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00005",
                        0, personalInfo.getInternalUserId(),
                        payload, deviceData.getInstallationId(), header.getCorrelationId()));
                return true;
            }
            LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00006",
                    personalInfo.getInternalUserId(), request.getPayload(),
                    deviceData.getPlatformType(), header.getCorrelationId()));
        }
        return false;
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
            LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00014",
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
    private NotificationOutcome executePostMessage(
            RequestHeaderDto header,
            PersonalInfoList personalInfo,
            NtfInfoEntity deviceData,
            String payload) {

        LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00005",
                personalInfo.getInternalUserId(), payload, deviceData.getPlatformType(), header.getCorrelationId()));
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
                        LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00005",
                                ex.httpStatusCode(), personalInfo.getInternalUserId(),
                                payload, deviceData.getInstallationId(), header.getCorrelationId()));
                        return null;
                    }
                    LogUtil.warn(getClass(), CommonUtil.getBatMessage("RS07W00003",
                            cnt, ex.httpStatusCode(), personalInfo.getInternalUserId(),
                            payload, deviceData.getInstallationId(), header.getCorrelationId(),
                            String.valueOf(cnt)));
                    continue;
                }
                LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00005",
                        ex.httpStatusCode(), personalInfo.getInternalUserId(),
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

    // #region 3-4-3:PrimaryContact送信判定&実行
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
            PersonalInfoList personalInfo,
            List<NotificationSendListDto> notificationSendList) {

        // ブランド取得
        String brdCd = notificationSendList.stream()
                .filter(n -> n.getInternalUserId().equals(personalInfo.getInternalUserId()))
                .findFirst()
                .map(NotificationSendListDto::getBrdCd)
                .orElse("");

        if (request.getNotificationType().equals(TYPE_MAILSMS) ||
                request.getNotificationType().equals(TYPE_NTF_AND_MAILSMS)) {
            // PrimaryContact送信処理実行
            return sendRequest(request, header, personalInfo, brdCd);
        }
        return false;
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
            PersonalInfoList personalInfo,
            String brdCd) {

        boolean errorFlag = false;
        for (var contact : personalInfo.getContactList()) {
            if (!contact.isPrimaryContactFlag()) {
                continue; // primaryContactFlagがfalseの場合はスキップ
            }
            if (contact.getContactType().equals(CONTACT_PHONE)) {
                // SMS送信要求
                LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00013",
                        personalInfo.getInternalUserId(), brdCd, contact.getContact(), request.getTitle(),
                        header.getCorrelationId()));
                errorFlag |= executeSendSms(request, header, contact.getContact(), brdCd);

            } else if (contact.getContactType().equals(CONTACT_EMAIL)) {
                // メール送信要求
                LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00014",
                        personalInfo.getInternalUserId(), brdCd, contact.getContact(), request.getTitle(),
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
    private boolean executeSendSms(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            String phoneNo,
            String brdCd) {

        ResponseEntity<String> smsResponse = smsCountryUtil.executeSendSms(
                phoneNo, request.getBodySms(), brdCd);
        if (!smsResponse.getStatusCode().is2xxSuccessful()) {
            LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00006",
                    smsResponse.getStatusCode().value(), CommonUtil.maskPhoneNumber(phoneNo),
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
    private boolean executeSendEmail(
            SendMessageNotificationRequestDto request,
            RequestHeaderDto header,
            String email,
            String brdCd) {

        Mail mail = sendGridUtil.generateEmail(
                email, request.getTitle(), request.getBodyText(), request.getBodyHtml(), brdCd);
        Response response = sendGridUtil.executeSendEmail(mail);
        if (!HttpStatusCode.valueOf(response.getStatusCode()).is2xxSuccessful()) {
            LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00007",
                    response.getStatusCode(), CommonUtil.maskText(email), header.getCorrelationId()));
            return true;
        }
        return false;
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

        ntfBatchExecErrorInfoRepository.insert(
                new NtfBatchExecErrorInfoEntity(
                        request.getRegistrationSerialNumber(),
                        Integer.valueOf(userInfo.getSequenceNumber()),
                        internalUserId,
                        notificationStatus,
                        pushStatus,
                        primaryContactStatus,
                        null, null, null));
        LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07D00008",
                request.getRegistrationSerialNumber(), userInfo.getSequenceNumber(),
                notificationStatus, pushStatus, primaryContactStatus, header.getCorrelationId()));
    }
    // #endregion

}