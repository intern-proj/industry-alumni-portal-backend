package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.UserProfileRequestDto;
import com.portal.userprofileservice.dto.response.UserProfileResponseDto;
import com.portal.userprofileservice.exception.ResourceNotFoundException;
import com.portal.userprofileservice.model.UserProfile;
import com.portal.userprofileservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;

    @Override
    public UserProfileResponseDto createProfile(UserProfileRequestDto dto) {
        if (userProfileRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("User with email " + dto.getEmail() + " already exists.");
        }

        UserProfile userProfile = UserProfile.builder()
                .userId(dto.getUserId())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .bio(dto.getBio())
                .profilePicUrl(dto.getProfilePicUrl())
                .userType(dto.getUserType())
                .isActivelyLooking(dto.getIsActivelyLooking() != null ? dto.getIsActivelyLooking() : false)
                .build();

        UserProfile saved = userProfileRepository.save(userProfile);
        return mapToDto(saved);
    }

    @Override
    public UserProfileResponseDto getProfileByUserId(String userId) {
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));
        return mapToDto(userProfile);
    }

    @Override
    public UserProfileResponseDto updateProfile(String userId, UserProfileRequestDto dto) {
        UserProfile existing = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));

        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());
        existing.setPhone(dto.getPhone());
        existing.setBio(dto.getBio());
        existing.setProfilePicUrl(dto.getProfilePicUrl());
        existing.setUserType(dto.getUserType());
        if (dto.getIsActivelyLooking() != null) {
            existing.setIsActivelyLooking(dto.getIsActivelyLooking());
        }

        UserProfile updated = userProfileRepository.save(existing);
        return mapToDto(updated);
    }

    private UserProfileResponseDto mapToDto(UserProfile profile) {
        return UserProfileResponseDto.builder()
                .userId(profile.getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .bio(profile.getBio())
                .profilePicUrl(profile.getProfilePicUrl())
                .userType(profile.getUserType())
                .isActivelyLooking(profile.getIsActivelyLooking())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
