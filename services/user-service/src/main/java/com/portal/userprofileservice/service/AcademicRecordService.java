package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.AcademicRecordRequestDto;
import com.portal.userprofileservice.dto.response.AcademicRecordResponseDto;

public interface AcademicRecordService {
    AcademicRecordResponseDto createOrUpdateAcademicRecord(String userId, AcademicRecordRequestDto dto);
    AcademicRecordResponseDto getAcademicRecordByUserId(String userId);
    void deleteAcademicRecord(String userId);
}
