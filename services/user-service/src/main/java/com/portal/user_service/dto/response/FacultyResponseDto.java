package com.portal.user_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyResponseDto {
    private String facultyId;
    private String name;
    private String code;
    private String description;
    private List<DepartmentResponseDto> departments;
}
