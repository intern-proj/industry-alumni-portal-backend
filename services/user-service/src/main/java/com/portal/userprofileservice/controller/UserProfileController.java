package com.portal.userprofileservice.controller;

import com.portal.userprofileservice.dto.request.UserProfileRequestDto;
import com.portal.userprofileservice.dto.response.UserProfileResponseDto;
import com.portal.userprofileservice.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-profiles")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping
    public ResponseEntity<UserProfileResponseDto> createProfile(@RequestBody UserProfileRequestDto requestDto) {
        UserProfileResponseDto created = userProfileService.createProfile(requestDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDto> getProfile(@PathVariable String userId) {
        UserProfileResponseDto profile = userProfileService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserProfileResponseDto> updateProfile(@PathVariable String userId, @RequestBody UserProfileRequestDto requestDto) {
        UserProfileResponseDto updated = userProfileService.updateProfile(userId, requestDto);
        return ResponseEntity.ok(updated);
    }
}
