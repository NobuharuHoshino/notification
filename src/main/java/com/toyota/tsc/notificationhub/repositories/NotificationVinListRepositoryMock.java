package com.toyota.tsc.notificationhub.repositories;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * 通知情報リポジトリ実装クラス
 */
@Repository
@Profile("local")
public class NotificationVinListRepositoryMock implements NotificationVinListRepositoryIF {

    @Override
    public List<NotificationVinListEntity> select(Integer registrationSerialNumber, Integer isLinked) {
        return new ArrayList<>();
    }

    @Override
    public int update(Integer registrationSerialNumber, Long sequenceNumber, Integer isLinked) {
        return 1;
    }
}