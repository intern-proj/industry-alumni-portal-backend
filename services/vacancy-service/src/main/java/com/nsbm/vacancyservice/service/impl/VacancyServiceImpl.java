package com.nsbm.vacancyservice.service.impl;

import com.nsbm.vacancyservice.dto.request.CreateVacancyRequest;
import com.nsbm.vacancyservice.dto.request.UpdateVacancyRequest;
import com.nsbm.vacancyservice.dto.request.VacancyApprovalRequest;
import com.nsbm.vacancyservice.dto.response.VacancyResponseDto;
import com.nsbm.vacancyservice.dto.response.VacancyStatsDto;
import com.nsbm.vacancyservice.entity.JobType;
import com.nsbm.vacancyservice.entity.Vacancy;
import com.nsbm.vacancyservice.entity.VacancyStatus;
import com.nsbm.vacancyservice.entity.WorkplaceType;
import com.nsbm.vacancyservice.exception.ResourceNotFoundException;
import com.nsbm.vacancyservice.repository.VacancyRepository;
import com.nsbm.vacancyservice.publisher.VacancyEventPublisher;
import com.nsbm.vacancyservice.service.VacancyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VacancyServiceImpl implements VacancyService {

    private final VacancyRepository vacancyRepository;
    private final VacancyEventPublisher vacancyEventPublisher;

    @Override
    @Transactional
    public VacancyResponseDto createVacancy(CreateVacancyRequest request) {
        Vacancy vacancy = Vacancy.builder()
                .partnerId(request.getPartnerId())
                .companyName(request.getCompanyName())
                .title(request.getTitle())
                .description(request.getDescription())
                .requirements(request.getRequirements())
                .location(request.getLocation())
                .jobType(request.getJobType())
                .workplaceType(request.getWorkplaceType() != null ? request.getWorkplaceType() : WorkplaceType.ON_SITE)
                .status(VacancyStatus.PENDING)
                .salaryRange(request.getSalaryRange())
                .applicationDeadline(request.getApplicationDeadline())
                .tags(request.getTags())
                .targetFaculties(request.getTargetFaculties())
                .numberOfOpenings(request.getNumberOfOpenings() != null ? request.getNumberOfOpenings() : 1)
                .applicantCount(0)
                .coordinatorNotes(request.getCoordinatorNotes())
                .aiMissingFields(request.getAiMissingFields())
                .storageFileId(request.getStorageFileId())
                .build();

        Vacancy saved = vacancyRepository.save(vacancy);

        if (saved.getStorageFileId() != null && !saved.getStorageFileId().isBlank()) {
            vacancyEventPublisher.publishVacancyFlyerUploaded(
                    saved.getId(),
                    saved.getPartnerId(),
                    saved.getStorageFileId(),
                    null
            );
        }

        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VacancyResponseDto getVacancyById(Long id) {
        Vacancy vacancy = vacancyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with ID: " + id));
        return mapToDto(vacancy);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VacancyResponseDto> getPublicVacancies(String keyword, JobType jobType, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        if (hasKeyword) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            if (jobType != null) {
                return vacancyRepository.searchPublicVacanciesWithKeywordAndJobType(VacancyStatus.APPROVED, jobType, pattern, pageable)
                        .map(this::mapToDto);
            } else {
                return vacancyRepository.searchPublicVacanciesWithKeyword(VacancyStatus.APPROVED, pattern, pageable)
                        .map(this::mapToDto);
            }
        } else {
            if (jobType != null) {
                return vacancyRepository.findByStatusAndJobType(VacancyStatus.APPROVED, jobType, pageable)
                        .map(this::mapToDto);
            } else {
                return vacancyRepository.findByStatus(VacancyStatus.APPROVED, pageable)
                        .map(this::mapToDto);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VacancyResponseDto> getPartnerVacancies(String partnerId, VacancyStatus status, Pageable pageable) {
        if (status != null) {
            return vacancyRepository.findByPartnerIdAndStatus(partnerId, status, pageable).map(this::mapToDto);
        }
        return vacancyRepository.findByPartnerId(partnerId, pageable).map(this::mapToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VacancyResponseDto> getAllVacanciesForAdmin(VacancyStatus status, String keyword, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        if (hasKeyword) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            if (status != null) {
                return vacancyRepository.searchAdminVacanciesWithStatusAndKeyword(status, pattern, pageable)
                        .map(this::mapToDto);
            } else {
                return vacancyRepository.searchAdminVacanciesWithKeyword(pattern, pageable)
                        .map(this::mapToDto);
            }
        } else {
            if (status != null) {
                return vacancyRepository.findByStatus(status, pageable)
                        .map(this::mapToDto);
            } else {
                return vacancyRepository.findAll(pageable)
                        .map(this::mapToDto);
            }
        }
    }

    @Override
    @Transactional
    public VacancyResponseDto updateVacancy(Long id, UpdateVacancyRequest request) {
        Vacancy vacancy = vacancyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with ID: " + id));

        if (request.getTitle() != null) vacancy.setTitle(request.getTitle());
        if (request.getDescription() != null) vacancy.setDescription(request.getDescription());
        if (request.getRequirements() != null) vacancy.setRequirements(request.getRequirements());
        if (request.getLocation() != null) vacancy.setLocation(request.getLocation());
        if (request.getJobType() != null) vacancy.setJobType(request.getJobType());
        if (request.getWorkplaceType() != null) vacancy.setWorkplaceType(request.getWorkplaceType());
        if (request.getSalaryRange() != null) vacancy.setSalaryRange(request.getSalaryRange());
        if (request.getApplicationDeadline() != null) vacancy.setApplicationDeadline(request.getApplicationDeadline());
        if (request.getTags() != null) vacancy.setTags(request.getTags());
        if (request.getTargetFaculties() != null) vacancy.setTargetFaculties(request.getTargetFaculties());
        if (request.getNumberOfOpenings() != null) vacancy.setNumberOfOpenings(request.getNumberOfOpenings());
        if (request.getStatus() != null) vacancy.setStatus(request.getStatus());
        if (request.getStorageFileId() != null) vacancy.setStorageFileId(request.getStorageFileId());
        if (request.getAiMissingFields() != null) vacancy.setAiMissingFields(request.getAiMissingFields());

        Vacancy updated = vacancyRepository.save(vacancy);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public VacancyResponseDto reviewVacancyApproval(Long id, VacancyApprovalRequest request) {
        Vacancy vacancy = vacancyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with ID: " + id));

        vacancy.setStatus(request.getStatus());
        if (request.getRejectionReason() != null) {
            vacancy.setRejectionReason(request.getRejectionReason());
        }
        if (request.getComments() != null) {
            vacancy.setCoordinatorNotes(request.getComments());
        }

        Vacancy updated = vacancyRepository.save(vacancy);

        String partnerEmail = resolvePartnerEmail(updated);

        try {
            if (updated.getStatus() == VacancyStatus.APPROVED) {
                vacancyEventPublisher.publishVacancyApproved(
                        updated.getId(),
                        updated.getTitle(),
                        updated.getCompanyName(),
                        partnerEmail
                );
            } else if (updated.getStatus() == VacancyStatus.CHANGES_REQUESTED) {
                vacancyEventPublisher.publishVacancyChangesRequested(
                        updated.getId(),
                        updated.getTitle(),
                        updated.getCompanyName(),
                        partnerEmail,
                        updated.getCoordinatorNotes()
                );
            } else if (updated.getStatus() == VacancyStatus.REJECTED) {
                vacancyEventPublisher.publishVacancyRejected(
                        updated.getId(),
                        updated.getTitle(),
                        updated.getCompanyName(),
                        partnerEmail,
                        updated.getRejectionReason() != null ? updated.getRejectionReason() : updated.getCoordinatorNotes()
                );
            }
        } catch (Exception ex) {
            log.error("Failed to publish email notification event for vacancy ID {}: {}", updated.getId(), ex.getMessage());
        }

        return mapToDto(updated);
    }

    private String resolvePartnerEmail(Vacancy vacancy) {
        if (vacancy == null) return "partner@nsbm.ac.lk";
        String partnerId = vacancy.getPartnerId();
        if (partnerId != null && partnerId.contains("@")) {
            return partnerId;
        }
        try {
            String url = "http://localhost:8081/api/v1/user-profiles/" + partnerId;
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map response = restTemplate.getForObject(url, java.util.Map.class);
            if (response != null && response.containsKey("data")) {
                java.util.Map data = (java.util.Map) response.get("data");
                if (data != null && data.get("email") != null) {
                    return data.get("email").toString();
                }
            }
        } catch (Exception ex) {
            log.warn("Could not retrieve partner email from user-service for partnerId {}: {}", partnerId, ex.getMessage());
        }
        return (partnerId != null ? partnerId : "partner") + "@nsbm.ac.lk";
    }

    @Override
    @Transactional
    public VacancyResponseDto closeVacancy(Long id) {
        Vacancy vacancy = vacancyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with ID: " + id));

        vacancy.setStatus(VacancyStatus.CLOSED);
        Vacancy updated = vacancyRepository.save(vacancy);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public VacancyResponseDto reopenVacancy(Long id) {
        Vacancy vacancy = vacancyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found with ID: " + id));

        vacancy.setStatus(VacancyStatus.APPROVED);
        Vacancy updated = vacancyRepository.save(vacancy);
        return mapToDto(updated);
    }

    @Override
    @Transactional
    public void deleteVacancy(Long id) {
        if (!vacancyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Vacancy not found with ID: " + id);
        }
        vacancyRepository.deleteById(id);
        vacancyEventPublisher.publishVacancyDeleted(id);
    }

    @Override
    @Transactional(readOnly = true)
    public VacancyStatsDto getVacancyStats() {
        return VacancyStatsDto.builder()
                .totalVacancies(vacancyRepository.count())
                .approvedVacancies(vacancyRepository.countByStatus(VacancyStatus.APPROVED))
                .pendingVacancies(vacancyRepository.countByStatus(VacancyStatus.PENDING))
                .rejectedVacancies(vacancyRepository.countByStatus(VacancyStatus.REJECTED))
                .closedVacancies(vacancyRepository.countByStatus(VacancyStatus.CLOSED))
                .build();
    }

    @Override
    @Transactional
    public void incrementApplicantCount(Long id) {
        vacancyRepository.findById(id).ifPresent(v -> {
            v.setApplicantCount((v.getApplicantCount() != null ? v.getApplicantCount() : 0) + 1);
            vacancyRepository.save(v);
        });
    }

    private VacancyResponseDto mapToDto(Vacancy v) {
        return VacancyResponseDto.builder()
                .id(v.getId())
                .partnerId(v.getPartnerId())
                .companyName(v.getCompanyName())
                .title(v.getTitle())
                .description(v.getDescription())
                .requirements(v.getRequirements())
                .location(v.getLocation())
                .jobType(v.getJobType())
                .workplaceType(v.getWorkplaceType())
                .status(v.getStatus())
                .salaryRange(v.getSalaryRange())
                .applicationDeadline(v.getApplicationDeadline())
                .tags(v.getTags())
                .targetFaculties(v.getTargetFaculties())
                .numberOfOpenings(v.getNumberOfOpenings())
                .applicantCount(v.getApplicantCount())
                .rejectionReason(v.getRejectionReason())
                .coordinatorNotes(v.getCoordinatorNotes())
                .aiMissingFields(v.getAiMissingFields())
                .storageFileId(v.getStorageFileId())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
