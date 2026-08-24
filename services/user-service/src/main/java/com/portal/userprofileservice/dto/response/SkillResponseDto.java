package com.portal.userprofileservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillResponseDto {
    private String skillId;
    private String userId;
    private String skillName;
    private String skillLevel;
    private String category;
}
