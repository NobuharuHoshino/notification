
package com.toyota.tsc.notificationhub.repositories;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SaNtfInfoEntity {
    private String internalUserId;
    private String deviceId;
    private String brdCd;
    private String platformType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
