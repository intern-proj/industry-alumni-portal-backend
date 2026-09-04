package com.portal.user_service.service;

import com.portal.user_service.dto.request.SpeakerProfileRequestDto;
import com.portal.user_service.dto.response.PageResponseDto;
import com.portal.user_service.dto.response.SpeakerProfileResponseDto;
import com.portal.user_service.exception.ResourceNotFoundException;
import com.portal.user_service.model.SpeakerProfile;
import com.portal.user_service.repository.SpeakerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SpeakerProfileServiceImpl implements SpeakerProfileService {

    private final SpeakerProfileRepository speakerProfileRepository;

    @Override
    @Transactional
    public SpeakerProfileResponseDto createSpeakerProfile(SpeakerProfileRequestDto dto) {
        SpeakerProfile speaker = SpeakerProfile.builder()
                .speakerId(UUID.randomUUID().toString())
                .userId(dto.getUserId())
                .name(dto.getName())
                .organization(dto.getOrganization())
                .designation(dto.getDesignation())
                .bio(dto.getBio())
                .contactEmail(dto.getContactEmail())
                .contactPhone(dto.getContactPhone())
                .expertiseTags(dto.getExpertiseTags())
                .profilePicUrl(dto.getProfilePicUrl())
                .build();

        SpeakerProfile saved = speakerProfileRepository.save(speaker);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakerProfileResponseDto getSpeakerProfileById(String speakerId) {
        SpeakerProfile speaker = speakerProfileRepository.findById(speakerId)
                .orElseThrow(() -> new ResourceNotFoundException("Speaker profile not found for ID: " + speakerId));
        return mapToDto(speaker);
    }

    @Override
    @Transactional(readOnly = true)
    public SpeakerProfileResponseDto getSpeakerProfileByUserId(String userId) {
        SpeakerProfile speaker = speakerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Speaker profile not found for user ID: " + userId));
        return mapToDto(speaker);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto<SpeakerProfileResponseDto> searchSpeakers(String query, Pageable pageable) {
        Page<SpeakerProfile> page = speakerProfileRepository.searchSpeakers(query, pageable);
        return PageResponseDto.from(page.map(this::mapToDto));
    }

    @Override
    @Transactional
    public SpeakerProfileResponseDto updateSpeakerProfile(String speakerId, SpeakerProfileRequestDto dto) {
        SpeakerProfile existing = speakerProfileRepository.findById(speakerId)
                .orElseThrow(() -> new ResourceNotFoundException("Speaker profile not found for ID: " + speakerId));

        if (dto.getUserId() != null) existing.setUserId(dto.getUserId());
        if (dto.getName() != null) existing.setName(dto.getName());
        if (dto.getOrganization() != null) existing.setOrganization(dto.getOrganization());
        if (dto.getDesignation() != null) existing.setDesignation(dto.getDesignation());
        if (dto.getBio() != null) existing.setBio(dto.getBio());
        if (dto.getContactEmail() != null) existing.setContactEmail(dto.getContactEmail());
        if (dto.getContactPhone() != null) existing.setContactPhone(dto.getContactPhone());
        if (dto.getExpertiseTags() != null) existing.setExpertiseTags(dto.getExpertiseTags());
        if (dto.getProfilePicUrl() != null) existing.setProfilePicUrl(dto.getProfilePicUrl());

        SpeakerProfile saved = speakerProfileRepository.save(existing);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteSpeakerProfile(String speakerId) {
        SpeakerProfile existing = speakerProfileRepository.findById(speakerId)
                .orElseThrow(() -> new ResourceNotFoundException("Speaker profile not found for ID: " + speakerId));
        speakerProfileRepository.delete(existing);
    }

    private SpeakerProfileResponseDto mapToDto(SpeakerProfile speaker) {
        return SpeakerProfileResponseDto.builder()
                .speakerId(speaker.getSpeakerId())
                .userId(speaker.getUserId())
                .name(speaker.getName())
                .organization(speaker.getOrganization())
                .designation(speaker.getDesignation())
                .bio(speaker.getBio())
                .contactEmail(speaker.getContactEmail())
                .contactPhone(speaker.getContactPhone())
                .expertiseTags(speaker.getExpertiseTags())
                .profilePicUrl(speaker.getProfilePicUrl())
                .createdAt(speaker.getCreatedAt())
                .updatedAt(speaker.getUpdatedAt())
                .build();
    }
}
