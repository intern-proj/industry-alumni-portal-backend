package com.portal.user_service.service;

import com.portal.user_service.dto.request.DepartmentRequestDto;
import com.portal.user_service.dto.request.FacultyRequestDto;
import com.portal.user_service.dto.response.DepartmentResponseDto;
import com.portal.user_service.dto.response.FacultyResponseDto;

import java.util.List;

public interface AcademicConfigService {
    FacultyResponseDto createFaculty(FacultyRequestDto dto);
    List<FacultyResponseDto> getAllFaculties();
    FacultyResponseDto getFacultyById(String facultyId);
    DepartmentResponseDto addDepartmentToFaculty(String facultyId, DepartmentRequestDto dto);
    List<DepartmentResponseDto> getDepartmentsByFaculty(String facultyId);
    void deleteFaculty(String facultyId);
    void deleteDepartment(String departmentId);
}
