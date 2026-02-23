
package com.toyota.tsc.notificationhub.repositories;

import java.sql.SQLException;
import java.sql.SQLTransientException;
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
public class NotificationRepositoryImpl implements NotificationRepositoryIF {
    private final NotificationMapper notificationMapper;
    private final PropertiesUtil propertiesUtil;

    @Autowired
    public NotificationRepositoryImpl(
            NotificationMapper notificationMapper,
            PropertiesUtil propertiesUtil) {
        this.notificationMapper = notificationMapper;
        this.propertiesUtil = propertiesUtil;
    }

    @Override
    /**
     * お知らせ対象リストの連携フラグを更新します。
     * 
     * @param regionCode               地域コード
     * @param registrationSerialNumber 登録連番
     * @return 更新件数
     */
    public int update(String regionCode, Integer registrationSerialNumber) {
        return executeWithRetry(
                () -> notificationMapper.update(
                        regionCode,
                        registrationSerialNumber),
                propertiesUtil.getNtfinfoUpsertRetryCount());
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
                if (shouldThrow(e, retry, retryCount)) {
                    throw toCustom(e);
                }
                sleep(backoffMillis(retry + 1));
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