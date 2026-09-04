package com.portal.user_service.service;

import com.portal.user_service.dto.request.ResumeRequestDto;
import com.portal.user_service.dto.response.ResumeResponseDto;

import java.util.List;

public interface ResumeService {
    ResumeResponseDto addResume(String userId, ResumeRequestDto dto);
    List<ResumeResponseDto> getResumesByUserId(String userId);
    ResumeResponseDto setPrimaryResume(String userId, String resumeId);
    ResumeResponseDto updateResume(String userId, String resumeId, ResumeRequestDto dto);
    void deleteResume(String userId, String resumeId);
}
