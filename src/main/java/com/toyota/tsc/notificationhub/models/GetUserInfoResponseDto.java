package com.toyota.tsc.notificationhub.models;

import lombok.Data;
import java.util.List;

@Data
public class GetUserInfoResponseDto {
    private String userId;
    private String country;
    private String firstName;
    private String lastName;
    private String dateOfBirth;
    private List<ContactDto> contactList;

    @Data
    public static class ContactDto {
        private String contactType;
        private String contact;
        private boolean primaryContactFlag;
    }
}
