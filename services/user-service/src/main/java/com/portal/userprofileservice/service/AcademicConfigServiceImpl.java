package com.portal.userprofileservice.service;

import com.portal.userprofileservice.dto.request.DepartmentRequestDto;
import com.portal.userprofileservice.dto.request.FacultyRequestDto;
import com.portal.userprofileservice.dto.response.DepartmentResponseDto;
import com.portal.userprofileservice.dto.response.FacultyResponseDto;
import com.portal.userprofileservice.exception.ResourceNotFoundException;
import com.portal.userprofileservice.model.Department;
import com.portal.userprofileservice.model.Faculty;
import com.portal.userprofileservice.repository.DepartmentRepository;
import com.portal.userprofileservice.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicConfigServiceImpl implements AcademicConfigService {

    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public FacultyResponseDto createFaculty(FacultyRequestDto dto) {
        if (facultyRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Faculty with name '" + dto.getName() + "' already exists.");
        }

        Faculty faculty = Faculty.builder()
                .facultyId(UUID.randomUUID().toString())
                .name(dto.getName())
                .code(dto.getCode())
                .description(dto.getDescription())
                .departments(new ArrayList<>())
                .build();

        if (dto.getDepartments() != null) {
            for (DepartmentRequestDto deptDto : dto.getDepartments()) {
                Department dept = Department.builder()
                        .departmentId(UUID.randomUUID().toString())
                        .faculty(faculty)
                        .name(deptDto.getName())
                        .code(deptDto.getCode())
                        .build();
                faculty.getDepartments().add(dept);
            }
        }

        Faculty saved = facultyRepository.save(faculty);
        return mapFacultyToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FacultyResponseDto> getAllFaculties() {
        return facultyRepository.findAll().stream()
                .map(this::mapFacultyToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FacultyResponseDto getFacultyById(String facultyId) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found for ID: " + facultyId));
        return mapFacultyToDto(faculty);
    }

    @Override
    @Transactional
    public DepartmentResponseDto addDepartmentToFaculty(String facultyId, DepartmentRequestDto dto) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found for ID: " + facultyId));

        Department department = Department.builder()
                .departmentId(UUID.randomUUID().toString())
                .faculty(faculty)
                .name(dto.getName())
                .code(dto.getCode())
                .build();

        Department saved = departmentRepository.save(department);
        return mapDepartmentToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponseDto> getDepartmentsByFaculty(String facultyId) {
        return departmentRepository.findByFacultyFacultyId(facultyId).stream()
                .map(this::mapDepartmentToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteFaculty(String facultyId) {
        Faculty faculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new ResourceNotFoundException("Faculty not found for ID: " + facultyId));
        facultyRepository.delete(faculty);
    }

    @Override
    @Transactional
    public void deleteDepartment(String departmentId) {
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found for ID: " + departmentId));
        departmentRepository.delete(dept);
    }

    private FacultyResponseDto mapFacultyToDto(Faculty faculty) {
        List<DepartmentResponseDto> deptDtos = faculty.getDepartments() != null
                ? faculty.getDepartments().stream().map(this::mapDepartmentToDto).collect(Collectors.toList())
                : new ArrayList<>();

        return FacultyResponseDto.builder()
                .facultyId(faculty.getFacultyId())
                .name(faculty.getName())
                .code(faculty.getCode())
                .description(faculty.getDescription())
                .departments(deptDtos)
                .build();
    }

    private DepartmentResponseDto mapDepartmentToDto(Department dept) {
        return DepartmentResponseDto.builder()
                .departmentId(dept.getDepartmentId())
                .name(dept.getName())
                .code(dept.getCode())
                .build();
    }
}
