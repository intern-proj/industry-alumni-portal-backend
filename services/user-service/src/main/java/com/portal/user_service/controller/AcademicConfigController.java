package com.portal.user_service.controller;

import com.portal.user_service.dto.request.DepartmentRequestDto;
import com.portal.user_service.dto.request.FacultyRequestDto;
import com.portal.user_service.dto.response.ApiResponseDto;
import com.portal.user_service.dto.response.DepartmentResponseDto;
import com.portal.user_service.dto.response.FacultyResponseDto;
import com.portal.user_service.service.AcademicConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-config")
@RequiredArgsConstructor
public class AcademicConfigController {

    private final AcademicConfigService academicConfigService;

    @PostMapping("/faculties")
    public ResponseEntity<ApiResponseDto<FacultyResponseDto>> createFaculty(
            @Valid @RequestBody FacultyRequestDto requestDto) {
        FacultyResponseDto created = academicConfigService.createFaculty(requestDto);
        return new ResponseEntity<>(ApiResponseDto.success(created, "Faculty created successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/faculties")
    public ResponseEntity<ApiResponseDto<List<FacultyResponseDto>>> getAllFaculties() {
        List<FacultyResponseDto> faculties = academicConfigService.getAllFaculties();
        return ResponseEntity.ok(ApiResponseDto.success(faculties, "Faculties retrieved successfully"));
    }

    @GetMapping("/faculties/{facultyId}")
    public ResponseEntity<ApiResponseDto<FacultyResponseDto>> getFacultyById(@PathVariable String facultyId) {
        FacultyResponseDto faculty = academicConfigService.getFacultyById(facultyId);
        return ResponseEntity.ok(ApiResponseDto.success(faculty, "Faculty retrieved successfully"));
    }

    @PostMapping("/faculties/{facultyId}/departments")
    public ResponseEntity<ApiResponseDto<DepartmentResponseDto>> addDepartment(
            @PathVariable String facultyId,
            @Valid @RequestBody DepartmentRequestDto requestDto) {
        DepartmentResponseDto created = academicConfigService.addDepartmentToFaculty(facultyId, requestDto);
        return new ResponseEntity<>(ApiResponseDto.success(created, "Department added successfully"), HttpStatus.CREATED);
    }

    @GetMapping("/faculties/{facultyId}/departments")
    public ResponseEntity<ApiResponseDto<List<DepartmentResponseDto>>> getDepartmentsByFaculty(
            @PathVariable String facultyId) {
        List<DepartmentResponseDto> departments = academicConfigService.getDepartmentsByFaculty(facultyId);
        return ResponseEntity.ok(ApiResponseDto.success(departments, "Departments retrieved successfully"));
    }

    @DeleteMapping("/faculties/{facultyId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteFaculty(@PathVariable String facultyId) {
        academicConfigService.deleteFaculty(facultyId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Faculty deleted successfully"));
    }

    @DeleteMapping("/departments/{departmentId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteDepartment(@PathVariable String departmentId) {
        academicConfigService.deleteDepartment(departmentId);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Department deleted successfully"));
    }
}
