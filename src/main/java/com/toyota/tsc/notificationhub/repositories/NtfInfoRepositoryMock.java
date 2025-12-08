package com.toyota.tsc.notificationhub.repositories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import java.util.Collections;
import java.util.List;

/**
 * ローカル端末からAzureDBへの接続ができないため、デバッグ時はこちらのモックを利用してください。
 */
@Profile("local")
@Repository
public class NtfInfoRepositoryMock implements NtfInfoRepositoryIF {
    @Override
    public List<NtfInfoEntity> selectAllByInternalUserId(String internalUserId) {
        return Collections.emptyList();
    }

    @Override
    public int upsert(NtfInfoEntity entity) {
        return 1; // モックなので常に成功
    }

    @Override
    public int delete(String internalUserId, String installationId) {
        return 1; // モックなので常に成功
    }

    @Override
    public NtfInfoEntity select(String internalUserId, String installationId) {
        return null; // モック
    }

    @Override
    public int update(NtfInfoEntity entity) {
        return 1; // モック
    }

    @Override
    public int insert(NtfInfoEntity entity) {
        return 1; // モック
    }
}
