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

    private String skillName;
    private String name;
    private String skillLevel;
    private String category;

    public String getSkillName() {
        if (skillName != null && !skillName.trim().isEmpty()) {
            return skillName.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        return null;
    }
}
