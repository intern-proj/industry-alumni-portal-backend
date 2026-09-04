package com.portal.platformservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequest {

    @NotBlank(message = "Message is required")
    private String message;

    @NotBlank(message = "Type is required")
    private String type;

    private String icon;

    private String priority;

    private String color;

    private String textColor;

    private LocalDate startDate;

    private LocalDate endDate;

    private String targetAudience;

    @NotNull
    @Builder.Default
    private Boolean active = true;
}
