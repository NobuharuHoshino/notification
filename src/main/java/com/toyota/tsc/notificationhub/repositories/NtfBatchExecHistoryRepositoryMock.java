
package com.toyota.tsc.notificationhub.repositories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * 通知情報リポジトリ実装クラス
 */
@Repository
@Profile("local")
public class NtfBatchExecHistoryRepositoryMock implements NtfBatchExecHistoryRepositoryIF {

    @Override
    /**
     * バッチ実行履歴を新規登録します。
     * 
     * @param <T>
     * @param action
     * @param retryCount
     * @return
     */
    public int insert(NtfBatchExecHistoryEntity entity) {
        return 1;
    }

    @Override
    public int updateStatus(Integer registrationSerialNumber, Integer sequenceNumber, String status) {
        return 1;
    }
}