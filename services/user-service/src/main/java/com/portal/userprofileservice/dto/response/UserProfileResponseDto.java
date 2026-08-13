package com.portal.userprofileservice.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String bio;
    private String profilePicUrl;
    private String userType;
    private Boolean isActivelyLooking;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
