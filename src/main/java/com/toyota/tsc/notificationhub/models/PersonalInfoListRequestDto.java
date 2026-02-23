package com.toyota.tsc.notificationhub.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PersonalInfoListRequestDto {
    private String acquisitionType;
    private String distributorCode;
    private String dealerCode;
    private List<String> internalUserIdList;
}
