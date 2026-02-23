package com.toyota.tsc.notificationhub.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.toyota.tsc.notificationhub.commons.CommonUtil;
import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.JsapUtil;
import com.toyota.tsc.notificationhub.commons.LogUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;
import com.toyota.tsc.notificationhub.exceptions.CustomSqlException;
import com.toyota.tsc.notificationhub.exceptions.TscApplicationException;
import com.toyota.tsc.notificationhub.models.DvcLinkageResponseDto;
import com.toyota.tsc.notificationhub.models.GetAccessTokenResponseDto;
import com.toyota.tsc.notificationhub.models.GetUserIdResponseDto;
import com.toyota.tsc.notificationhub.models.RegistNotificationDeviceInfoRequestDto;
import com.toyota.tsc.notificationhub.models.RequestHeaderDto;
import com.toyota.tsc.notificationhub.models.ResponseDto;
import com.toyota.tsc.notificationhub.repositories.SaNtfInfoEntity;
import com.toyota.tsc.notificationhub.repositories.SaNtfInfoRepositoryIF;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 端末情報登録サービス実装クラス
 */
@Profile("sa")
@Service
public class SaRegistNotificationDeviceInfoServiceImpl implements RegistNotificationDeviceInfoServiceIF {

    private SaNtfInfoRepositoryIF saNtfInfoRepository;
    private JsapUtil jsapUtil;

    public SaRegistNotificationDeviceInfoServiceImpl(
            SaNtfInfoRepositoryIF saNtfInfoRepository,
            JsapUtil jsapUtil) {
        this.saNtfInfoRepository = saNtfInfoRepository;
        this.jsapUtil = jsapUtil;
    }

    private static final String PROCCESS_NAME = "デバイス情報登録";

    private static final String RESULT_SUCCESS = "SA_RND_SUCCESS";
    private static final String RESULT_FIELD_MISSING = "SA_RND_FIELD_MISSING";
    private static final String RESULT_INVALID_BRAND = "SA_RND_INVALID_BRAND";
    private static final String RESULT_INVALID_PLATFORM = "SA_RND_INVALID_PLATFORM";
    private static final String RESULT_GET_USERID_ERROR = "SA_RND_GET_USERID_ERROR";
    private static final String RESULT_DVCLINKAGE_ERROR = "SA_RND_DVCLINKAGE_ERROR";
    private static final String RESULT_EXCEPTION = "SA_RND_EXCEPTION";
    private static final String RESULT_TOKENFOUND_ERROR = "SA_RND_TOKENFOUND_ERROR";

    private static final String RES_GETUSERID_SUCCESS = "00001548B123";
    private static final String RES_DVCLINKAGE_SUCCESS = "000000";

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
            LogUtil.info(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00001", PROCCESS_NAME, header.getCorrelationId(), CommonUtil.toJson(request)));

            // リクエスト検証
            validate(request, header);

            // Installation実行
            execRegistNotificationInfo(request, header);

            String resultCode = CommonUtil.getResultCode(RESULT_SUCCESS);

            // 正常終了ログ
            LogUtil.info(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00002", PROCCESS_NAME, resultCode, header.getCorrelationId()));

            return new ResponseDto(resultCode);

        } catch (TscApplicationException e) {
            throw new TscApplicationException(e.getResultCode());

        } catch (Exception e) {
            SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
            if (sqlEx != null) {
                if (ExtractSqlExceptionUtil.isSqlConnectionError(sqlEx)) {
                    // 接続エラー
                    LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                            "RS07E00010", sqlEx.getMessage(), sqlEx.getStackTrace(), header.getCorrelationId()));
                    throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
                } else if (ExtractSqlExceptionUtil.isSqlOperationError(sqlEx)) {
                    // 操作エラー
                    LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                            "RS07E00011", sqlEx.getMessage(), sqlEx.getStackTrace(), sqlEx.getSQLState(),
                            sqlEx.getErrorCode(), header.getCorrelationId()));
                    throw new CustomSqlException(CommonUtil.getResultCode(RESULT_EXCEPTION));
                }
                LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
            } else {
                // その他予期せぬエラー
                LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00001", e.getMessage(), e.getStackTrace(), header.getCorrelationId()));
                throw new CustomException(CommonUtil.getResultCode(RESULT_EXCEPTION));
            }
        }

    }

    /**
     * 通知端末情報登録処理を実行します。
     * 
     * @param request
     * @param header
     */
    private void execRegistNotificationInfo(RegistNotificationDeviceInfoRequestDto request, RequestHeaderDto header) {
        try {
            // JSAPトークン取得
            ResponseEntity<String> getToken = jsapUtil.executeGetToken();
            ObjectMapper mapper = new ObjectMapper();
            GetAccessTokenResponseDto tokenDto = mapper.readValue(getToken.getBody(),
                    GetAccessTokenResponseDto.class);
            String token = tokenDto.getAccess_token();
            if (token == null || token.isEmpty()) {
                LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00013", tokenDto.getAccess_token(), request.getInternalUserId(),
                        header.getCorrelationId()));
                throw new TscApplicationException(CommonUtil.getResultCode(RESULT_TOKENFOUND_ERROR));
            }

            // DB登録or更新
            int upsertCount = upsertDeviceInfo(request);
            if (upsertCount == 0) {
                throw new CustomException();
            }
            LogUtil.info(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07D00001", request.getInternalUserId(), request.getDvcId(),
                    CommonUtil.getBrd(request.getBrdCd()),
                    CommonUtil.getPlt(request.getPlatform()), header.getCorrelationId()));

            // 認証規約 UserID取得
            ResponseEntity<String> getUserIdResponce = jsapUtil.executeGetUserId(
                    request.getInternalUserId(), header.getCorrelationId());
            GetUserIdResponseDto getUserIdDto = mapper.readValue(getUserIdResponce.getBody(),
                    GetUserIdResponseDto.class);
            if (!getUserIdDto.getResultCode().equals(RES_GETUSERID_SUCCESS)) {
                LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00014", getUserIdDto.getResultCode(), request.getInternalUserId(),
                        header.getCorrelationId()));
                throw new TscApplicationException(CommonUtil.getResultCode(RESULT_GET_USERID_ERROR));
            }
            LogUtil.info(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00009", request.getInternalUserId(), header.getCorrelationId()));

            // JSAP デバイス登録
            LogUtil.info(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00003", request.getInternalUserId(), request.getDvcId(),
                    CommonUtil.getPlt(request.getPlatform()),
                    header.getCorrelationId()));
            ResponseEntity<String> dvcLinkResponce = jsapUtil.executeDvcLink(
                    getUserIdDto.getUserId(), request.getDeviceToken(), request.getPlatform(), token);
            DvcLinkageResponseDto dvcLinkDto = mapper.readValue(dvcLinkResponce.getBody(), DvcLinkageResponseDto.class);
            if (!dvcLinkDto.getResultCode().equals(RES_DVCLINKAGE_SUCCESS)) {
                LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                        "RS07E00006", dvcLinkDto.getResultCode(), request.getInternalUserId(),
                        dvcLinkResponce.getBody(), request.getInternalUserId(), header.getCorrelationId()));
                throw new TscApplicationException(CommonUtil.getResultCode(RESULT_DVCLINKAGE_ERROR));
            }
            LogUtil.info(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07I00004", dvcLinkDto.getResultCode(), request.getInternalUserId(), request.getDvcId(),
                    CommonUtil.getPlt(request.getPlatform()), header.getCorrelationId()));

        } catch (JsonProcessingException e) {
            throw new CustomException(e);
        }
    }

    /**
     * 端末情報をUpsert（登録または更新）します。
     * 
     * @param request        リクエストDTO
     * @param header         ヘッダーDTO
     * @param installationId Installation ID
     * @return Upsert件数
     */
    private int upsertDeviceInfo(RegistNotificationDeviceInfoRequestDto request) {
        SaNtfInfoEntity entity = new SaNtfInfoEntity(
                request.getInternalUserId(),
                request.getDvcId(),
                request.getBrdCd(),
                request.getPlatform(),
                LocalDateTime.now(),
                LocalDateTime.now());
        return saNtfInfoRepository.upsert(entity);
    }

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
            LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07E00003", missingField, header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_FIELD_MISSING));
        }
        if (!isValidBrdCd(request.getBrdCd())) {
            LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07E00004", request.getBrdCd(), header.getCorrelationId()));
            throw new TscApplicationException(CommonUtil.getResultCode(RESULT_INVALID_BRAND));
        }
        if (!isValidPlatform(request.getPlatform())) {
            LogUtil.error(SaRegistNotificationDeviceInfoServiceImpl.class, CommonUtil.getSaMessage(
                    "RS07E00005", request.getPlatform(), header.getCorrelationId()));
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
}
