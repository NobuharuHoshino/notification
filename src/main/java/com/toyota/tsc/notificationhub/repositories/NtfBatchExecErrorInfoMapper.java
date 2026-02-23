package com.toyota.tsc.notificationhub.repositories;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NtfBatchExecErrorInfoMapper {

        int insert(@Param("entity") NtfBatchExecErrorInfoEntity entity);
}
