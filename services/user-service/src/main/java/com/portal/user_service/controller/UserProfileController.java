package com.portal.user_service.controller;

import com.portal.user_service.dto.request.JobPreferenceRequestDto;
import com.portal.user_service.dto.request.UserProfileRequestDto;
import com.portal.user_service.dto.response.ApiResponseDto;
import com.portal.user_service.dto.response.JobPreferenceResponseDto;
import com.portal.user_service.dto.response.UserProfileResponseDto;
import com.portal.user_service.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> createProfile(
            @Valid @RequestBody UserProfileRequestDto requestDto) {
        UserProfileResponseDto created = userProfileService.createProfile(requestDto);
        return new ResponseEntity<>(ApiResponseDto.success(created, "User profile created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> getProfile(@PathVariable String userId) {
        UserProfileResponseDto profile = userProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponseDto.success(profile, "User profile retrieved successfully"));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> updateProfile(
            @PathVariable String userId,
            @RequestBody UserProfileRequestDto requestDto) {
        UserProfileResponseDto updated = userProfileService.updateProfile(userId, requestDto);
        return ResponseEntity.ok(ApiResponseDto.success(updated, "User profile updated successfully"));
    }

    @PatchMapping("/{userId}/availability")
    public ResponseEntity<ApiResponseDto<UserProfileResponseDto>> toggleAvailability(
            @PathVariable String userId,
            @RequestParam boolean isActivelyLooking) {
        UserProfileResponseDto updated = userProfileService.toggleAvailability(userId, isActivelyLooking);
        return ResponseEntity.ok(ApiResponseDto.success(updated, "Availability status updated successfully"));
    }

    @PutMapping("/{userId}/job-preferences")
    public ResponseEntity<ApiResponseDto<JobPreferenceResponseDto>> updateJobPreference(
            @PathVariable String userId,
            @RequestBody JobPreferenceRequestDto requestDto) {
        JobPreferenceResponseDto updated = userProfileService.updateJobPreference(userId, requestDto);
        return ResponseEntity.ok(ApiResponseDto.success(updated, "Job preferences updated successfully"));
    }

    @GetMapping("/{userId}/job-preferences")
    public ResponseEntity<ApiResponseDto<JobPreferenceResponseDto>> getJobPreference(@PathVariable String userId) {
        JobPreferenceResponseDto preference = userProfileService.getJobPreference(userId);
        return ResponseEntity.ok(ApiResponseDto.success(preference, "Job preferences retrieved successfully"));
    }

    @GetMapping("/search/skills")
    public ResponseEntity<ApiResponseDto<List<UserProfileResponseDto>>> searchUsersBySkills(
            @RequestParam List<String> skills) {
        List<UserProfileResponseDto> users = userProfileService.searchUsersBySkills(skills);
        return ResponseEntity.ok(ApiResponseDto.success(users, "Users with requested skills retrieved successfully"));
    }
}
