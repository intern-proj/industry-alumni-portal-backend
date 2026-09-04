package com.portal.user_service.service;

import com.portal.user_service.dto.request.UserAdminCreateRequestDto;
import com.portal.user_service.dto.request.UserAdminUpdateRequestDto;
import com.portal.user_service.dto.response.PageResponseDto;
import com.portal.user_service.dto.response.UserAdminResponseDto;
import com.portal.user_service.exception.InvalidOperationException;
import com.portal.user_service.exception.ResourceNotFoundException;
import com.portal.user_service.exception.UserAlreadyExistsException;
import com.portal.user_service.model.AccountStatus;
import com.portal.user_service.model.UserProfile;
import com.portal.user_service.model.UserRole;
import com.portal.user_service.repository.UserProfileRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserProfileRepository userProfileRepository;

    private static final EnumSet<UserRole> ALLOWED_ADMIN_CREATION_ROLES = EnumSet.of(
            UserRole.SYSTEM_ADMIN,
            UserRole.FACULTY_MANAGEMENT,
            UserRole.FACULTY_COORDINATOR,
            UserRole.EVENT_COORDINATOR,
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
        Specification<UserProfile> spec = (root, q, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                String pattern = "%" + query.trim().toLowerCase() + "%";
                Predicate firstMatch = cb.like(cb.lower(root.get("firstName")), pattern);
                Predicate lastMatch = cb.like(cb.lower(root.get("lastName")), pattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), pattern);
                Predicate userMatch = cb.like(cb.lower(root.get("userId")), pattern);
                predicates.add(cb.or(firstMatch, lastMatch, emailMatch, userMatch));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("userRole"), role));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("accountStatus"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserProfile> usersPage = userProfileRepository.findAll(spec, pageable);
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
