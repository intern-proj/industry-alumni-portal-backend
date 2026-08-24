package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.JobPreferenceRequestDto;
import com.portal.userprofileservice.dto.request.UserProfileRequestDto;
import com.portal.userprofileservice.dto.response.*;
import com.portal.userprofileservice.exception.ResourceNotFoundException;
import com.portal.userprofileservice.exception.UserAlreadyExistsException;
import com.portal.userprofileservice.model.*;
import com.portal.userprofileservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AcademicRecordRepository academicRecordRepository;
    private final SkillRepository skillRepository;
    private final ResumeRepository resumeRepository;
    private final JobPreferenceRepository jobPreferenceRepository;

    @Override
    @Transactional
    public UserProfileResponseDto createProfile(UserProfileRequestDto dto) {
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
                .userRole(dto.getUserRole() != null ? dto.getUserRole() : UserRole.STUDENT)
                .accountStatus(AccountStatus.ACTIVE)
                .faculty(dto.getFaculty())
                .department(dto.getDepartment())
                .isActivelyLooking(dto.getIsActivelyLooking() != null ? dto.getIsActivelyLooking() : false)
                .build();

        UserProfile saved = userProfileRepository.save(userProfile);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponseDto getProfileByUserId(String userId) {
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));
        return mapToDto(userProfile);
    }

    @Override
    @Transactional
    public UserProfileResponseDto updateProfile(String userId, UserProfileRequestDto dto) {
        UserProfile existing = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));

        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());
        existing.setPhone(dto.getPhone());
        existing.setBio(dto.getBio());
        existing.setProfilePicUrl(dto.getProfilePicUrl());
        if (dto.getFaculty() != null) existing.setFaculty(dto.getFaculty());
        if (dto.getDepartment() != null) existing.setDepartment(dto.getDepartment());
        if (dto.getUserRole() != null) existing.setUserRole(dto.getUserRole());
        if (dto.getIsActivelyLooking() != null) {
            existing.setIsActivelyLooking(dto.getIsActivelyLooking());
        }

        UserProfile updated = userProfileRepository.save(existing);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public UserProfileResponseDto toggleAvailability(String userId, boolean isActivelyLooking) {
        UserProfile existing = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found for ID: " + userId));
        existing.setIsActivelyLooking(isActivelyLooking);
        UserProfile saved = userProfileRepository.save(existing);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public JobPreferenceResponseDto updateJobPreference(String userId, JobPreferenceRequestDto dto) {
        if (!userProfileRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User profile not found for ID: " + userId);
        }

        JobPreference preference = jobPreferenceRepository.findByUserId(userId)
                .orElse(JobPreference.builder()
                        .preferenceId(UUID.randomUUID().toString())
                        .userId(userId)
                        .build());

        preference.setJobRole(dto.getJobRole());
        preference.setLocation(dto.getLocation());
        preference.setJobType(dto.getJobType());

        JobPreference saved = jobPreferenceRepository.save(preference);
        return JobPreferenceResponseDto.builder()
                .preferenceId(saved.getPreferenceId())
                .userId(saved.getUserId())
                .jobRole(saved.getJobRole())
                .location(saved.getLocation())
                .jobType(saved.getJobType())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public JobPreferenceResponseDto getJobPreference(String userId) {
        JobPreference preference = jobPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Job preference not found for user ID: " + userId));
        return JobPreferenceResponseDto.builder()
                .preferenceId(preference.getPreferenceId())
                .userId(preference.getUserId())
                .jobRole(preference.getJobRole())
                .location(preference.getLocation())
                .jobType(preference.getJobType())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfileResponseDto> searchUsersBySkills(List<String> skills) {
        if (skills == null || skills.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> lowerSkills = skills.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<String> userIds = userProfileRepository.findUserIdsBySkills(lowerSkills);
        return userProfileRepository.findAllById(userIds).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private UserProfileResponseDto mapToDto(UserProfile profile) {
        AcademicRecordResponseDto academicDto = academicRecordRepository.findByUserId(profile.getUserId())
                .map(ar -> AcademicRecordResponseDto.builder()
                        .recordId(ar.getRecordId())
                        .userId(ar.getUserId())
                        .faculty(ar.getFaculty())
                        .department(ar.getDepartment())
                        .degreeProgram(ar.getDegreeProgram())
                        .semester(ar.getSemester())
                        .year(ar.getYear())
                        .gpa(ar.getGpa())
                        .batch(ar.getBatch())
                        .transcriptUrl(ar.getTranscriptUrl())
                        .build())
                .orElse(null);

        List<SkillResponseDto> skillDtos = skillRepository.findByUserId(profile.getUserId()).stream()
                .map(s -> SkillResponseDto.builder()
                        .skillId(s.getSkillId())
                        .userId(s.getUserId())
                        .skillName(s.getSkillName())
                        .skillLevel(s.getSkillLevel())
                        .category(s.getCategory())
                        .build())
                .collect(Collectors.toList());

        List<ResumeResponseDto> resumeDtos = resumeRepository.findByUserId(profile.getUserId()).stream()
                .map(r -> ResumeResponseDto.builder()
                        .resumeId(r.getResumeId())
                        .userId(r.getUserId())
                        .fileUrl(r.getFileUrl())
                        .fileName(r.getFileName())
                        .isPrimary(r.getIsPrimary())
                        .uploadedAt(r.getUploadedAt())
                        .build())
                .collect(Collectors.toList());

        JobPreferenceResponseDto jobPreferenceDto = jobPreferenceRepository.findByUserId(profile.getUserId())
                .map(jp -> JobPreferenceResponseDto.builder()
                        .preferenceId(jp.getPreferenceId())
                        .userId(jp.getUserId())
                        .jobRole(jp.getJobRole())
                        .location(jp.getLocation())
                        .jobType(jp.getJobType())
                        .build())
                .orElse(null);

        return UserProfileResponseDto.builder()
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
                .academicRecord(academicDto)
                .skills(skillDtos)
                .resumes(resumeDtos)
                .jobPreference(jobPreferenceDto)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
