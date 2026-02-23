
package com.toyota.tsc.notificationhub.repositories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * 通知情報リポジトリ実装クラス
 */
@Repository
@Profile("local")
public class NtfBatchExecErrorInfoRepositoryMock implements NtfBatchExecErrorInfoRepositoryIF {

    @Override
    public int insert(NtfBatchExecErrorInfoEntity entity) {
        return 1;
    }
}