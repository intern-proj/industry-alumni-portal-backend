package com.portal.userprofileservice.controller;

import com.portal.userprofileservice.dto.request.SpeakerProfileRequestDto;
import com.portal.userprofileservice.dto.response.ApiResponseDto;
import com.portal.userprofileservice.dto.response.PageResponseDto;
import com.portal.userprofileservice.dto.response.SpeakerProfileResponseDto;
import com.portal.userprofileservice.service.SpeakerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/speakers")
@RequiredArgsConstructor
public class SpeakerProfileController {

    private final SpeakerProfileService speakerProfileService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<SpeakerProfileResponseDto>> createSpeakerProfile(
            @Valid @RequestBody SpeakerProfileRequestDto requestDto) {
        SpeakerProfileResponseDto created = speakerProfileService.createSpeakerProfile(requestDto);
        return new ResponseEntity<>(ApiResponseDto.success(created, "Guest speaker profile created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/{speakerId}")
    public ResponseEntity<ApiResponseDto<SpeakerProfileResponseDto>> getSpeakerById(@PathVariable String speakerId) {
        SpeakerProfileResponseDto speaker = speakerProfileService.getSpeakerProfileById(speakerId);
        return ResponseEntity.ok(ApiResponseDto.success(speaker, "Speaker profile retrieved successfully"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponseDto<SpeakerProfileResponseDto>> getSpeakerByUserId(@PathVariable String userId) {
        SpeakerProfileResponseDto speaker = speakerProfileService.getSpeakerProfileByUserId(userId);
        return ResponseEntity.ok(ApiResponseDto.success(speaker, "Speaker profile retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<SpeakerProfileResponseDto>>> searchSpeakers(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        PageResponseDto<SpeakerProfileResponseDto> speakers = speakerProfileService.searchSpeakers(query, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(speakers, "Speaker profiles retrieved successfully"));
    }

    @PutMapping("/{speakerId}")
    public ResponseEntity<ApiResponseDto<SpeakerProfileResponseDto>> updateSpeakerProfile(
            @PathVariable String speakerId,
            @RequestBody SpeakerProfileRequestDto requestDto) {
        SpeakerProfileResponseDto updated = speakerProfileService.updateSpeakerProfile(speakerId, requestDto);
        return ResponseEntity.ok(ApiResponseDto.success(updated, "Speaker profile updated successfully"));
    }

    @DeleteMapping("/{speakerId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteSpeakerProfile(@PathVariable String speakerId) {
        speakerProfileService.deleteSpeakerProfile(speakerId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Speaker profile deleted successfully"));
    }
}
