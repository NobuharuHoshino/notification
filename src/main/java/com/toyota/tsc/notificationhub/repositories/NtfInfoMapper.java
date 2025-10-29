package com.toyota.tsc.notificationhub.repositories;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NtfInfoMapper {
        int upsert(@Param("entity") NtfInfoEntity entity);

        NtfInfoEntity select(@Param("internalUserId") String internalUserId,
                        @Param("installationId") String installationId);

        int insert(@Param("entity") NtfInfoEntity entity);

        int update(@Param("entity") NtfInfoEntity entity);

        int delete(@Param("internalUserId") String internalUserId, @Param("installationId") String installationId);

        List<NtfInfoEntity> selectAllByInternalUserId(@Param("internalUserId") String internalUserId);
}
