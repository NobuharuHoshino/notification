package com.toyota.tsc.notificationhub.repositories;

import java.util.List;

public interface NotificationVinListRepositoryIF {

    List<NotificationVinListEntity> select(Integer registrationSerialNumber, Integer isLinked);

    int update(Integer registrationSerialNumber, String sequenceNumber, Integer isLinked);
}
