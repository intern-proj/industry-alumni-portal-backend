package com.nsbm.application_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfileDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String profilePicUrl;
    private String faculty;
    private String department;
    private AcademicRecordDto academicRecord;
    private List<SkillDto> skills;
    
    public String getFullName() {
        return (firstName != null ? firstName : "") + (lastName != null ? " " + lastName : "");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AcademicRecordDto {
        private String facultyName;
        private String degreeProgram;
        private Double gpa;
        private String batch;
        private String currentYear;
        private String expectedGraduation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SkillDto {
        private String skillId;
        private String skillName;
        private String skillLevel;
        private String category;
    }
}

