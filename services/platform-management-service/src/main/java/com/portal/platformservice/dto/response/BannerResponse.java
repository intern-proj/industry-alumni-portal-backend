package com.portal.platformservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerResponse {
    private UUID id;
    private String message;
    private String type;
    private String icon;
    private String priority;
    private String color;
    private String textColor;
    private LocalDate startDate;
    private LocalDate endDate;
    private String targetAudience;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
