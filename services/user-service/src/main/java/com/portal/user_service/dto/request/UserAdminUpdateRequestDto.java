package com.portal.user_service.dto.request;

import com.portal.user_service.model.AccountStatus;
import com.portal.user_service.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminUpdateRequestDto {
    private String firstName;
    private String lastName;
    private String phone;
    private String bio;
    private String profilePicUrl;
    private UserRole userRole;
    private AccountStatus accountStatus;
    private String faculty;
    private String department;
}
