package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.AcademicRecordRequestDto;
import com.portal.userprofileservice.dto.response.AcademicRecordResponseDto;
import com.portal.userprofileservice.exception.ResourceNotFoundException;
import com.portal.userprofileservice.model.AcademicRecord;
import com.portal.userprofileservice.repository.AcademicRecordRepository;
import com.portal.userprofileservice.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcademicRecordServiceImpl implements AcademicRecordService {

    private final AcademicRecordRepository academicRecordRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public AcademicRecordResponseDto createOrUpdateAcademicRecord(String userId, AcademicRecordRequestDto dto) {
        if (!userProfileRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found for ID: " + userId);
        }

        AcademicRecord record = academicRecordRepository.findByUserId(userId)
                .orElse(AcademicRecord.builder()
                        .recordId(UUID.randomUUID().toString())
                        .userId(userId)
                        .build());

        record.setFaculty(dto.getFaculty());
        record.setDepartment(dto.getDepartment());
        record.setDegreeProgram(dto.getDegreeProgram());
        record.setSemester(dto.getSemester());
        record.setYear(dto.getYear());
        record.setGpa(dto.getGpa());
        record.setBatch(dto.getBatch());
        record.setTranscriptUrl(dto.getTranscriptUrl());

        AcademicRecord saved = academicRecordRepository.save(record);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicRecordResponseDto getAcademicRecordByUserId(String userId) {
        AcademicRecord record = academicRecordRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic record not found for user ID: " + userId));
        return mapToDto(record);
    }

    @Override
    @Transactional
    public void deleteAcademicRecord(String userId) {
        AcademicRecord record = academicRecordRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic record not found for user ID: " + userId));
        academicRecordRepository.delete(record);
    }

    private AcademicRecordResponseDto mapToDto(AcademicRecord record) {
        return AcademicRecordResponseDto.builder()
                .recordId(record.getRecordId())
                .userId(record.getUserId())
                .faculty(record.getFaculty())
                .department(record.getDepartment())
                .degreeProgram(record.getDegreeProgram())
                .semester(record.getSemester())
                .year(record.getYear())
                .gpa(record.getGpa())
                .batch(record.getBatch())
                .transcriptUrl(record.getTranscriptUrl())
                .build();
    }
}
