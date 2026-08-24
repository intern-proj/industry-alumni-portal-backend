package com.portal.userprofileservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicRecordRequestDto {
    private String faculty;
    private String department;
    private String degreeProgram;
    @Min(value = 1, message = "Semester must be at least 1")
    @Max(value = 12, message = "Semester cannot exceed 12")
    private Integer semester;
    private Integer year;
    @Min(value = 0, message = "GPA cannot be negative")
    @Max(value = 4, message = "GPA cannot exceed 4.0")
    private Double gpa;
    private String batch;
    private String transcriptUrl;
}
