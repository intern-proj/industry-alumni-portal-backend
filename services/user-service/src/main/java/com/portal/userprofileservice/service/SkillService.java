package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.SkillRequestDto;
import com.portal.userprofileservice.dto.response.SkillResponseDto;

import java.util.List;

public interface SkillService {
    SkillResponseDto addSkill(String userId, SkillRequestDto dto);
    List<SkillResponseDto> getSkillsByUserId(String userId);
    void deleteSkill(String userId, String skillId);
}
