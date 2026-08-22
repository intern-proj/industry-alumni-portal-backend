package com.nsbm.eventmanagementservice.dto;

import com.nsbm.eventmanagementservice.model.VenueType;
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

    @NotNull(message = "Venue type is required")
    private VenueType venueType;

    private String contactInfo;

    private String onlineMeetingLink;
}
