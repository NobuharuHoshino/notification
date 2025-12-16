package com.toyota.tsc.notificationhub.repositories;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SaNtfInfoMapper {
        int upsert(@Param("entity") SaNtfInfoEntity entity);

        SaNtfInfoEntity select(@Param("internalUserId") String internalUserId);

        int insert(@Param("entity") SaNtfInfoEntity entity);

        int update(@Param("entity") SaNtfInfoEntity entity);

        int delete(@Param("internalUserId") String internalUserId);

        List<SaNtfInfoEntity> selectAllByInternalUserId(@Param("internalUserId") String internalUserId);
}
