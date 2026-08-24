package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.ResumeRequestDto;
import com.portal.userprofileservice.dto.response.ResumeResponseDto;

import java.util.List;

public interface ResumeService {
    ResumeResponseDto addResume(String userId, ResumeRequestDto dto);
    List<ResumeResponseDto> getResumesByUserId(String userId);
    ResumeResponseDto setPrimaryResume(String userId, String resumeId);
    void deleteResume(String userId, String resumeId);
}
