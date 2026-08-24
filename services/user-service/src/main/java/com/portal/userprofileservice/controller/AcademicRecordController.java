package com.portal.userprofileservice.controller;

import com.portal.userprofileservice.dto.request.AcademicRecordRequestDto;
import com.portal.userprofileservice.dto.response.AcademicRecordResponseDto;
import com.portal.userprofileservice.dto.response.ApiResponseDto;
import com.portal.userprofileservice.service.AcademicRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user-profiles/{userId}/academic-records")
@RequiredArgsConstructor
public class AcademicRecordController {

    private final AcademicRecordService academicRecordService;

    @PostMapping
    public ResponseEntity<ApiResponseDto<AcademicRecordResponseDto>> saveAcademicRecord(
            @PathVariable String userId,
            @Valid @RequestBody AcademicRecordRequestDto requestDto) {
        AcademicRecordResponseDto saved = academicRecordService.createOrUpdateAcademicRecord(userId, requestDto);
        return new ResponseEntity<>(ApiResponseDto.success(saved, "Academic record saved successfully"), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponseDto<AcademicRecordResponseDto>> getAcademicRecord(@PathVariable String userId) {
        AcademicRecordResponseDto record = academicRecordService.getAcademicRecordByUserId(userId);
        return ResponseEntity.ok(ApiResponseDto.success(record, "Academic record retrieved successfully"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponseDto<Void>> deleteAcademicRecord(@PathVariable String userId) {
        academicRecordService.deleteAcademicRecord(userId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Academic record deleted successfully"));
    }
}
