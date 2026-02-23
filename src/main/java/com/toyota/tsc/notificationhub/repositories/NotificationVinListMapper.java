package com.toyota.tsc.notificationhub.repositories;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationVinListMapper {

        // 取得
        List<NotificationVinListEntity> select(
                        @Param("registrationSerialNumber") Integer registrationSerialNumber,
                        @Param("isLinked") Integer isLinked);

        // 更新
        int update(
                        @Param("registrationSerialNumber") Integer registrationSerialNumber,
                        @Param("sequenceNumber") String sequenceNumber,
                        @Param("isLinked") Integer isLinked);
}
