package com.portal.userprofileservice.controller;

import com.portal.userprofileservice.dto.request.SkillRequestDto;
import com.portal.userprofileservice.dto.response.ApiResponseDto;
import com.portal.userprofileservice.dto.response.SkillResponseDto;
import com.portal.userprofileservice.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-profiles/{userId}/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<SkillResponseDto>> addSkill(
            @PathVariable String userId,
            @Valid @RequestBody SkillRequestDto requestDto) {
        SkillResponseDto created = skillService.addSkill(userId, requestDto);
        return new ResponseEntity<>(ApiResponseDto.success(created, "Skill added successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<SkillResponseDto>>> getSkills(@PathVariable String userId) {
        List<SkillResponseDto> skills = skillService.getSkillsByUserId(userId);
        return ResponseEntity.ok(ApiResponseDto.success(skills, "Skills retrieved successfully"));
    }

    @DeleteMapping("/{skillId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteSkill(
            @PathVariable String userId,
            @PathVariable String skillId) {
        skillService.deleteSkill(userId, skillId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Skill deleted successfully"));
    }
}
