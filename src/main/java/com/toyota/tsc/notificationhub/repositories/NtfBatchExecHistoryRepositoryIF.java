package com.toyota.tsc.notificationhub.repositories;

public interface NtfBatchExecHistoryRepositoryIF {

    int insert(NtfBatchExecHistoryEntity entity);

    int updateStatus(Integer registrationSerialNumber, Integer sequenceNumber, String status);
}
