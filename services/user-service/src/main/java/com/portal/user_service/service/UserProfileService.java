package com.portal.user_service.service;

import com.portal.user_service.dto.request.JobPreferenceRequestDto;
import com.portal.user_service.dto.request.UserProfileRequestDto;
import com.portal.user_service.dto.response.JobPreferenceResponseDto;
import com.portal.user_service.dto.response.UserProfileResponseDto;

import java.util.List;

public interface UserProfileService {

    UserProfileResponseDto createProfile(UserProfileRequestDto dto);

    UserProfileResponseDto getProfileByUserId(String userId);

    UserProfileResponseDto updateProfile(String userId, UserProfileRequestDto dto);

    UserProfileResponseDto toggleAvailability(String userId, boolean isActivelyLooking);

    JobPreferenceResponseDto updateJobPreference(String userId, JobPreferenceRequestDto dto);

    JobPreferenceResponseDto getJobPreference(String userId);

    List<UserProfileResponseDto> searchUsersBySkills(List<String> skills);
}
