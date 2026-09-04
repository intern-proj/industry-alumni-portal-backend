package com.portal.user_service.dto.response;

import com.portal.user_service.model.AccountStatus;
import com.portal.user_service.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminResponseDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String bio;
    private String profilePicUrl;
    private UserRole userRole;
    private AccountStatus accountStatus;
    private String faculty;
    private String department;
    private Boolean isActivelyLooking;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
