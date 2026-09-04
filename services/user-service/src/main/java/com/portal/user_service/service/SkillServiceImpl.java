package com.portal.user_service.service;

import com.portal.user_service.dto.request.SkillRequestDto;
import com.portal.user_service.dto.response.SkillResponseDto;
import com.portal.user_service.exception.ResourceNotFoundException;
import com.portal.user_service.model.Skill;
import com.portal.user_service.repository.SkillRepository;
import com.portal.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public SkillResponseDto addSkill(String userId, SkillRequestDto dto) {
        if (!userProfileRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found for ID: " + userId);
        }

        Skill skill = Skill.builder()
                .skillId(UUID.randomUUID().toString())
                .userId(userId)
                .skillName(dto.getSkillName())
                .skillLevel(dto.getSkillLevel())
                .category(dto.getCategory())
                .build();

        Skill saved = skillRepository.save(skill);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SkillResponseDto> getSkillsByUserId(String userId) {
        return skillRepository.findByUserId(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSkill(String userId, String skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found for ID: " + skillId));

        if (!skill.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Skill does not belong to user ID: " + userId);
        }

        skillRepository.delete(skill);
    }

    private SkillResponseDto mapToDto(Skill skill) {
        return SkillResponseDto.builder()
                .skillId(skill.getSkillId())
                .userId(skill.getUserId())
                .skillName(skill.getSkillName())
                .skillLevel(skill.getSkillLevel())
                .category(skill.getCategory())
                .build();
    }
}
