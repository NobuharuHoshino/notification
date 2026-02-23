
package com.toyota.tsc.notificationhub.repositories;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NtfBatchExecHistoryEntity {
    private Integer registrationSerialNumber;
    private Integer sequenceNumber;
    private String status;
    private String scheduledSendData;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
