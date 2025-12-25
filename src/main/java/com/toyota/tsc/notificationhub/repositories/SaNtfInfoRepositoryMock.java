package com.toyota.tsc.notificationhub.repositories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * ローカル端末からAzureDBへの接続ができないため、デバッグ時はこちらのモックを利用してください。
 */
@Profile("local")
@Repository
public class SaNtfInfoRepositoryMock implements SaNtfInfoRepositoryIF {

    @Override
    public int upsert(SaNtfInfoEntity entity) {
        return 1; // モックなので常に成功
    }

    @Override
    public int delete(String internalUserId) {
        return 1; // モックなので常に成功
    }

    @Override
    public SaNtfInfoEntity select(String internalUserId) {
        return null; // モック
    }

    @Override
    public int update(SaNtfInfoEntity entity) {
        return 1; // モック
    }

    @Override
    public int insert(SaNtfInfoEntity entity) {
        return 1; // モック
    }
}
