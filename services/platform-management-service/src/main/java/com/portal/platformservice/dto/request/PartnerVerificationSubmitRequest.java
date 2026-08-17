package com.portal.platformservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerVerificationSubmitRequest {

    @NotNull
    private UUID userId;

    @NotBlank
    private String organizationNameSnapshot;

    @NotBlank
    @Email
    private String contactEmailSnapshot;
}
