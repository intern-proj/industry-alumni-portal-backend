package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.UserProfileRequestDto;
import com.portal.userprofileservice.dto.response.UserProfileResponseDto;

public interface UserProfileService {
    UserProfileResponseDto createProfile(UserProfileRequestDto requestDto);
    UserProfileResponseDto getProfileByUserId(String userId);
    UserProfileResponseDto updateProfile(String userId, UserProfileRequestDto requestDto);
}
