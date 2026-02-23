
package com.toyota.tsc.notificationhub.repositories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * 通知情報リポジトリ実装クラス
 */
@Repository
@Profile("local")
public class NotificationRepositoryMock implements NotificationRepositoryIF {

    @Override
    public int update(String regionCode, Integer registrationSerialNumber) {
        return 1;
    }
}