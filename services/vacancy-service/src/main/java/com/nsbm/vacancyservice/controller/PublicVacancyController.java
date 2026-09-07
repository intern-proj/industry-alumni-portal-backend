package com.nsbm.vacancyservice.controller;

import com.nsbm.vacancyservice.dto.response.ApiResponseDto;
import com.nsbm.vacancyservice.dto.response.VacancyResponseDto;
import com.nsbm.vacancyservice.entity.JobType;
import com.nsbm.vacancyservice.service.VacancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vacancies/public")
@RequiredArgsConstructor
@Tag(name = "Public Vacancy Controller", description = "Public endpoints for exploring approved job and internship postings")
public class PublicVacancyController {

    private final VacancyService vacancyService;

    @GetMapping
    @Operation(summary = "Get published vacancies for undergraduates with filters")
    public ResponseEntity<ApiResponseDto<Page<VacancyResponseDto>>> getPublicVacancies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        Page<VacancyResponseDto> result = vacancyService.getPublicVacancies(keyword, jobType, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(result, "Vacancies fetched successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get public vacancy details by ID")
    public ResponseEntity<ApiResponseDto<VacancyResponseDto>> getVacancyById(@PathVariable Long id) {
        VacancyResponseDto vacancy = vacancyService.getVacancyById(id);
        return ResponseEntity.ok(ApiResponseDto.success(vacancy, "Vacancy details retrieved successfully"));
    }
}
