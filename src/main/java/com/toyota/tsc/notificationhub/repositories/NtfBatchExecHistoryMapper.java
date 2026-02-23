package com.toyota.tsc.notificationhub.repositories;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NtfBatchExecHistoryMapper {

        int insert(@Param("entity") NtfBatchExecHistoryEntity entity);

        int updateStatus(@Param("registrationSerialNumber") Integer registrationSerialNumber,
                        @Param("sequenceNumber") Integer sequenceNumber,
                        @Param("status") String status);
}
