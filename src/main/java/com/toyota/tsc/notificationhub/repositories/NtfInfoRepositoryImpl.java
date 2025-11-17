
package com.toyota.tsc.notificationhub.repositories;

import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.util.List;
import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import com.toyota.tsc.notificationhub.commons.ExtractSqlExceptionUtil;

/**
 * 通知情報リポジトリ実装クラス
 */
@Repository
@Primary
public class NtfInfoRepositoryImpl implements NtfInfoRepositoryIF {
    private final NtfInfoMapper ntfInfoMapper;

    @Value("${ntfinfo.upsert.retry.count:3}")
    private int upsertRetryCount;
    @Value("${ntfinfo.upsert.retry.base-interval:100}")
    private long upsertRetryBaseInterval;
    @Value("${ntfinfo.upsert.retry.max-interval:20000}")
    private long upsertRetryMaxInterval;

    @Autowired
    public NtfInfoRepositoryImpl(NtfInfoMapper ntfInfoMapper) {
        this.ntfInfoMapper = ntfInfoMapper;
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
        return executeWithRetry(() -> ntfInfoMapper.delete(internalUserId, installationId), upsertRetryCount);
    }

    @Override
    /**
     * 通知情報をUpsert（登録または更新）します。
     * 
     * @param entity 通知情報エンティティ
     * @return Upsert件数
     */
    public int upsert(NtfInfoEntity entity) {
        return executeWithRetry(() -> ntfInfoMapper.upsert(entity), upsertRetryCount);
    }

    @Override
    /**
     * ユーザーIDで全通知情報を取得します。
     * 
     * @param internalUserId ユーザーID
     * @return 通知情報エンティティリスト
     */
    public List<NtfInfoEntity> selectAllByInternalUserId(String internalUserId) {
        return ntfInfoMapper.selectAllByInternalUserId(internalUserId);
    }

    /**
     * SQLTransientException発生時にリトライ処理を行います。
     * 
     * @param action     実行アクション
     * @param retryCount リトライ回数
     * @return アクション実行結果
     */
    private <T> T executeWithRetry(Callable<T> action, int retryCount) {
        int retry = 0;
        while (true) {
            try {
                return action.call();

            } catch (Exception e) {
                SQLException sqlEx = ExtractSqlExceptionUtil.findSqlException(e);
                if (!(sqlEx instanceof SQLTransientException)) {
                    if (sqlEx instanceof SQLNonTransientException) {
                        throw new RuntimeException(sqlEx);
                    } else if (sqlEx != null) {
                        throw new RuntimeException(sqlEx);
                    } else {
                        throw new RuntimeException(e);
                    }
                }
                retry++;
                if (retry > retryCount) {
                    throw new RuntimeException(e);
                }
                long wait = Math.min((long) (upsertRetryBaseInterval * Math.pow(2, retry - 1)), upsertRetryMaxInterval);
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
