package com.nsbm.vacancyservice.controller;

import com.nsbm.vacancyservice.dto.request.CreateVacancyRequest;
import com.nsbm.vacancyservice.dto.request.UpdateVacancyRequest;
import com.nsbm.vacancyservice.dto.response.ApiResponseDto;
import com.nsbm.vacancyservice.dto.response.VacancyResponseDto;
import com.nsbm.vacancyservice.entity.VacancyStatus;
import com.nsbm.vacancyservice.service.VacancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vacancies/partner")
@RequiredArgsConstructor
@Tag(name = "Partner Vacancy Controller", description = "Endpoints for industry partners to post, manage, and edit vacancies")
public class PartnerVacancyController {

    private final VacancyService vacancyService;

    @PostMapping
    @Operation(summary = "Post a new vacancy by partner organization")
    public ResponseEntity<ApiResponseDto<VacancyResponseDto>> createVacancy(
            @Valid @RequestBody CreateVacancyRequest request
    ) {
        VacancyResponseDto created = vacancyService.createVacancy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(created, "Vacancy submitted successfully and is pending faculty coordinator review."));
    }

    @GetMapping("/{partnerId}")
    @Operation(summary = "Get all vacancies posted by a specific partner")
    public ResponseEntity<ApiResponseDto<Page<VacancyResponseDto>>> getPartnerVacancies(
            @PathVariable String partnerId,
            @RequestParam(required = false) VacancyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VacancyResponseDto> result = vacancyService.getPartnerVacancies(partnerId, status, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(result, "Partner vacancies retrieved successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing vacancy posting")
    public ResponseEntity<ApiResponseDto<VacancyResponseDto>> updateVacancy(
            @PathVariable Long id,
            @RequestBody UpdateVacancyRequest request
    ) {
        VacancyResponseDto updated = vacancyService.updateVacancy(id, request);
        return ResponseEntity.ok(ApiResponseDto.success(updated, "Vacancy updated successfully"));
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Close vacancy to stop receiving new applications")
    public ResponseEntity<ApiResponseDto<VacancyResponseDto>> closeVacancy(@PathVariable Long id) {
        VacancyResponseDto closed = vacancyService.closeVacancy(id);
        return ResponseEntity.ok(ApiResponseDto.success(closed, "Vacancy closed successfully"));
    }

    @PatchMapping("/{id}/reopen")
    @Operation(summary = "Reopen a closed vacancy")
    public ResponseEntity<ApiResponseDto<VacancyResponseDto>> reopenVacancy(@PathVariable Long id) {
        VacancyResponseDto reopened = vacancyService.reopenVacancy(id);
        return ResponseEntity.ok(ApiResponseDto.success(reopened, "Vacancy reopened successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete vacancy posting")
    public ResponseEntity<ApiResponseDto<Void>> deleteVacancy(@PathVariable Long id) {
        vacancyService.deleteVacancy(id);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Vacancy deleted successfully"));
    }
}
