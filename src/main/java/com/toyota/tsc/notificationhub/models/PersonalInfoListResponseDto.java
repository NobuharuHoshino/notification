package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class PersonalInfoListResponseDto {
    private String resultCode;
    private List<PersonalInfoList> personalInfoList = new ArrayList<>();

    @Data
    public static class PersonalInfoList {
        private String internalUserId;
        private String userId;
        private String firstName;
        private String lastName;
        private String birthday;
        private String memberStatus;
        private List<ContactDto> contactList;
    }

    @Data
    public static class ContactDto {
        private int displayOrder;
        private String contactType;
        private String contact;
        private boolean primaryContactFlag;
    }
}
