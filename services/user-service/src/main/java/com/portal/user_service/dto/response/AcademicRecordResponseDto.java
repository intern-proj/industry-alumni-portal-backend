package com.portal.user_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicRecordResponseDto {
    private String recordId;
    private String userId;
    private String faculty;
    private String department;
    private String degreeProgram;
    private Integer semester;
    private Integer year;
    private Double gpa;
    private String batch;
    private String transcriptUrl;
}
