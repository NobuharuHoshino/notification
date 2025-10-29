package com.toyota.tsc.notificationhub.repositories;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NtfInfoEntity {
    private String internalUserId;
    private String installationId;
    private String deviceToken;
    private String deviceId;
    private String brdCd;
    private String platformType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
