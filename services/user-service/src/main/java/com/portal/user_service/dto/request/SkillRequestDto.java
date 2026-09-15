package com.portal.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillRequestDto {

    @NotBlank(message = "Skill name is required")
    private String skillName;

    private String skillLevel;
    private String category;
}
