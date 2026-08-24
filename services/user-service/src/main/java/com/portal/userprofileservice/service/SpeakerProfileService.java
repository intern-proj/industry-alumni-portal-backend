package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.SpeakerProfileRequestDto;
import com.portal.userprofileservice.dto.response.PageResponseDto;
import com.portal.userprofileservice.dto.response.SpeakerProfileResponseDto;
import org.springframework.data.domain.Pageable;

public interface SpeakerProfileService {
    SpeakerProfileResponseDto createSpeakerProfile(SpeakerProfileRequestDto dto);
    SpeakerProfileResponseDto getSpeakerProfileById(String speakerId);
    SpeakerProfileResponseDto getSpeakerProfileByUserId(String userId);
    PageResponseDto<SpeakerProfileResponseDto> searchSpeakers(String query, Pageable pageable);
    SpeakerProfileResponseDto updateSpeakerProfile(String speakerId, SpeakerProfileRequestDto dto);
    void deleteSpeakerProfile(String speakerId);
}
