package com.portal.platformservice.service;

import com.portal.platformservice.dto.request.BannerRequest;
import com.portal.platformservice.dto.response.BannerResponse;
import com.portal.platformservice.entity.SystemBanner;
import com.portal.platformservice.exception.ResourceNotFoundException;
import com.portal.platformservice.repository.SystemBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemBannerService {

    private final SystemBannerRepository repository;

    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        return repository.findActiveBanners(LocalDate.now()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BannerResponse createBanner(BannerRequest request) {
        SystemBanner banner = SystemBanner.builder()
                .message(request.getMessage())
                .type(request.getType())
                .icon(request.getIcon())
                .priority(request.getPriority())
                .color(request.getColor())
                .textColor(request.getTextColor())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .targetAudience(request.getTargetAudience())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        
        SystemBanner saved = repository.save(banner);
        return mapToResponse(saved);
    }

    @Transactional
    public BannerResponse updateBanner(UUID id, BannerRequest request) {
        SystemBanner banner = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id " + id));

        banner.setMessage(request.getMessage());
        banner.setType(request.getType());
        banner.setIcon(request.getIcon());
        banner.setPriority(request.getPriority());
        banner.setColor(request.getColor());
        banner.setTextColor(request.getTextColor());
        banner.setStartDate(request.getStartDate());
        banner.setEndDate(request.getEndDate());
        banner.setTargetAudience(request.getTargetAudience());
        if (request.getActive() != null) {
            banner.setActive(request.getActive());
        }

        SystemBanner saved = repository.save(banner);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteBanner(UUID id) {
        SystemBanner banner = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found with id " + id));
        repository.delete(banner);
    }

    private BannerResponse mapToResponse(SystemBanner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .message(banner.getMessage())
                .type(banner.getType())
                .icon(banner.getIcon())
                .priority(banner.getPriority())
                .color(banner.getColor())
                .textColor(banner.getTextColor())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .targetAudience(banner.getTargetAudience())
                .active(banner.isActive())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .build();
    }
}
