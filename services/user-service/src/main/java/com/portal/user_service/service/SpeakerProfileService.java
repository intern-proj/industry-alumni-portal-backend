package com.portal.user_service.service;

import com.portal.user_service.dto.request.SpeakerProfileRequestDto;
import com.portal.user_service.dto.response.PageResponseDto;
import com.portal.user_service.dto.response.SpeakerProfileResponseDto;
import org.springframework.data.domain.Pageable;

public interface SpeakerProfileService {
    SpeakerProfileResponseDto createSpeakerProfile(SpeakerProfileRequestDto dto);
    SpeakerProfileResponseDto getSpeakerProfileById(String speakerId);
    SpeakerProfileResponseDto getSpeakerProfileByUserId(String userId);
    PageResponseDto<SpeakerProfileResponseDto> searchSpeakers(String query, Pageable pageable);
    SpeakerProfileResponseDto updateSpeakerProfile(String speakerId, SpeakerProfileRequestDto dto);
    void deleteSpeakerProfile(String speakerId);
}
