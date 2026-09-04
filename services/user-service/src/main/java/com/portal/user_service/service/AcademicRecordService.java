package com.portal.user_service.service;

import com.portal.user_service.dto.request.AcademicRecordRequestDto;
import com.portal.user_service.dto.response.AcademicRecordResponseDto;

public interface AcademicRecordService {
    AcademicRecordResponseDto createOrUpdateAcademicRecord(String userId, AcademicRecordRequestDto dto);
    AcademicRecordResponseDto getAcademicRecordByUserId(String userId);
    void deleteAcademicRecord(String userId);
}
