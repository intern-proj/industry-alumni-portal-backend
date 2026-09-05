package com.portal.user_service.dto.request;

import com.portal.user_service.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequestDto {

    private String userId;

    @NotBlank(message = "First name is required")
    private String firstName;

    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String headline;
    private String bio;
    private String linkedinUrl;
    private String githubUrl;
    private String profilePicUrl;
    private UserRole userRole;
    private String faculty;
    private String department;
    private Boolean isActivelyLooking;
    private String projects;
}