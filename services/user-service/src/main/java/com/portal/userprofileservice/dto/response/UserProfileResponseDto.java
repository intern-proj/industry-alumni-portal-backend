package com.portal.userprofileservice.dto.response;

import com.portal.userprofileservice.model.AccountStatus;
import com.portal.userprofileservice.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponseDto {
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
    private AcademicRecordResponseDto academicRecord;
    private List<SkillResponseDto> skills;
    private List<ResumeResponseDto> resumes;
    private JobPreferenceResponseDto jobPreference;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
