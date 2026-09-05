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

        String resolvedName = dto.getSkillName();
        if (resolvedName == null || resolvedName.trim().isEmpty()) {
            throw new IllegalArgumentException("Skill name is required");
        }

        // Check if skill already exists for this user to avoid duplicates
        java.util.Optional<Skill> existing = skillRepository.findFirstByUserIdAndSkillNameIgnoreCase(userId, resolvedName.trim());
        if (existing.isPresent()) {
            return mapToDto(existing.get());
        }

        Skill skill = Skill.builder()
                .skillId(UUID.randomUUID().toString())
                .userId(userId)
                .skillName(resolvedName.trim())
                .skillLevel(dto.getSkillLevel() != null ? dto.getSkillLevel() : "INTERMEDIATE")
                .category(dto.getCategory() != null ? dto.getCategory() : "TECHNICAL")
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
    public void deleteSkill(String userId, String skillIdOrName) {
        if (skillIdOrName == null || skillIdOrName.trim().isEmpty()) {
            return;
        }

        // 1. Try finding by skillId
        java.util.Optional<Skill> byId = skillRepository.findById(skillIdOrName.trim());
        if (byId.isPresent()) {
            if (byId.get().getUserId().equals(userId)) {
                skillRepository.delete(byId.get());
                return;
            }
        }

        // 2. Try finding by skillName for this user
        java.util.Optional<Skill> byName = skillRepository.findFirstByUserIdAndSkillNameIgnoreCase(userId, skillIdOrName.trim());
        if (byName.isPresent()) {
            skillRepository.delete(byName.get());
            return;
        }

        // Idempotent: if skill was already deleted or doesn't exist, do not fail
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
