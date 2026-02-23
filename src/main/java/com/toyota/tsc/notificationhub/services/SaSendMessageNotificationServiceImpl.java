
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
import com.toyota.tsc.notificationhub.models.GetAccessTokenResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserIdResponseDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.models.SaNotificationSendListDto;
import com.toyota.tsc.notificationhub.models.SendMessageNotificationRequestDto;
import com.toyota.tsc.notificationhub.models.SendMessageResponseDto;
import com.toyota.tsc.notificationhub.models.PersonalInfoListResponseDto.PersonalInfoList;
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
import java.util.concurrent.Semaphore;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    // オンプレ個人情報リスト取得API 正常終了コード
    private static final String GETPERSONAL_SUCCESS = "00001581U000";
    // 内部UserID変換 APIレスポンス 正常終了コード
    private static final String GETUSERID_SUCCESS = "00001548B123";
    // Jsap 正常終了コード
    private static final String JSAP_SUCCESS = "000000";
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
    // #endregion

    @Override
    @Transactional(rollbackFor = Exception.class)
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
            this.notificationRepository.update(CNT_ME, request.getRegistrationSerialNumber());

            // 終了ログ
            LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00002",
                    PROCESS_NAME, CommonUtil.getResultCode(RESULT_SUCCESS), header.getCorrelationId()));

            return new ResponseDto(CommonUtil.getResultCode(RESULT_SUCCESS));

        } catch (TscApplicationException e) {
            throw new TscApplicationException(e.getResultCode());

        } catch (Exception e) {
            SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
            if (sqlEx != null) {
                if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                    // 接続エラー
                    LogUtil.error(getClass(), CommonUtil.getBatMessage(
                            "RS07E00010", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                    throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
                } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                    // 操作エラー
                    LogUtil.error(getClass(), CommonUtil.getBatMessage(
                            "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(), sqlEx.getSQLState(),
                            sqlEx.getErrorCode(), header.getCorrelationId()));
                    throw new CustomSqlException(CommonUtil.getResultCode(RESULT_EXCEPTION));
                }
                LogUtil.error(getClass(), CommonUtil.getBatMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
            } else {
                // その他予期せぬエラー
                LogUtil.error(getClass(), CommonUtil.getBatMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
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

        List<CompletableFuture<Void>> f = new ArrayList<>();
        userInfoList.forEach(entity -> {

            try {

                // 3-1:お知らせ通知処理履歴登録（更新0の場合は異常終了）
                int insertCnt = this.insertHistory(request, header, entity);
                if (insertCnt == 0) {
                    throw new CustomException();
                }

                // 3-2:通知基盤連携情報をListオブジェクトで取得(Max1000件)
                List<SaNotificationSendListDto> notificationSendList = convertToNotificationSendListDto(
                        entity.getNotificationSendList());

                // 3-3:内部UserIDをUserIDに変換
                List<SaNotificationSendListDto> saNotificationSendList = convertInternalUserIdToUserId(
                        header, notificationSendList);
                if (saNotificationSendList == null) {
                    return;
                }

                // 3-4:NotificationVinList連携フラグ更新（更新0の場合は異常終了）
                int updCnt = this.updateNotificationVinList(request, entity, LINKTYPE_NOTLINKED);
                if (updCnt == 0) {
                    throw new CustomException();
                }

                // 3-5:トークン取得
                String token = this.getToken();
                if (token == null || token.isEmpty()) {
                    LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00017",
                            request.getRegistrationSerialNumber(), entity.getSequenceNumber(),
                            header.getCorrelationId()));
                    return;
                }

                // 3-6:連絡先リスト取得
                PersonalInfoListResponseDto personalInfoListResDto = this.getPersonalInfoList(
                        header, saNotificationSendList);
                if (personalInfoListResDto == null) {
                    LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00015",
                            request.getRegistrationSerialNumber(), entity.getSequenceNumber(),
                            header.getCorrelationId()));
                    executeErrorProcess(request, header, entity,
                            null, ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                    return;
                }
                // 3-7:非同期で通知処理実行
                f.add(CompletableFuture.runAsync(() -> {
                    boolean errorFlag = this.executeNotificationProcess(
                            request, header, entity, personalInfoListResDto, saNotificationSendList);
                    // お知らせ通知処理履歴更新
                    if (errorFlag) {
                        ntfBatchExecHistoryRepository.updateStatus(
                                request.getRegistrationSerialNumber(),
                                Integer.valueOf(entity.getSequenceNumber()),
                                STATUS_ERR);
                    } else {
                        ntfBatchExecHistoryRepository.updateStatus(
                                request.getRegistrationSerialNumber(),
                                Integer.valueOf(entity.getSequenceNumber()),
                                STATUS_DONE);
                    }
                    // NotificationVinList連携フラグ更新（更新0の場合は異常終了）
                    this.updateNotificationVinList(request, entity, LINKTYPE_LINKED);
                }));

            } catch (Exception e) {
                // X:システム例外時は例外スローしてループ中断
                throw new CustomException(e);
            }
        });

        // 全タスク完了待機
        CompletableFuture.allOf(f.toArray(new CompletableFuture[0])).join();
    }
    // endregion

    // #region 3-1:お知らせ通知処理履歴登録
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

    // #region 3-2:通知基盤連携情報をListオブジェクトで取得(Max1000件)
    /**
     * notification_vin_listテーブルから取得した通知基盤連携情報をList<DTO>にマッピングします。
     * 通知基盤連携情報はレコードカンマ区切り&フィールドコロン区切りのテキストデータです。
     * ※[vin]:[internalUserIId]:[lisenceCode]:[brdCd]
     * 
     * @param csvText notification_vin_list.notification_send_list
     * @return マッピングされたSaNotificationSendListDtoのリスト
     */
    private List<SaNotificationSendListDto> convertToNotificationSendListDto(String csvText) {
        return Arrays.stream(csvText.split(","))
                .map(r -> r.split(":"))
                .map(p -> new SaNotificationSendListDto(p[0], p[1], p[2], p[3], null))
                .toList();
    }
    // #endregion

    // #region 3-3:内部UserIDをUserIDに変換
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
                ObjectMapper mapper = new ObjectMapper();
                GetUserIdResponseDto getUserIdDto = mapper.readValue(
                        response.getBody(), GetUserIdResponseDto.class);

                // エラー時ループを抜ける
                if (!getUserIdDto.getResultCode().equals(GETUSERID_SUCCESS)) {
                    LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00016",
                            getUserIdDto.getResultCode(), dto.getInternalUserId(), header.getCorrelationId()));
                    errorFlag = true;
                    break;
                }

                // 正常時UserIDをセット
                dto.setUserId(getUserIdDto.getUserId());
            }
            if (errorFlag) {
                return null;
            }

            return notificationSendList;
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }
    // #endregion

    // #region 3-4:連携フラグ更新
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

    // #region 3-5:トークン取得
    /**
     * トークン取得
     * 
     * @param header
     * @param notificationSendList
     * @return トークン
     */
    private String getToken() {
        try {
            ResponseEntity<String> getToken = jsapUtil.executeGetToken();
            ObjectMapper mapper = new ObjectMapper();
            GetAccessTokenResponseDto tokenDto = mapper.readValue(getToken.getBody(),
                    GetAccessTokenResponseDto.class);
            if (tokenDto == null || !getToken.getStatusCode().is2xxSuccessful()) {
                return null;
            }
            return tokenDto.getAccess_token();
        } catch (Exception e) {
            throw new CustomException(e);
        }
    }
    // endregion

    // #region 3-6:連絡先リスト取得
    /**
     * ALJから連絡先リストを取得します。
     * 
     * @param request  リクエストDTO
     * @param header   ヘッダーDTO
     * @param userInfo ユーザー情報
     */
    private PersonalInfoListResponseDto getPersonalInfoList(
            RequestHeaderDto header,
            List<SaNotificationSendListDto> notificationSendList) {

        final ObjectMapper mapper = new ObjectMapper();
        final int pal = properties.getParallelCurrent();
        final Semaphore semaphore = new Semaphore(pal);

        // 多重度=palで並列処理実行
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            final CompletionService<PersonalInfoListResponseDto> completion = new ExecutorCompletionService<>(executor);
            final List<Future<PersonalInfoListResponseDto>> futures = new ArrayList<>(
                    notificationSendList.size());

            // 並列処理タスク投入（100件単位の内部UserIDリスト数に応じたAPI呼び出し）
            for (SaNotificationSendListDto notificationSendListDto : notificationSendList) {

                futures.add(completion.submit(() -> {

                    // 処理枠（=多重度）を確保
                    semaphore.acquire();
                    try {

                        // API実行
                        ResponseEntity<String> response = jsapUtil.executeGetUserInfo(
                                notificationSendListDto.getUserId());

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
            for (int i = 0; i < notificationSendList.size(); i++) {

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
            List<SaNotificationSendListDto> notificationSendList) {

        boolean errorFlag = false;
        try {
            // UserList（個人情報リスト）でループ
            List<PersonalInfoList> personalInfoList = personalInfoListResDto.getPersonalInfoList();
            for (PersonalInfoList personalInfo : personalInfoList) {

                // 0:トークン取得（期限切れを避けるため）
                String token = this.getToken();
                if (token == null || token.isEmpty()) {
                    LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00017",
                            request.getRegistrationSerialNumber(), userInfo.getSequenceNumber(),
                            header.getCorrelationId()));
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(request, header, userInfo, personalInfo.getInternalUserId(),
                            ERR_STATUS_SKP, ERR_STATUS_SKP, ERR_STATUS_SKP);
                    errorFlag = true;
                    continue;
                }

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
                        request, header, personalInfo, userInfo, notificationSendList, token);
                if (pushError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(request, header, userInfo, personalInfo.getInternalUserId(),
                            ERR_STATUS_SUCCESS, ERR_STATUS_ERR, ERR_STATUS_SKP);
                    errorFlag = true;
                    continue;
                }

                // 3:PrimaryContact送信判定&実行
                boolean primaryContactError = executePrimaryContact(
                        request, header, personalInfo, token);
                if (primaryContactError) {
                    // エラー処理（エラー登録＆ロギング＆次ループ）
                    executeErrorProcess(request, header, userInfo, personalInfo.getInternalUserId(),
                            ERR_STATUS_SUCCESS, ERR_STATUS_SUCCESS, ERR_STATUS_ERR);
                    errorFlag = true;
                    continue;
                }
                LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00012",
                        personalInfo.getInternalUserId(), header.getCorrelationId()));
            }

        } catch (Exception e) {
            // X:システム例外時は例外スローしてループ中断
            throw new CustomException(e);
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
                throw new CustomException();
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
            List<SaNotificationSendListDto> notificationSendList,
            String token) {

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
                    .findFirst().map(SaNotificationSendListDto::getLicenseCode).orElse("");
            payload = notificationHubUtil.replaceLcsSelected(payload, deviceData.getPlatformType(), lisence);

            // 通知送信実行
            PushRequestResponseDto obj = executePostMessage(header, personalInfo, deviceData, payload, token);
            if (!obj.getResultCode().equals(JSAP_SUCCESS)) {
                LogUtil.error(getClass(), CommonUtil.getBatMessage("RS07E00005",
                        obj.getResultCode(), personalInfo.getInternalUserId(),
                        payload, header.getCorrelationId()));
                return false;
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
    private PushRequestResponseDto executePostMessage(
            RequestHeaderDto header,
            PersonalInfoList personalInfo,
            NtfInfoEntity deviceData,
            String payload,
            String token) {

        try {
            LogUtil.info(getClass(), CommonUtil.getBatMessage("RS07I00005",
                    personalInfo.getInternalUserId(), payload, deviceData.getPlatformType(),
                    header.getCorrelationId()));

            ResponseEntity<String> jsapNotificationResponce = jsapUtil.executePushRequest(
                    personalInfo.getUserId(), payload, token);

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
            String token) {

        if (request.getNotificationType().equals(TYPE_MAILSMS) ||
                request.getNotificationType().equals(TYPE_NTF_AND_MAILSMS)) {
            // PrimaryContact送信処理実行
            return sendRequest(request, header, personalInfo, token);
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
            String token) {

        boolean errorFlag = false;
        for (var contact : personalInfo.getContactList()) {
            if (!contact.isPrimaryContactFlag()) {
                continue; // primaryContactFlagがfalseの場合はスキップ
            }
            Object context;
            if (contact.getContactType().equals(CONTACT_PHONE)) {
                context = jsapUtil.createSmsContext(request.getBodyText());

            } else if (contact.getContactType().equals(CONTACT_EMAIL)) {
                context = jsapUtil.createMailContext(request.getBodyText(), request.getBodyHtml());
            } else {
                throw new CustomException();
            }

            errorFlag = executeSendMessage(request, header, contact, personalInfo.getUserId(), true, context, token);
        }
        return errorFlag;
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
    private boolean executeSendMessage(SendMessageNotificationRequestDto request, RequestHeaderDto header,
            PersonalInfoListResponseDto.ContactDto contact, String userId, boolean hasUserId, Object context,
            String token) {

        try {
            ResponseEntity<String> sendMessageResponse = jsapUtil.executeSendMessage(
                    null, userId, contact.getContactType(),
                    request.getTitle(), context, token);

            ObjectMapper mapper = new ObjectMapper();
            SendMessageResponseDto sendMessageDto = mapper.readValue(sendMessageResponse.getBody(),
                    SendMessageResponseDto.class);
            if (!sendMessageDto.getResultCode().equals(JSAP_SUCCESS)) {
                return true;
            }
            return false;

        } catch (Exception e) {
            throw new CustomException(e);
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