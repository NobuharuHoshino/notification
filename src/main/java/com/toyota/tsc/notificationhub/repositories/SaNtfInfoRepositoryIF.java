package com.toyota.tsc.notificationhub.repositories;

public interface SaNtfInfoRepositoryIF {
    int insert(SaNtfInfoEntity entity);

    int update(SaNtfInfoEntity entity);

    int delete(String internalUserId);

    SaNtfInfoEntity select(String internalUserId);

    int upsert(SaNtfInfoEntity entity);
}
