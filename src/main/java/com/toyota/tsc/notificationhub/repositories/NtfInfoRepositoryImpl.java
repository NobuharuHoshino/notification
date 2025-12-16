
package com.toyota.tsc.notificationhub.repositories;

import java.sql.SQLException;
import java.sql.SQLTransientException;
import java.util.List;
import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;
import com.toyota.tsc.notificationhub.commons.PropertiesUtil;
import com.toyota.tsc.notificationhub.exceptions.CustomException;

/**
 * 通知情報リポジトリ実装クラス
 */
@Repository
@Profile("!local")
public class NtfInfoRepositoryImpl implements NtfInfoRepositoryIF {
    private final NtfInfoMapper ntfInfoMapper;
    private final PropertiesUtil propertiesUtil;

    @Autowired
    public NtfInfoRepositoryImpl(NtfInfoMapper ntfInfoMapper, PropertiesUtil propertiesUtil) {
        this.ntfInfoMapper = ntfInfoMapper;
        this.propertiesUtil = propertiesUtil;
    }

    @Override
    /**
     * 通知情報を新規登録します。
     * 
     * @param entity 通知情報エンティティ
     * @return 登録件数
     */
    public int insert(NtfInfoEntity entity) {
        return ntfInfoMapper.insert(entity);
    }

    @Override
    /**
     * 通知情報を更新します。
     * 
     * @param entity 通知情報エンティティ
     * @return 更新件数
     */
    public int update(NtfInfoEntity entity) {
        return ntfInfoMapper.update(entity);
    }

    @Override
    /**
     * ユーザーID・インストールIDで通知情報を取得します。
     * 
     * @param internalUserId ユーザーID
     * @param installationId インストールID
     * @return 通知情報エンティティ
     */
    public NtfInfoEntity select(String internalUserId, String installationId) {
        return ntfInfoMapper.select(internalUserId, installationId);
    }

    @Override
    /**
     * ユーザーID・インストールIDで通知情報を削除します。
     * 
     * @param internalUserId ユーザーID
     * @param installationId インストールID
     * @return 削除件数
     */
    public int delete(String internalUserId, String installationId) {
        return executeWithRetry(() -> ntfInfoMapper.delete(internalUserId, installationId),
                propertiesUtil.getNtfinfoUpsertRetryCount());
    }

    @Override
    /**
     * 通知情報をUpsert（登録または更新）します。
     * 
     * @param entity 通知情報エンティティ
     * @return Upsert件数
     */
    public int upsert(NtfInfoEntity entity) {
        return executeWithRetry(() -> ntfInfoMapper.upsert(entity), propertiesUtil.getNtfinfoUpsertRetryCount());
    }

    @Override
    /**
     * ユーザーIDで全通知情報を取得します。
     * 
     * @param internalUserId ユーザーID
     * @return 通知情報エンティティリスト
     */
    public List<NtfInfoEntity> selectAllByInternalUserId(String internalUserId) {
        try {
            return ntfInfoMapper.selectAllByInternalUserId(internalUserId);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * SQLTransientException発生時にリトライ処理を行います。
     * 
     * @param action     実行アクション
     * @param retryCount リトライ回数
     * @return アクション実行結果
     */
    private <T> T executeWithRetry(Callable<T> action, int retryCount) {
        for (int retry = 0;; retry++) {
            try {
                return action.call();
            } catch (Exception e) {
                // ここを「投げるか（throw）／待つか（sleep）」の1行判定にする
                if (shouldThrow(e, retry, retryCount)) {
                    throw toCustom(e);
                }
                sleep(backoffMillis(retry + 1)); // 1回目の待機は指数0
            }
        }
    }

    /** 例外をどう扱うかの“ロジック”をここに集約（本体の複雑度を下げる） */
    private boolean shouldThrow(Exception e, int retry, int retryCount) {
        final SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);

        // SQL例外でない → リトライ不可
        if (sqlEx == null)
            return true;

        // 非Transient → リトライ不可
        if (!(sqlEx instanceof SQLTransientException))
            return true;

        // Transientだが回数超過 → リトライ不可
        return retry >= retryCount;
    }

    /** 例外ラップも1箇所に集約（本体の分岐を削減） */
    private CustomException toCustom(Exception e) {
        final SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
        return new CustomException(sqlEx != null ? sqlEx : e);
    }

    private long backoffMillis(int attempt) {
        long base = (long) (propertiesUtil.getNtfinfoUpsertRetryBaseInterval() * Math.pow(2, (double) attempt - 1));
        return Math.min(base, propertiesUtil.getNtfinfoUpsertRetryMaxInterval());
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}