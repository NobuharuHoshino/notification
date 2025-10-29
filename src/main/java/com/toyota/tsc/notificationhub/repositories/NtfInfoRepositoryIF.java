package com.toyota.tsc.notificationhub.repositories;

import java.util.List;

public interface NtfInfoRepositoryIF {
    int insert(NtfInfoEntity entity);

    int update(NtfInfoEntity entity);

    int delete(String internalUserId, String installationId);

    NtfInfoEntity select(String internalUserId, String installationId);

    int upsert(NtfInfoEntity entity);

    List<NtfInfoEntity> selectAllByInternalUserId(String internalUserId);
}
