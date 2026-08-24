package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.UserAdminCreateRequestDto;
import com.portal.userprofileservice.dto.request.UserAdminUpdateRequestDto;
import com.portal.userprofileservice.dto.response.PageResponseDto;
import com.portal.userprofileservice.dto.response.UserAdminResponseDto;
import com.portal.userprofileservice.exception.InvalidOperationException;
import com.portal.userprofileservice.exception.ResourceNotFoundException;
import com.portal.userprofileservice.exception.UserAlreadyExistsException;
import com.portal.userprofileservice.model.AccountStatus;
import com.portal.userprofileservice.model.UserProfile;
import com.portal.userprofileservice.model.UserRole;
import com.portal.userprofileservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserProfileRepository userProfileRepository;

    private static final EnumSet<UserRole> ALLOWED_ADMIN_CREATION_ROLES = EnumSet.of(
            UserRole.SYSTEM_ADMIN,
            UserRole.FACULTY_MANAGEMENT,
            UserRole.FACULTY_COORDINATOR,
            UserRole.ACADEMIC_STAFF,
            UserRole.ADMINISTRATIVE_STAFF
    );

    @Override
    @Transactional
    public UserAdminResponseDto createManagementOrAdminUser(UserAdminCreateRequestDto dto) {
        if (!ALLOWED_ADMIN_CREATION_ROLES.contains(dto.getUserRole())) {
            throw new InvalidOperationException("System Admin can only register Admin and Management accounts. Role " 
                    + dto.getUserRole() + " is not permitted for admin direct registration.");
        }

        if (userProfileRepository.existsByEmail(dto.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + dto.getEmail() + " already exists.");
        }

        String userId = (dto.getUserId() != null && !dto.getUserId().isBlank())
                ? dto.getUserId()
                : UUID.randomUUID().toString();

        UserProfile userProfile = UserProfile.builder()
                .userId(userId)
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .bio(dto.getBio())
                .profilePicUrl(dto.getProfilePicUrl())
                .userRole(dto.getUserRole())
                .accountStatus(AccountStatus.ACTIVE)
                .faculty(dto.getFaculty())
                .department(dto.getDepartment())
                .isActivelyLooking(false)
                .build();

        UserProfile saved = userProfileRepository.save(userProfile);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAdminResponseDto getUserById(String userId) {
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID: " + userId));
        return mapToDto(userProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<UserAdminResponseDto> getAllUsers(String query, UserRole role, AccountStatus status, Pageable pageable) {
        Page<UserProfile> usersPage = userProfileRepository.searchUsers(query, role, status, pageable);
        Page<UserAdminResponseDto> dtoPage = usersPage.map(this::mapToDto);
        return PageResponseDto.from(dtoPage);
    }

    @Override
    @Transactional
    public UserAdminResponseDto updateUser(String userId, UserAdminUpdateRequestDto dto) {
        UserProfile existing = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID: " + userId));

        if (dto.getFirstName() != null) existing.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) existing.setLastName(dto.getLastName());
        if (dto.getPhone() != null) existing.setPhone(dto.getPhone());
        if (dto.getBio() != null) existing.setBio(dto.getBio());
        if (dto.getProfilePicUrl() != null) existing.setProfilePicUrl(dto.getProfilePicUrl());
        if (dto.getUserRole() != null) existing.setUserRole(dto.getUserRole());
        if (dto.getAccountStatus() != null) existing.setAccountStatus(dto.getAccountStatus());
        if (dto.getFaculty() != null) existing.setFaculty(dto.getFaculty());
        if (dto.getDepartment() != null) existing.setDepartment(dto.getDepartment());

        UserProfile updated = userProfileRepository.save(existing);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public UserAdminResponseDto updateAccountStatus(String userId, AccountStatus status) {
        UserProfile existing = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID: " + userId));
        existing.setAccountStatus(status);
        UserProfile saved = userProfileRepository.save(existing);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        UserProfile existing = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for ID: " + userId));
        userProfileRepository.delete(existing);
    }

    private UserAdminResponseDto mapToDto(UserProfile profile) {
        return UserAdminResponseDto.builder()
                .userId(profile.getUserId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .bio(profile.getBio())
                .profilePicUrl(profile.getProfilePicUrl())
                .userRole(profile.getUserRole())
                .accountStatus(profile.getAccountStatus())
                .faculty(profile.getFaculty())
                .department(profile.getDepartment())
                .isActivelyLooking(profile.getIsActivelyLooking())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
