package com.portal.userprofileservice.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileRequestDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String bio;
    private String profilePicUrl;
    private String userType;
    private Boolean isActivelyLooking;
}