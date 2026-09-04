package com.portal.user_service.service;

import com.portal.user_service.dto.request.SkillRequestDto;
import com.portal.user_service.dto.response.SkillResponseDto;

import java.util.List;

public interface SkillService {
    SkillResponseDto addSkill(String userId, SkillRequestDto dto);
    List<SkillResponseDto> getSkillsByUserId(String userId);
    void deleteSkill(String userId, String skillId);
}
