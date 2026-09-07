package com.portal.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacultyRequestDto {

    @NotBlank(message = "Faculty name is required")
    private String name;

    private String code;
    private String description;
    private List<DepartmentRequestDto> departments;
}
