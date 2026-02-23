
package com.toyota.tsc.notificationhub.repositories;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationVinListEntity {
    private Integer id;
    private String regionCode; // DBではregeion_code
    private Integer notificationId;
    private String vinList;
    private String notificationSendList; // DBではnotification_send_list
    private Long registrationSerialNumber;
    private String sequenceNumber; // DBではseqence_number
    private Integer isLinked;
    private Boolean isDeleted;
    private LocalDateTime created;
    private LocalDateTime updated;
}
