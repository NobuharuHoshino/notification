package com.toyota.tsc.notificationhub.repositories;

import java.util.List;

import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.SQLTransientException;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Repository
public class NtfInfoRepository implements NtfInfoRepositoryIF {
    private final NtfInfoMapper ntfInfoMapper;

    @Value("${ntfinfo.upsert.retry.count:3}")
    private int upsertRetryCount;

    @Value("${ntfinfo.upsert.retry.base-interval:100}")
    private long upsertRetryBaseInterval;

    @Value("${ntfinfo.upsert.retry.max-interval:20000}")
    private long upsertRetryMaxInterval;

    @Autowired
    public NtfInfoRepository(NtfInfoMapper ntfInfoMapper) {
        this.ntfInfoMapper = ntfInfoMapper;
    }

    @Override
    public int insert(NtfInfoEntity entity) {
        return ntfInfoMapper.insert(entity);
    }

    @Override
    public int update(NtfInfoEntity entity) {
        return ntfInfoMapper.update(entity);
    }

    @Override
    public int delete(String internalUserId, String installationId) {
        return ntfInfoMapper.delete(internalUserId, installationId);
    }

    @Override
    public NtfInfoEntity select(String internalUserId, String installationId) {
        return ntfInfoMapper.select(internalUserId, installationId);
    }

    @Override
    public int upsert(NtfInfoEntity entity) {
        return executeWithRetry(() -> ntfInfoMapper.upsert(entity), upsertRetryCount);
    }

    @Override
    public List<NtfInfoEntity> selectAllByInternalUserId(String internalUserId) {
        return ntfInfoMapper.selectAllByInternalUserId(internalUserId);
    }

    private <T> T executeWithRetry(Callable<T> action, int retryCount) {
        int retry = 0;
        while (true) {
            try {
                // Excecute Database Action
                return action.call();

            } catch (Exception e) {
                // Handle SQLException for retry logic
                SQLException sqlEx = null;
                if (e instanceof SQLException) {
                    sqlEx = (SQLException) e;
                } else if (e.getCause() instanceof SQLException) {
                    sqlEx = (SQLException) e.getCause();
                }
                // Determine if we should retry or not
                if (!(sqlEx instanceof SQLTransientException)) {
                    if (sqlEx instanceof SQLNonTransientException) {
                        throw new RuntimeException("DBリトライ非推奨例外(SQLNonTransientException)", sqlEx);
                    } else if (sqlEx != null) {
                        throw new RuntimeException("DBリトライ非推奨例外(SQLException)", sqlEx);
                    } else {
                        throw new RuntimeException(e);
                    }
                }
                retry++;
                // Check if retry limit exceeded
                if (retry > retryCount) {
                    throw new RuntimeException(e);
                }
                // Exponential backoff before next retry
                long wait = Math.min((long) (upsertRetryBaseInterval * Math.pow(2, retry - 1)), upsertRetryMaxInterval);
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
