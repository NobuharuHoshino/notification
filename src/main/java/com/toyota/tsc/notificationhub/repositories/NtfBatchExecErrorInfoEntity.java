
package com.toyota.tsc.notificationhub.repositories;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NtfBatchExecErrorInfoEntity {
    private Integer registrationSerialNumber;
    private Integer sequenceNumber;
    private String internalUserId;
    private String notificationStatus;
    private String pushStatus;
    private String primaryContactStatus;
    private LocalDateTime erroredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
