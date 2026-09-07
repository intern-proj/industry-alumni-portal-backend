package com.nsbm.vacancyservice.service;

import com.nsbm.vacancyservice.dto.request.CreateVacancyRequest;
import com.nsbm.vacancyservice.dto.request.UpdateVacancyRequest;
import com.nsbm.vacancyservice.dto.request.VacancyApprovalRequest;
import com.nsbm.vacancyservice.dto.response.VacancyResponseDto;
import com.nsbm.vacancyservice.dto.response.VacancyStatsDto;
import com.nsbm.vacancyservice.entity.JobType;
import com.nsbm.vacancyservice.entity.VacancyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VacancyService {

    VacancyResponseDto createVacancy(CreateVacancyRequest request);

    VacancyResponseDto getVacancyById(Long id);

    Page<VacancyResponseDto> getPublicVacancies(String keyword, JobType jobType, Pageable pageable);

    Page<VacancyResponseDto> getPartnerVacancies(String partnerId, VacancyStatus status, Pageable pageable);

    Page<VacancyResponseDto> getAllVacanciesForAdmin(VacancyStatus status, String keyword, Pageable pageable);

    VacancyResponseDto updateVacancy(Long id, UpdateVacancyRequest request);

    VacancyResponseDto reviewVacancyApproval(Long id, VacancyApprovalRequest request);

    VacancyResponseDto closeVacancy(Long id);

    VacancyResponseDto reopenVacancy(Long id);

    void deleteVacancy(Long id);

    VacancyStatsDto getVacancyStats();

    void incrementApplicantCount(Long id);

    void reprocessVacancy(Long id);
}
