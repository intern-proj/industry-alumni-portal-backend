package com.portal.userprofileservice.controller;

import com.portal.userprofileservice.dto.request.ResumeRequestDto;
import com.portal.userprofileservice.dto.response.ApiResponseDto;
import com.portal.userprofileservice.dto.response.ResumeResponseDto;
import com.portal.userprofileservice.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-profiles/{userId}/resumes")
@RequiredArgsConstructor
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<ResumeResponseDto>> addResume(
            @PathVariable String userId,
            @Valid @RequestBody ResumeRequestDto requestDto) {
        ResumeResponseDto created = resumeService.addResume(userId, requestDto);
        return new ResponseEntity<>(ApiResponseDto.success(created, "Resume uploaded successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<List<ResumeResponseDto>>> getResumes(@PathVariable String userId) {
        List<ResumeResponseDto> resumes = resumeService.getResumesByUserId(userId);
        return ResponseEntity.ok(ApiResponseDto.success(resumes, "Resumes retrieved successfully"));
    }

    @PatchMapping("/{resumeId}/primary")
    public ResponseEntity<ApiResponseDto<ResumeResponseDto>> setPrimaryResume(
            @PathVariable String userId,
            @PathVariable String resumeId) {
        ResumeResponseDto updated = resumeService.setPrimaryResume(userId, resumeId);
        return ResponseEntity.ok(ApiResponseDto.success(updated, "Primary resume updated successfully"));
    }

    @DeleteMapping("/{resumeId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteResume(
            @PathVariable String userId,
            @PathVariable String resumeId) {
        resumeService.deleteResume(userId, resumeId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Resume deleted successfully"));
    }
}
