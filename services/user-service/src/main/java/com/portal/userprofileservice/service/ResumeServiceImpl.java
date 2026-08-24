package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.ResumeRequestDto;
import com.portal.userprofileservice.dto.response.ResumeResponseDto;
import com.portal.userprofileservice.exception.ResourceNotFoundException;
import com.portal.userprofileservice.model.Resume;
import com.portal.userprofileservice.repository.ResumeRepository;
import com.portal.userprofileservice.repository.UserProfileRepository;
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

        Resume resume = Resume.builder()
                .resumeId(UUID.randomUUID().toString())
                .userId(userId)
                .fileUrl(dto.getFileUrl())
                .fileName(dto.getFileName())
                .isPrimary(shouldBePrimary)
                .build();

        Resume saved = resumeRepository.save(resume);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponseDto> getResumesByUserId(String userId) {
        return resumeRepository.findByUserId(userId).stream()
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
    public void deleteResume(String userId, String resumeId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found for ID: " + resumeId));

        if (!resume.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Resume does not belong to user ID: " + userId);
        }

        resumeRepository.delete(resume);
    }

    private ResumeResponseDto mapToDto(Resume resume) {
        return ResumeResponseDto.builder()
                .resumeId(resume.getResumeId())
                .userId(resume.getUserId())
                .fileUrl(resume.getFileUrl())
                .fileName(resume.getFileName())
                .isPrimary(resume.getIsPrimary())
                .uploadedAt(resume.getUploadedAt())
                .build();
    }
}
