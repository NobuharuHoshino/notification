package com.toyota.tsc.notificationhub.repositories;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {

        // 更新
        int update(
                        @Param("regionCode") String regionCode,
                        @Param("registrationSerialNumber") Integer registrationSerialNumber);
}
