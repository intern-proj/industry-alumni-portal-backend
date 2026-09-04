package com.nsbm.eventmanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenueRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String address;

    private Integer capacity;


    private String contactInfo;

    private String onlineMeetingLink;
}
