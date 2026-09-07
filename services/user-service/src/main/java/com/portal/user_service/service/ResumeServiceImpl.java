package com.portal.user_service.service;

import com.portal.user_service.dto.request.ResumeRequestDto;
import com.portal.user_service.dto.response.ResumeResponseDto;
import com.portal.user_service.exception.ResourceNotFoundException;
import com.portal.user_service.model.Resume;
import com.portal.user_service.repository.ResumeRepository;
import com.portal.user_service.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public ResumeResponseDto addResume(String userId, ResumeRequestDto dto) {
        if (!userProfileRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found for ID: " + userId);
        }

        boolean isFirstResume = resumeRepository.findByUserId(userId).isEmpty();
        boolean shouldBePrimary = isFirstResume || (dto.getIsPrimary() != null && dto.getIsPrimary());

        if (shouldBePrimary) {
            resumeRepository.resetPrimaryResumes(userId);
        }

        String effectiveTitle = (dto.getTitle() != null && !dto.getTitle().isBlank())
                ? dto.getTitle()
                : (dto.getFileName() != null ? dto.getFileName() : "Resume");

        Resume resume = Resume.builder()
                .resumeId(UUID.randomUUID().toString())
                .userId(userId)
                .title(effectiveTitle)
                .fileUrl(dto.getFileUrl())
                .fileName(dto.getFileName())
                .fileSize(dto.getFileSize())
                .targetRole(dto.getTargetRole())
                .storageFileId(dto.getStorageFileId())
                .isPrimary(shouldBePrimary)
                .build();

        Resume saved = resumeRepository.save(resume);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponseDto> getResumesByUserId(String userId) {
        return resumeRepository.findByUserIdOrderByUploadedAtDesc(userId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResumeResponseDto setPrimaryResume(String userId, String resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found for ID: " + resumeId));

        if (!resume.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Resume does not belong to user ID: " + userId);
        }

        resumeRepository.resetPrimaryResumes(userId);
        resume.setIsPrimary(true);
        Resume updated = resumeRepository.save(resume);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public ResumeResponseDto updateResume(String userId, String resumeId, ResumeRequestDto dto) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found for ID: " + resumeId));

        if (!resume.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Resume does not belong to user ID: " + userId);
        }

        if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
            resume.setTitle(dto.getTitle());
        }
        if (dto.getTargetRole() != null) {
            resume.setTargetRole(dto.getTargetRole());
        }
        if (dto.getIsPrimary() != null && dto.getIsPrimary()) {
            resumeRepository.resetPrimaryResumes(userId);
            resume.setIsPrimary(true);
        }

        Resume updated = resumeRepository.save(resume);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteResume(String userId, String resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found for ID: " + resumeId));

        if (!resume.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Resume does not belong to user ID: " + userId);
        }

        boolean wasPrimary = Boolean.TRUE.equals(resume.getIsPrimary());
        resumeRepository.delete(resume);

        if (wasPrimary) {
            List<Resume> remaining = resumeRepository.findByUserIdOrderByUploadedAtDesc(userId);
            if (!remaining.isEmpty()) {
                Resume nextPrimary = remaining.get(0);
                nextPrimary.setIsPrimary(true);
                resumeRepository.save(nextPrimary);
            }
        }
    }

    private ResumeResponseDto mapToDto(Resume resume) {
        return ResumeResponseDto.builder()
                .resumeId(resume.getResumeId())
                .userId(resume.getUserId())
                .title(resume.getTitle())
                .fileUrl(resume.getFileUrl())
                .fileName(resume.getFileName())
                .fileSize(resume.getFileSize())
                .targetRole(resume.getTargetRole())
                .storageFileId(resume.getStorageFileId())
                .isPrimary(resume.getIsPrimary())
                .uploadedAt(resume.getUploadedAt())
                .build();
    }
}
