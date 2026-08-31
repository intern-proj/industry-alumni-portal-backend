package com.nsbm.vacancyservice.controller;

import com.nsbm.vacancyservice.dto.request.VacancyApprovalRequest;
import com.nsbm.vacancyservice.dto.response.ApiResponseDto;
import com.nsbm.vacancyservice.dto.response.VacancyResponseDto;
import com.nsbm.vacancyservice.dto.response.VacancyStatsDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vacancies/admin")
@RequiredArgsConstructor
@Tag(name = "Admin / Staff Vacancy Controller", description = "Endpoints for faculty coordinators to audit, approve, and track vacancy statistics")
public class AdminVacancyController {

    private final VacancyService vacancyService;

    @GetMapping
    @Operation(summary = "Get all vacancies with admin filters")
    public ResponseEntity<ApiResponseDto<Page<VacancyResponseDto>>> getAllVacancies(
            @RequestParam(required = false) VacancyStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<VacancyResponseDto> result = vacancyService.getAllVacanciesForAdmin(status, keyword, pageable);
        return ResponseEntity.ok(ApiResponseDto.success(result, "All vacancies retrieved successfully"));
    }

    @PutMapping("/{id}/approval")
    @Operation(summary = "Approve or Reject vacancy with coordinator notes")
    public ResponseEntity<ApiResponseDto<VacancyResponseDto>> reviewVacancy(
            @PathVariable Long id,
            @Valid @RequestBody VacancyApprovalRequest request
    ) {
        VacancyResponseDto reviewed = vacancyService.reviewVacancyApproval(id, request);
        return ResponseEntity.ok(ApiResponseDto.success(reviewed, "Vacancy approval review recorded successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vacancy details by ID for admin review")
    public ResponseEntity<ApiResponseDto<VacancyResponseDto>> getVacancyById(@PathVariable Long id) {
        VacancyResponseDto vacancy = vacancyService.getVacancyById(id);
        return ResponseEntity.ok(ApiResponseDto.success(vacancy, "Vacancy details retrieved successfully"));
    }

    @PatchMapping("/{id}/close")
    @Operation(summary = "Deactivate/Close an approved vacancy")
    public ResponseEntity<ApiResponseDto<VacancyResponseDto>> closeVacancy(@PathVariable Long id) {
        VacancyResponseDto closed = vacancyService.closeVacancy(id);
        return ResponseEntity.ok(ApiResponseDto.success(closed, "Vacancy deactivated/closed successfully"));
    }

    @PatchMapping("/{id}/reopen")
    @Operation(summary = "Reopen a closed vacancy")
    public ResponseEntity<ApiResponseDto<VacancyResponseDto>> reopenVacancy(@PathVariable Long id) {
        VacancyResponseDto reopened = vacancyService.reopenVacancy(id);
        return ResponseEntity.ok(ApiResponseDto.success(reopened, "Vacancy reopened successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete vacancy permanently")
    public ResponseEntity<ApiResponseDto<Void>> deleteVacancy(@PathVariable Long id) {
        vacancyService.deleteVacancy(id);
        return ResponseEntity.ok(ApiResponseDto.success(null, "Vacancy deleted successfully"));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get overall vacancy metrics and count breakdowns")
    public ResponseEntity<ApiResponseDto<VacancyStatsDto>> getVacancyStats() {
        VacancyStatsDto stats = vacancyService.getVacancyStats();
        return ResponseEntity.ok(ApiResponseDto.success(stats, "Vacancy statistics calculated successfully"));
    }
}
