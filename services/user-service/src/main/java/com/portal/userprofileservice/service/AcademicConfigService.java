package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.DepartmentRequestDto;
import com.portal.userprofileservice.dto.request.FacultyRequestDto;
import com.portal.userprofileservice.dto.response.DepartmentResponseDto;
import com.portal.userprofileservice.dto.response.FacultyResponseDto;

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
